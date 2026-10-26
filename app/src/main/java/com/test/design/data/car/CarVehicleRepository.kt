package com.test.design.data.car

import android.car.Car
import android.car.VehicleAreaWheel
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.VehicleUnit
import android.car.hardware.CarPropertyConfig
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reads the VHAL properties that reference vehicles commonly expose.
 * Properties the car does not publish are skipped, and the UI hides them.
 */
class CarVehicleRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val started = AtomicBoolean(false)

    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null
    private var subscribed = emptySet<Int>()
    private var tireAreas = emptyList<Int>()
    private val reading = Reading()

    private val _signals = MutableStateFlow(VehicleSignals())
    val signals: StateFlow<VehicleSignals> = _signals.asStateFlow()

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>?) {
            if (value == null) return
            mainHandler.post {
                if (apply(value)) publish()
            }
        }

        override fun onErrorEvent(propId: Int, areaId: Int) {
            Log.w(TAG, "Vehicle property error propId=$propId area=$areaId")
        }
    }

    fun start() {
        if (!started.compareAndSet(false, true)) return
        try {
            car = Car.createCar(
                appContext,
                mainHandler,
                Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER,
            ) { created, ready ->
                if (ready) onCarReady(created) else onCarDisconnected()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Car.createCar failed", t)
            started.set(false)
            _signals.value = VehicleSignals()
        }
    }

    fun stop() {
        if (!started.getAndSet(false)) return
        unregister()
        propertyManager = null
        runCatching { car?.disconnect() }
        car = null
        subscribed = emptySet()
        tireAreas = emptyList()
        reading.clear()
        _signals.value = VehicleSignals()
    }

    private fun onCarReady(created: Car) {
        car = created
        val mgr = runCatching {
            created.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
        }.getOrNull()
        if (mgr == null) {
            _signals.value = VehicleSignals()
            return
        }
        propertyManager = mgr
        val present = accessibleProperties(mgr)
        subscribed = present.intersect(VALUE_PROPERTIES)
        tireAreas = if (VehiclePropertyIds.TIRE_PRESSURE in subscribed) {
            areaIds(mgr, VehiclePropertyIds.TIRE_PRESSURE).toList()
        } else {
            emptyList()
        }
        reading.tiresAvailable = tireAreas.isNotEmpty()
        if (subscribed.isEmpty()) {
            Log.w(TAG, "No standard vehicle properties accessible")
            _signals.value = VehicleSignals()
            return
        }
        present.forEach { propId -> register(mgr, propId) }
        readAll(mgr, present)
        publish()
        Log.i(TAG, "Vehicle properties live: $subscribed")
    }

    private fun onCarDisconnected() {
        unregister()
        propertyManager = null
        subscribed = emptySet()
        tireAreas = emptyList()
        reading.clear()
        _signals.value = VehicleSignals()
    }

    private fun accessibleProperties(mgr: CarPropertyManager): Set<Int> {
        val listed = runCatching { mgr.propertyList.orEmpty().map { it.propertyId }.toSet() }.getOrNull()
        return WANTED.filter { id ->
            (listed == null || id in listed) && config(mgr, id) != null
        }.toSet()
    }

    private fun register(mgr: CarPropertyManager, propId: Int) {
        val config = config(mgr, propId) ?: return
        val rate = if (config.changeMode == CarPropertyConfig.VEHICLE_PROPERTY_CHANGE_MODE_CONTINUOUS) {
            CarPropertyManager.SENSOR_RATE_UI
        } else {
            CarPropertyManager.SENSOR_RATE_ONCHANGE
        }
        runCatching { mgr.registerCallback(callback, propId, rate) }
            .onFailure { Log.w(TAG, "register $propId failed", it) }
    }

    private fun readAll(mgr: CarPropertyManager, present: Set<Int>) {
        present.forEach { propId ->
            val areas = when (propId) {
                VehiclePropertyIds.TIRE_PRESSURE,
                VehiclePropertyIds.CRITICALLY_LOW_TIRE_PRESSURE,
                -> areaIds(mgr, propId).toList().ifEmpty { tireAreas }
                else -> listOf(primaryArea(mgr, propId))
            }
            areas.forEach { area ->
                val value = runCatching { mgr.getProperty<Any?>(propId, area) }.getOrNull() ?: return@forEach
                apply(value)
            }
        }
    }

    private fun publish() {
        val distance = reading.distanceUnit ?: DistanceUnit.Kilometers
        val speedIsMph = reading.speedUnitMph ?: (distance == DistanceUnit.Miles)
        _signals.value = VehicleSignals(
            connected = subscribed.isNotEmpty(),
            batteryRemainingWh = reading.batteryWh,
            batteryCapacityWh = reading.batteryCapacityWh,
            rangeMeters = reading.rangeMeters,
            odometerKilometers = reading.odometerKm,
            chargeRateRaw = reading.chargeRateRaw,
            chargePortConnected = reading.chargePortConnected,
            fuelMilliliters = reading.fuelMl,
            fuelCapacityMilliliters = reading.fuelCapacityMl,
            speedMetersPerSecond = reading.speedMps,
            gear = reading.gear,
            parkingBrakeOn = reading.parkingBrakeOn,
            outsideCelsius = reading.outsideC,
            distanceUnit = distance,
            speedInMilesPerHour = speedIsMph,
            pressureUnit = reading.pressureUnit ?: PressureUnit.Kilopascal,
            temperatureInFahrenheit = reading.temperatureFahrenheit == true,
            tires = tireAreas.mapNotNull { area ->
                val kPa = reading.tireKpa[area] ?: return@mapNotNull null
                TireSignal(
                    position = wheelPosition(area),
                    kiloPascals = kPa,
                    criticallyLow = reading.tireCritical[area] == true,
                )
            },
            tiresAvailable = reading.tiresAvailable,
        )
    }

    private fun apply(value: CarPropertyValue<*>): Boolean {
        if (!isAvailable(value)) return false
        val area = value.areaId
        when (value.propertyId) {
            VehiclePropertyIds.EV_BATTERY_LEVEL -> reading.batteryWh = number(value)
            VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY -> reading.batteryCapacityWh = number(value)
            VehiclePropertyIds.RANGE_REMAINING -> reading.rangeMeters = number(value)
            VehiclePropertyIds.PERF_ODOMETER -> reading.odometerKm = number(value)
            VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE -> reading.chargeRateRaw = number(value)
            VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED -> reading.chargePortConnected = flag(value)
            VehiclePropertyIds.FUEL_LEVEL -> reading.fuelMl = number(value)
            VehiclePropertyIds.INFO_FUEL_CAPACITY -> reading.fuelCapacityMl = number(value)
            VehiclePropertyIds.PERF_VEHICLE_SPEED -> reading.speedMps = number(value)
            VehiclePropertyIds.GEAR_SELECTION -> reading.gear = gearKind(number(value)?.toInt())
            VehiclePropertyIds.PARKING_BRAKE_ON -> reading.parkingBrakeOn = flag(value)
            VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE -> reading.outsideC = number(value)
            VehiclePropertyIds.TIRE_PRESSURE -> number(value)?.let { reading.tireKpa[area] = it }
            VehiclePropertyIds.CRITICALLY_LOW_TIRE_PRESSURE ->
                flag(value)?.let { reading.tireCritical[area] = it }
            VehiclePropertyIds.DISTANCE_DISPLAY_UNITS -> reading.distanceUnit = distanceUnit(number(value)?.toInt())
            VehiclePropertyIds.VEHICLE_SPEED_DISPLAY_UNITS ->
                reading.speedUnitMph = speedIsMph(number(value)?.toInt())
            VehiclePropertyIds.TIRE_PRESSURE_DISPLAY_UNITS ->
                reading.pressureUnit = pressureUnit(number(value)?.toInt())
            VehiclePropertyIds.HVAC_TEMPERATURE_DISPLAY_UNITS ->
                reading.temperatureFahrenheit = number(value)?.toInt() == VehicleUnit.FAHRENHEIT
            else -> return false
        }
        return true
    }

    private fun unregister() {
        val mgr = propertyManager ?: return
        runCatching { mgr.unregisterCallback(callback) }
    }

    private fun config(mgr: CarPropertyManager, propId: Int): CarPropertyConfig<*>? =
        runCatching { mgr.getCarPropertyConfig(propId) }.getOrNull()

    private fun areaIds(mgr: CarPropertyManager, propId: Int): IntArray =
        config(mgr, propId)?.areaIds?.takeIf { it.isNotEmpty() } ?: intArrayOf()

    private fun primaryArea(mgr: CarPropertyManager, propId: Int): Int =
        config(mgr, propId)?.areaIds?.firstOrNull() ?: 0

    private fun isAvailable(value: CarPropertyValue<*>): Boolean {
        val status = runCatching { value.propertyStatus }.getOrElse { value.status }
        return status == CarPropertyValue.STATUS_AVAILABLE
    }

    private fun number(value: CarPropertyValue<*>): Float? = (value.value as? Number)?.toFloat()

    private fun flag(value: CarPropertyValue<*>): Boolean? = when (val raw = value.value) {
        is Boolean -> raw
        is Number -> raw.toInt() != 0
        else -> null
    }

    private fun distanceUnit(unit: Int?): DistanceUnit? = when (unit) {
        VehicleUnit.MILE -> DistanceUnit.Miles
        VehicleUnit.KILOMETER -> DistanceUnit.Kilometers
        else -> null
    }

    private fun speedIsMph(unit: Int?): Boolean? = when (unit) {
        VehicleUnit.MILES_PER_HOUR -> true
        VehicleUnit.KILOMETERS_PER_HOUR -> false
        else -> null
    }

    private fun pressureUnit(unit: Int?): PressureUnit? = when (unit) {
        VehicleUnit.PSI -> PressureUnit.Psi
        VehicleUnit.BAR -> PressureUnit.Bar
        VehicleUnit.KILOPASCAL -> PressureUnit.Kilopascal
        else -> null
    }

    private fun gearKind(value: Int?): VehicleGearKind? = when (value) {
        VehicleGear.GEAR_PARK -> VehicleGearKind.Park
        VehicleGear.GEAR_REVERSE -> VehicleGearKind.Reverse
        VehicleGear.GEAR_NEUTRAL -> VehicleGearKind.Neutral
        VehicleGear.GEAR_DRIVE -> VehicleGearKind.Drive
        else -> null
    }

    private fun wheelPosition(area: Int): WheelPosition = when (area) {
        VehicleAreaWheel.WHEEL_LEFT_FRONT -> WheelPosition.FrontLeft
        VehicleAreaWheel.WHEEL_RIGHT_FRONT -> WheelPosition.FrontRight
        VehicleAreaWheel.WHEEL_LEFT_REAR -> WheelPosition.RearLeft
        VehicleAreaWheel.WHEEL_RIGHT_REAR -> WheelPosition.RearRight
        else -> WheelPosition.Other
    }

    private class Reading {
        var batteryWh: Float? = null
        var batteryCapacityWh: Float? = null
        var rangeMeters: Float? = null
        var odometerKm: Float? = null
        var chargeRateRaw: Float? = null
        var chargePortConnected: Boolean? = null
        var fuelMl: Float? = null
        var fuelCapacityMl: Float? = null
        var speedMps: Float? = null
        var gear: VehicleGearKind? = null
        var parkingBrakeOn: Boolean? = null
        var outsideC: Float? = null
        var distanceUnit: DistanceUnit? = null
        var speedUnitMph: Boolean? = null
        var pressureUnit: PressureUnit? = null
        var temperatureFahrenheit: Boolean? = null
        var tiresAvailable: Boolean = false
        val tireKpa = mutableMapOf<Int, Float>()
        val tireCritical = mutableMapOf<Int, Boolean>()

        fun clear() {
            batteryWh = null
            batteryCapacityWh = null
            rangeMeters = null
            odometerKm = null
            chargeRateRaw = null
            chargePortConnected = null
            fuelMl = null
            fuelCapacityMl = null
            speedMps = null
            gear = null
            parkingBrakeOn = null
            outsideC = null
            distanceUnit = null
            speedUnitMph = null
            pressureUnit = null
            temperatureFahrenheit = null
            tiresAvailable = false
            tireKpa.clear()
            tireCritical.clear()
        }
    }

    private companion object {
        const val TAG = "CarVehicleRepository"

        val VALUE_PROPERTIES = setOf(
            VehiclePropertyIds.EV_BATTERY_LEVEL,
            VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY,
            VehiclePropertyIds.RANGE_REMAINING,
            VehiclePropertyIds.PERF_ODOMETER,
            VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE,
            VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED,
            VehiclePropertyIds.FUEL_LEVEL,
            VehiclePropertyIds.INFO_FUEL_CAPACITY,
            VehiclePropertyIds.PERF_VEHICLE_SPEED,
            VehiclePropertyIds.GEAR_SELECTION,
            VehiclePropertyIds.PARKING_BRAKE_ON,
            VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE,
            VehiclePropertyIds.TIRE_PRESSURE,
            VehiclePropertyIds.CRITICALLY_LOW_TIRE_PRESSURE,
        )

        val UNIT_PROPERTIES = setOf(
            VehiclePropertyIds.DISTANCE_DISPLAY_UNITS,
            VehiclePropertyIds.VEHICLE_SPEED_DISPLAY_UNITS,
            VehiclePropertyIds.TIRE_PRESSURE_DISPLAY_UNITS,
            VehiclePropertyIds.HVAC_TEMPERATURE_DISPLAY_UNITS,
        )

        val WANTED = VALUE_PROPERTIES + UNIT_PROPERTIES
    }
}
