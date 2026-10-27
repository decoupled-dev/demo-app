# Design (AAOS climate & vehicle demo): Software Requirements

| Field | Value |
|---|---|
| Source repository | https://github.com/decoupled-dev/demo-app |
| Branch | `climate-vehicle-only` |
| Commit | `4389a0cb592dd713e590d030b33495ebebdc88f5` |
| Generated | 2026-10-27 |
| Method | Reverse-engineered from source code. Every requirement cites code evidence; nothing was built or run. |

All file paths are relative to the repository root. Kotlin sources live under `app/src/main/java/com/test/design/` (abbreviated below as `…/design/`). Unit tests live under `app/src/test/java/com/test/design/` (abbreviated as `test/…`).

---

## 1. Introduction and scope

### 1.1 System
`com.test.design` ("Design") is an Android Automotive OS (AAOS) application written in Kotlin with Jetpack Compose. A single activity (`MainActivity`) hosts two screens behind a navigation rail:

- **Climate**: dual-zone HVAC temperature, fan speed, airflow direction, A/C, power, auto, sync, recirculation, defrost, seat heat, seat ventilation, steering-wheel heat.
- **Vehicle**: energy (battery or fuel) gauge, range summary, tire pressure, gear, drive-mode selector.

Both screens read live values through `CarPropertyManager` when the Car service and permissions are available (the app calls this "live" mode). Otherwise they show built-in demo values ("simulated" or "demo" mode).

### 1.2 In scope
Code reachable from `MainActivity`, `ClimateControlScreen`, and `VehicleScreen`, plus the data layer (`…/design/data/car`), ViewModels, theme and motion code they use, the manifest, build files, unit tests, and the privileged-install script and allowlist.

### 1.3 Out of scope
The repository contains code that is compiled but not reachable from the two screens. It is mentioned here only for completeness and does not produce requirements:

- `presentation/ivi/vehicle/components/VehicleSystemsPanel.kt`, `VehicleDriveInsightsCard.kt`, `VehicleLiveSummary` (in `VehicleLivePanel.kt`), and `TripMeterGraphics.kt` (`OdometerGraphic`, `RangeGraphic`). None are called from `VehicleScreen`.
- `presentation/ivi/climate/components/ClimateZoneSelector.kt` and `ClimateFanSpeedCard` (the composable, not the file's `ClimateBlowerCard`).
- `presentation/ivi/common/SimulatedBadge.kt`.
- `vehicle/VehicleDriveModeLayout.kt` `layoutProfile()` (only `defaultRegenLevel()` is used).
- `core/mvi/MviViewModel.kt`, `theme/OemBrand*.kt`, media/thermal-hub shape radii in `theme/MorphingRoundedShape.kt`.
- `.cursor/` cloud-agent tooling, `build.ps1`, `scripts/lib/device-safety.*`.

---

## 2. Glossary

| Term | Meaning |
|---|---|
| AAOS | Android Automotive OS. |
| IVI | In-vehicle infotainment. The package path `presentation/ivi` holds the screens. |
| VHAL | Vehicle Hardware Abstraction Layer. Exposes vehicle properties to Android. |
| CarPropertyManager | `android.car.hardware.property.CarPropertyManager`. Reads, writes, and subscribes to VHAL properties. |
| Property ID | A `VehiclePropertyIds` constant, e.g. `HVAC_TEMPERATURE_SET`. |
| Area ID | Identifies which seat, window, or wheel a property value applies to (e.g. `VehicleAreaSeat.SEAT_ROW_1_LEFT`, `VehicleAreaWheel.WHEEL_LEFT_FRONT`). |
| configArray | Integer array in `CarPropertyConfig`. For `HVAC_TEMPERATURE_SET` it encodes Celsius/Fahrenheit ranges and steps × 10. |
| Live mode | `ClimateUiState.isLive` / `VehicleUiState.isLive` is `true`; values come from the VHAL. |
| Simulated / demo mode | `isLive` is `false`; values are hard-coded defaults and changed only locally. |
| Capabilities | `ClimateCapabilities`: which HVAC controls the VHAL exposes. Drives control visibility. |
| Sync | Dual-zone link. Mapped to `HVAC_DUAL_ON`. |
| Driving UX state | `DrivingUxState` enum: `Parked`, `Driving`, `Restricted`. |
| Distraction optimized | Manifest metadata `distractionOptimized=true` telling AAOS the activity may run while driving. |
| privapp | Privileged system app installed under `/system/priv-app` with a permissions allowlist. |
| EARS | Easy Approach to Requirements Syntax (WHEN / WHILE / IF…THEN / WHERE / ubiquitous). |
| dp / sp | Density-independent pixels / scale-independent pixels. |

---

## 3. System context and assumptions

- **Runtime**: AAOS device or emulator, API 34 or higher (`minSdk = 34`). The manifest requires `android.hardware.type.automotive`.
- **Car library**: `android.car` is provided by the platform at runtime. The app compiles against `app/libs/android.car.jar` (`compileOnly`) and declares `<uses-library android:name="android.car" android:required="false" />`.
- **External system**: Vehicle HAL, accessed only through `Car.createCar(...)` → `Car.PROPERTY_SERVICE` → `CarPropertyManager`.
- **Actors**: Driver, front passenger (seat-heat and temperature zone), the AAOS system (intents, Car service), and a developer/demo operator (adb launch, demo gauge tap).
- **Permissions**: Car permissions are declared in the manifest and are intended to be granted through a privileged install (`scripts/install-climate-privapp.ps1`, `scripts/privapp-permissions-com.test.design.xml`). The app makes no runtime permission requests (no `requestPermissions` or `checkSelfPermission` calls in `app/src/main`).
- **Assumption (from code comments)**: Area IDs follow the AOSP default VHAL (`HvacWindowArea` comment in `data/car/HvacConstants.kt`). This VHAL treats `HVAC_DUAL_ON` as "link all seat temperatures" (comment in `CarClimateRepository.setSyncEnabled`).
- **Assumption (from tests)**: Target panel is 2560×1600 px at 220–320 dpi (`test/…/climate/TemperatureScaleTest.kt` `zoneWidthDp`, `test/…/vehicle/TireChassisLayoutTest.kt` `chassisSizeDp`).

---

## 4. Functional requirements

### 4.1 Climate (`REQ-CLIM-`)

### REQ-CLIM-001: Dual-zone temperature display
**User story:** As a driver or passenger, I want to see each front zone's set-point, so that I know the current cabin target.
**Status:** Implemented
**Acceptance criteria:**
1. WHERE `capabilities.hasDriverTemp` is true THE SYSTEM SHALL show a temperature dial labelled "Driver" bound to `temperatureCelsius`.
2. WHERE `capabilities.hasPassengerTemp` is true THE SYSTEM SHALL show a temperature dial labelled "Passenger" bound to `passengerTemperatureCelsius`.
3. IF neither `hasDriverTemp` nor `hasPassengerTemp` is true THEN THE SYSTEM SHALL hide the temperature band.
4. WHILE `WindowLayoutInfo.useSideBySide` is true (width ≥ 700 dp and width ≥ 0.85 × height) and both zones are available THE SYSTEM SHALL place the dials side by side; otherwise it SHALL stack them vertically and make the screen vertically scrollable.
5. WHILE simulated THE SYSTEM SHALL start with driver 22 °C and passenger 21 °C (`ClimateUiState` defaults).
**Evidence:** `…/design/presentation/ivi/climate/ClimateControlScreen.kt` (`ClimateControlScreen`, `DualZoneTemperatureBand`), `…/climate/ClimateContract.kt` (`ClimateUiState`, `ClimateZone`), `…/design/theme/WindowLayout.kt` (`WindowBreakpoints.SideBySideMin = 700.dp`, `useSideBySide`), `test/…/theme/WindowLayoutInfoTest.kt` (`expandedLandscape_usesSideBySide`, `compactWidthClass`)

### REQ-CLIM-002: Step temperature with +/- buttons
**User story:** As a driver, I want to raise or lower a zone's temperature in fixed steps, so that I can reach a precise set-point.
**Status:** Implemented
**Acceptance criteria:**
1. WHEN the user taps "Increase temperature" or "Decrease temperature" on a zone dial THE SYSTEM SHALL emit `ClimateEvent.AdjustZoneTemperature(zone, +1 | -1)`.
2. WHEN a zone is adjusted by `deltaSteps` THE SYSTEM SHALL compute the new Celsius value as `current + deltaSteps × temperatureStepCelsius`, snapped to `min + n × step` and clamped to [`minTemperature`, `maxTemperature`] (`stepTemperature`, `snapTemperature`).
3. THE SYSTEM SHALL step the Celsius set-point by `temperatureStepCelsius` even when Fahrenheit is displayed.
4. IF the zone value is ≥ `maxTemperature − 0.01` THEN THE SYSTEM SHALL disable the increase button; IF it is ≤ `minTemperature + 0.01` THEN THE SYSTEM SHALL disable the decrease button.
5. WHEN a zone is adjusted THE SYSTEM SHALL set `activeZone` to that zone.
6. WHILE simulated THE SYSTEM SHALL update `ClimateUiState` directly; WHILE live THE SYSTEM SHALL call `CarClimateRepository.setZoneTemperature` and wait for the VHAL change event to update the UI.
**Evidence:** `…/climate/components/ClimateTemperatureSection.kt` (`ClimateTemperatureDialContent`, `canIncrease`, `canDecrease`, `TemperatureAdjustButton`), `…/climate/ClimateContract.kt` (`stepTemperature`, `snapTemperature`), `…/climate/ClimateViewModel.kt` (`adjustZoneTemperature`, `nextTemp`)

### REQ-CLIM-003: Drag the temperature scale
**User story:** As a driver, I want to drag the tick scale under a fixed needle, so that I can change temperature quickly.
**Status:** Implemented
**Acceptance criteria:**
1. WHEN the user drags horizontally on a zone's `TemperatureScale` THE SYSTEM SHALL convert accumulated drag into steps of one tick per `TickSpacing = 12.dp`, with drag to the right producing cooler values (`temperatureScaleDragSteps` returns `-(dragPx / spacingPx).toInt()`).
2. IF the needle is at the lowest tick and the drag continues toward cooler, or at the highest tick and continues toward warmer, THEN THE SYSTEM SHALL discard the residual drag and emit no step.
3. THE SYSTEM SHALL clamp each emitted step count to the remaining ticks between the current index and the scale ends.
4. WHEN a non-zero step count is emitted THE SYSTEM SHALL play a haptic tick and send `AdjustZoneTemperature(zone, steps)`.
5. THE SYSTEM SHALL draw Celsius ticks every 0.5 °C and Fahrenheit ticks every `temperatureStepCelsius` (minimum 0.1) (`temperatureScaleSegmentStep`).
6. THE SYSTEM SHALL label whole-degree ticks divisible by 4 in Celsius and by 8 in Fahrenheit (`temperatureScaleLabelGap`, `temperatureScaleIsMajorTick`), and SHALL skip a label that would overlap the previous label plus an 8 dp gap (`temperatureScaleLabelFits`).
**Evidence:** `…/climate/components/TemperatureScale.kt` (`TemperatureScale`, `TickSpacing`), `…/climate/ClimateContract.kt` (`temperatureScaleDragSteps`, `temperatureScaleIndex`, `temperatureScaleSegmentStep`, `temperatureScaleLabelGap`, `temperatureScaleIsMajorTick`), `…/climate/TemperatureDialMetrics.kt` (`temperatureScaleLabelFits`), `test/…/climate/TemperatureScaleTest.kt` (`indexCountsStepsFromTheMinimum`, `dragRightSelectsCooler`, `labelsFollowTheUnit`, `celsiusUsesHalfDegreeSegmentsAndFahrenheitUsesWholeSteps`, `labelsSkipWhenTheyWouldCollide`)

### REQ-CLIM-004: Temperature range and step limits
**User story:** As a driver, I want the controls to respect the vehicle's supported temperature range, so that I cannot request an unsupported set-point.
**Status:** Implemented
**Acceptance criteria:**
1. WHILE simulated THE SYSTEM SHALL use `minTemperature = 16f`, `maxTemperature = 30f`, `temperatureStepCelsius = 0.5f`, `temperatureStepFahrenheit = 1f`, `minTemperatureFahrenheit = null`.
2. WHILE live THE SYSTEM SHALL use the min, max, and step values reported by `CarClimateRepository` (see REQ-CAR-003).
3. WHEN the repository writes a zone temperature THE SYSTEM SHALL snap it to the VHAL step and clamp it to the VHAL range before calling `setFloatProperty` (`CarClimateRepository.snapToStep`).
4. WHEN live values are read at connect THE SYSTEM SHALL clamp driver and passenger temperatures to [`minTempC`, `maxTempC`].
**Evidence:** `…/climate/ClimateContract.kt` (`ClimateUiState`), `…/climate/ClimateViewModel.kt` (`applyConnection`), `…/design/data/car/CarClimateRepository.kt` (`setZoneTemperature`, `snapToStep`, `readAll`, `HvacAreas` defaults)

### REQ-CLIM-005: Temperature display format
**User story:** As a driver, I want temperatures shown in my unit with the same rounding as the system bar, so that values match across the car.
**Status:** Implemented
**Acceptance criteria:**
1. WHILE the unit is Celsius and `temperatureStepCelsius < 0.999` THE SYSTEM SHALL format the value with one decimal and the suffix "°C" (e.g. `22.5°C`); otherwise it SHALL round to an integer.
2. WHILE the unit is Fahrenheit THE SYSTEM SHALL display `minF + round((C − minC) / stepC) × stepF`, rounded to an integer, with suffix "°F" (`celsiusToFahrenheitDisplay`).
3. IF `minTemperatureFahrenheit` is null THEN THE SYSTEM SHALL use `minC × 9/5 + 32` as `minF`.
4. IF `celsiusStep ≤ 0` or `fahrenheitStep ≤ 0` THEN THE SYSTEM SHALL fall back to `C × 9/5 + 32`.
5. WHEN the displayed value changes THE SYSTEM SHALL animate the numeral with a vertical slide whose direction follows the sign of the change (`AnimatedTemperatureCounter`, `defaultSpatialSpec`).
**Evidence:** `…/climate/ClimateContract.kt` (`formatTemperature`, `toDisplayTemperature`, `celsiusToFahrenheitDisplay`), `…/climate/components/AnimatedTemperatureCounter.kt`

### REQ-CLIM-006: Toggle temperature unit
**User story:** As a driver, I want to switch between °C and °F, so that I can read temperatures in my preferred unit.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL show a selected `FilterChip` in the climate header with the current unit label ("°C" or "°F").
2. WHEN the user taps the unit chip THE SYSTEM SHALL play a haptic tick and send `ClimateEvent.ToggleTemperatureUnit`.
3. WHILE live and `capabilities.hasTemperatureUnit` is true THE SYSTEM SHALL write `VehicleUnit.CELSIUS` or `VehicleUnit.FAHRENHEIT` to `HVAC_TEMPERATURE_DISPLAY_UNITS` (area ID `0`).
4. IF not live, or live without `hasTemperatureUnit`, THEN THE SYSTEM SHALL toggle `temperatureUnit` in local state only.
5. WHEN `HVAC_TEMPERATURE_DISPLAY_UNITS` changes to `VehicleUnit.FAHRENHEIT` THE SYSTEM SHALL show Fahrenheit; for any other value it SHALL show Celsius.
**Evidence:** `…/climate/ClimateControlScreen.kt` (`FilterChip` in `WidgetScreenHeader` `trailingContent`), `…/climate/ClimateViewModel.kt` (`ToggleTemperatureUnit`), `…/data/car/CarClimateRepository.kt` (`setTemperatureUnit`, `applyPropertyValue`)

### REQ-CLIM-007: Sync zones
**User story:** As a driver, I want to link both zones, so that one adjustment changes both seats.
**Status:** Implemented
**Acceptance criteria:**
1. WHERE `capabilities.hasSync` is true THE SYSTEM SHALL show a "Sync" toggle (content description "Sync zones on/off").
2. WHILE sync is enabled and a zone is adjusted THE SYSTEM SHALL apply the same step delta to both the driver and passenger set-points, each from its own current value.
3. WHILE sync is disabled THE SYSTEM SHALL change only the adjusted zone.
4. WHEN sync is turned on in live mode THE SYSTEM SHALL write `HVAC_DUAL_ON = true` and then write the current driver temperature to the passenger `HVAC_TEMPERATURE_SET` area.
5. WHEN sync is turned off in live mode THE SYSTEM SHALL write `HVAC_DUAL_ON = false`.
6. WHILE simulated THE SYSTEM SHALL start with sync enabled (`isSyncEnabled = true`).
**Evidence:** `…/climate/ClimateViewModel.kt` (`ToggleSync`, `adjustZoneTemperature`), `…/data/car/CarClimateRepository.kt` (`setSyncEnabled`), `…/climate/components/ClimateComfortControls.kt` (`ComfortIconToggle` "Sync")

### REQ-CLIM-008: Fan speed control
**User story:** As a driver, I want to set the blower level, so that I control airflow strength.
**Status:** Implemented
**Acceptance criteria:**
1. WHERE `capabilities.hasFanSpeed` is true THE SYSTEM SHALL show a fan control with a readout, a "Decrease fan speed" button, a track, and an "Increase fan speed" button.
2. THE SYSTEM SHALL display "Off" when the speed is 0 and the numeric level otherwise.
3. WHEN the user taps or drags on the track THE SYSTEM SHALL select `round(x / width × maxFanSpeed)` clamped to [0, `maxFanSpeed`] (`fanSpeedFromTrackPosition`) and send `SetFanSpeed` only when the value differs from the last value sent.
4. IF the speed is 0 THEN THE SYSTEM SHALL disable the decrease button; IF the speed equals `maxFanSpeed` THEN THE SYSTEM SHALL disable the increase button.
5. WHEN `SetFanSpeed(speed)` is received THE SYSTEM SHALL clamp it to [0, `maxFanSpeed`]; WHILE simulated it SHALL store it locally; WHILE live it SHALL call `CarClimateRepository.setFanSpeed`.
6. THE SYSTEM SHALL expose the track to accessibility as "Fan off" or "Fan speed N of M".
7. WHILE the speed is above 0 THE SYSTEM SHALL rotate the fan icon at `360 / (2.6 − t × 2.25)` degrees per second, where `t` is the normalised step (`fanDegreesPerSecond`).
8. WHILE simulated THE SYSTEM SHALL start at `fanSpeed = 3` of `maxFanSpeed = 5`.
**Evidence:** `…/climate/components/ClimateFanSpeedCard.kt` (`ClimateBlowerCard`, `FanSpeedControl`, `FanSpeedTrack`, `FanStepButton`, `SpinningFanIcon`, `fanDegreesPerSecond`), `…/climate/ClimateContract.kt` (`fanSpeedFromTrackPosition`), `…/climate/ClimateViewModel.kt` (`SetFanSpeed`), `test/…/climate/FanSpeedTrackTest.kt` (`trackPositionSnapsToFanSteps`)

### REQ-CLIM-009: Fan speed write to the VHAL
**User story:** As a driver, I want fan changes applied to every cabin fan area, so that the whole cabin responds.
**Status:** Implemented
**Acceptance criteria:**
1. WHEN a live fan speed is written THE SYSTEM SHALL clamp it to [0, `maxFan`].
2. IF the clamped speed is 0 and the VHAL minimum fan speed is above 0 THEN THE SYSTEM SHALL turn HVAC power off (`setPower(false)`) instead of writing `HVAC_FAN_SPEED`.
3. OTHERWISE THE SYSTEM SHALL write `max(speed, minFan)` to `HVAC_FAN_SPEED` for every discovered fan area, and SHALL ensure HVAC power is on for that area when the written value is above 0.
4. IF no fan areas are discovered THEN THE SYSTEM SHALL write nothing.
**Evidence:** `…/data/car/CarClimateRepository.kt` (`setFanSpeed`, `ensurePowerOn`)

### REQ-CLIM-010: Airflow direction
**User story:** As a driver, I want to choose where air is directed, so that I can target face, feet, or windshield.
**Status:** Implemented
**Acceptance criteria:**
1. WHERE `capabilities.hasFanDirection` is true THE SYSTEM SHALL show a segmented control with modes Face, Bi-level, Feet, and Defrost (`availableAirflowModes`).
2. WHEN the user selects a different segment THE SYSTEM SHALL play a haptic confirm, update `airflowMode` in local state immediately, and (WHILE live) call `setAirflowMode`.
3. WHEN a non-Auto mode is written live THE SYSTEM SHALL set `HVAC_AUTO_ON = false` on all auto areas, ensure power is on, and write `HVAC_FAN_DIRECTION` with `FACE = 0x1`, `FACE_AND_FLOOR = 0x3`, `FLOOR = 0x2`, or `FLOOR_AND_DEFROST = 0x6` to every fan-direction area.
4. WHEN the VHAL reports a fan direction THE SYSTEM SHALL map it as: DEFROST and FLOOR bits → Defrost; exactly DEFROST → Defrost; FACE and FLOOR bits → Bi-level; FLOOR bit → Feet; otherwise Face (`directionToAirflow`).
5. WHILE `HVAC_AUTO_ON` is true THE SYSTEM SHALL report `AirflowMode.Auto` regardless of fan direction.
6. WHILE the airflow mode is Auto THE SYSTEM SHALL show no selected segment.
7. WHEN the selection changes THE SYSTEM SHALL slide the indicator with `slowSpatialSpec`.
**Evidence:** `…/climate/components/MorphingAirflowSegmentedButton.kt`, `…/climate/ClimateContract.kt` (`AirflowMode`, `ClimateCapabilities.availableAirflowModes`), `…/data/car/HvacConstants.kt` (`HvacFanDirection`), `…/data/car/CarClimateRepository.kt` (`setAirflowMode`, `directionToAirflow`, `applyPropertyValue`)

### REQ-CLIM-011: Auto climate
**User story:** As a driver, I want an Auto mode, so that the car manages airflow for me.
**Status:** Implemented
**Acceptance criteria:**
1. WHERE `capabilities.hasAuto` is true THE SYSTEM SHALL show an "Auto" toggle that is active when `airflowMode == AirflowMode.Auto`.
2. WHEN the user taps Auto while it is off THE SYSTEM SHALL send `SelectAirflow(AirflowMode.Auto)`; WHEN tapped while on THE SYSTEM SHALL send `SelectAirflow(AirflowMode.Face)`.
3. WHEN Auto is written live THE SYSTEM SHALL ensure power is on and write `HVAC_AUTO_ON = true` on every auto area.
4. WHEN `HVAC_AUTO_ON` changes to false THE SYSTEM SHALL re-read `HVAC_FAN_DIRECTION` and show the mapped mode, or Face if the read fails and the previous mode was Auto.
**Evidence:** `…/climate/ClimateControlScreen.kt` (`onToggleAuto`), `…/climate/components/ClimateComfortControls.kt` ("Auto climate"), `…/data/car/CarClimateRepository.kt` (`setAirflowMode`, `applyPropertyValue` `HVAC_AUTO_ON`)

### REQ-CLIM-012: A/C toggle
**User story:** As a driver, I want to switch A/C on or off, so that I can cool or save energy.
**Status:** Implemented
**Acceptance criteria:**
1. WHERE `capabilities.hasAc` is true THE SYSTEM SHALL show an A/C icon, the text "A/C", and a `Switch` in the climate header.
2. WHEN the switch is toggled THE SYSTEM SHALL play a toggle haptic and send `ClimateEvent.ToggleAc`.
3. WHILE live THE SYSTEM SHALL write `HVAC_AC_ON` to every discovered A/C area; WHILE simulated it SHALL flip `isAcEnabled` locally (default `true`).
4. WHEN `isAcEnabled` changes THE SYSTEM SHALL morph the temperature dial corners between `ClimateDialCompactRadii` (56 dp uniform) and `ClimateDialExpandedRadii` (72/32/72/32 dp), and the blower and comfort cards between `ClimateCardRestRadii` and `ClimateCardActiveRadii`.
**Evidence:** `…/climate/ClimateControlScreen.kt` (`Switch`, `rememberClimateDialShape`), `…/climate/ClimateViewModel.kt` (`ToggleAc`), `…/data/car/CarClimateRepository.kt` (`setAcEnabled`), `…/design/theme/MorphingRoundedShape.kt`

### REQ-CLIM-013: HVAC power
**User story:** As a driver, I want to turn the climate system on or off, so that I control whether HVAC runs.
**Status:** Implemented
**Acceptance criteria:**
1. WHERE `capabilities.hasPower` is true THE SYSTEM SHALL show a "Power" toggle (content description "Climate power on/off").
2. WHEN toggled live THE SYSTEM SHALL write `HVAC_POWER_ON` to every discovered power area; WHILE simulated it SHALL flip `isPowerOn` locally (default `true`).
3. WHEN the system writes a zone temperature, a non-zero fan speed, or an airflow mode THE SYSTEM SHALL first read `HVAC_POWER_ON` for the matching (or first) power area and write `true` if it reads `false` (`ensurePowerOn`).
**Evidence:** `…/climate/components/ClimateComfortControls.kt` ("Climate power"), `…/climate/ClimateViewModel.kt` (`TogglePower`), `…/data/car/CarClimateRepository.kt` (`setPower`, `ensurePowerOn`)

### REQ-CLIM-014: Recirculation and fresh air
**User story:** As a driver, I want to choose recirculated or fresh air, so that I can manage cabin air quality.
**Status:** Implemented
**Acceptance criteria:**
1. WHERE `capabilities.hasRecirculation` is true THE SYSTEM SHALL show two toggles: "Fresh" (active when recirculation is off) and "Recirc" (active when recirculation is on).
2. WHEN the user taps "Recirc" THE SYSTEM SHALL send `ToggleRecirculation`.
3. WHEN the user taps "Fresh" while recirculation is on THE SYSTEM SHALL send `ToggleRecirculation`; WHILE recirculation is off, tapping "Fresh" SHALL change nothing.
4. WHILE live THE SYSTEM SHALL write `HVAC_RECIRC_ON` to every recirculation area; WHILE simulated it SHALL flip `isRecirculationOn` locally (default `false`).
**Evidence:** `…/climate/components/ClimateComfortControls.kt` ("Fresh air", "Recirculation"), `…/climate/ClimateViewModel.kt` (`ToggleRecirculation`), `…/data/car/CarClimateRepository.kt` (`setRecirculation`)

### REQ-CLIM-015: Front and rear defrost
**User story:** As a driver, I want to defrost the windshield and rear window, so that I can see clearly.
**Status:** Implemented
**Acceptance criteria:**
1. WHERE `capabilities.hasFrontDefrost` is true THE SYSTEM SHALL show a "Front" toggle; WHERE `hasRearDefrost` is true THE SYSTEM SHALL show a "Rear" toggle.
2. WHEN toggled live THE SYSTEM SHALL write `HVAC_DEFROSTER` to the front area (`HvacWindowArea.FRONT_WINDSHIELD = 0x1`, else the lowest defroster area) or rear area (`REAR_WINDSHIELD = 0x2`, else another defroster area).
3. WHILE simulated THE SYSTEM SHALL flip `isFrontDefrostOn` / `isRearDefrostOn` locally (default `false`).
**Evidence:** `…/climate/components/ClimateComfortControls.kt` ("Front defrost", "Rear defrost"), `…/climate/ClimateViewModel.kt`, `…/data/car/CarClimateRepository.kt` (`setFrontDefrost`, `setRearDefrost`, `discoverAreas`), `…/data/car/HvacConstants.kt` (`HvacWindowArea`)

### REQ-CLIM-016: Seat heat (driver and passenger)
**User story:** As a driver or passenger, I want to cycle my seat heater level, so that I stay warm.
**Status:** Implemented
**Acceptance criteria:**
1. WHERE `capabilities.hasSeatHeat` is true THE SYSTEM SHALL show a "Driver" seat-heat button left of the comfort card; WHERE `hasPassengerSeatHeat` is true THE SYSTEM SHALL show a "Passenger" button on the right.
2. WHEN a seat-heat button is tapped THE SYSTEM SHALL advance the level by 1 and wrap to 0 after `maxSeatHeatLevel` (`nextLevel`).
3. WHILE live THE SYSTEM SHALL write the level to `HVAC_SEAT_TEMPERATURE` for the driver area (preferring `SEAT_ROW_1_LEFT`) or the passenger area (the first other seat area).
4. WHEN live seat-heat values are read THE SYSTEM SHALL clamp them to [0, `maxSeatHeatLevel`], ignoring negative (cooling) values.
5. THE SYSTEM SHALL use VHAL min/max for the driver seat area when available, otherwise 0..3.
6. WHILE simulated THE SYSTEM SHALL start with driver level 1 and passenger level 0 (`ClimateUiState.seatHeatLevel = 1`, `passengerSeatHeatLevel = 0`).
**Evidence:** `…/climate/components/ClimateComfortControls.kt` (`SeatHeatButton`), `…/climate/ClimateViewModel.kt` (`CycleSeatHeat`, `CyclePassengerSeatHeat`, `nextLevel`), `…/data/car/CarClimateRepository.kt` (`setSeatHeatLevel`, `discoverAreas`, `readAll`, `applyPropertyValue`)

### REQ-CLIM-017: Seat-heat state presentation
**User story:** As an occupant, I want the seat-heat level to be readable at a glance, so that I know how warm the seat is set.
**Status:** Implemented
**Acceptance criteria:**
1. WHERE the heater has 3 steps THE SYSTEM SHALL label levels 0–3 as "Off", "Low", "Med", "High"; for other step counts it SHALL label 0 as "Off" and others by number, clamping the level to the step count.
2. THE SYSTEM SHALL show a meter with one bar per step, lighting bars below the current level.
3. THE SYSTEM SHALL interpolate the fill colour from low (`0xFFFF7043`) to high (`0xFFD84315` light / `0xFFFF3D00` dark) by `level / maxLevel`.
4. THE SYSTEM SHALL expose the button with role Button and content description "<Driver|Passenger> seat heat, <state label>".
**Evidence:** `…/climate/SeatHeatAppearance.kt` (`seatHeatStateLabel`, `seatHeatIntensity`, `seatHeatColors`), `…/climate/components/ClimateComfortControls.kt` (`SeatHeatButton`, `SeatHeatLevelMeter`), `test/…/climate/SeatHeatAppearanceTest.kt` (all four tests)

### REQ-CLIM-018: Steering-wheel heat
**User story:** As a driver, I want to heat the steering wheel, so that my hands stay warm.
**Status:** Implemented
**Acceptance criteria:**
1. WHERE `capabilities.hasSteeringHeat` is true THE SYSTEM SHALL show a "Wheel" level button with content description "Wheel heat, level N" or "Wheel heat, off".
2. WHEN tapped THE SYSTEM SHALL cycle the level 0..`maxSteeringHeatLevel` with wrap to 0.
3. WHILE live THE SYSTEM SHALL write `HVAC_STEERING_WHEEL_HEAT` on the first discovered area; range defaults to 0..3.
**Evidence:** `…/climate/components/ClimateComfortControls.kt` (`ComfortIconLevelButton`), `…/climate/ClimateViewModel.kt` (`CycleSteeringHeat`), `…/data/car/CarClimateRepository.kt` (`setSteeringHeatLevel`)

### REQ-CLIM-019: Seat ventilation (driver)
**User story:** As a driver, I want to ventilate my seat, so that I stay cool.
**Status:** Implemented
**Acceptance criteria:**
1. WHERE `capabilities.hasSeatVent` is true THE SYSTEM SHALL show a "Vent" level button with content description "Seat vent, level N" or "Seat vent, off".
2. WHEN tapped THE SYSTEM SHALL cycle 0..`maxSeatVentLevel` with wrap to 0.
3. WHILE live THE SYSTEM SHALL write `HVAC_SEAT_VENTILATION` to the driver seat area (preferring `SEAT_ROW_1_LEFT`); range defaults to 0..3.
**Evidence:** `…/climate/components/ClimateComfortControls.kt`, `…/climate/ClimateViewModel.kt` (`CycleSeatVent`), `…/data/car/CarClimateRepository.kt` (`setSeatVentLevel`)

### REQ-CLIM-020: Capability-driven control visibility
**User story:** As a driver, I want to see only controls my vehicle supports, so that I do not press controls that do nothing.
**Status:** Implemented
**Acceptance criteria:**
1. WHILE live THE SYSTEM SHALL derive each `ClimateCapabilities` flag from whether the VHAL exposes area IDs for the matching property (`HvacAreas.toCapabilities`).
2. WHILE simulated THE SYSTEM SHALL use `ClimateCapabilities()` defaults: every control flag `true` except `hasTemperatureUnit = false`.
3. IF neither fan speed nor airflow controls are available THEN THE SYSTEM SHALL hide the blower card.
4. IF `hasComfortControls` is false THEN THE SYSTEM SHALL hide the comfort row.
**Evidence:** `…/climate/ClimateContract.kt` (`ClimateCapabilities`), `…/data/car/CarClimateRepository.kt` (`HvacAreas.toCapabilities`), `…/climate/ClimateControlScreen.kt`, `…/climate/components/ClimateFanSpeedCard.kt` (`ClimateBlowerCard` early return)

### REQ-CLIM-021: Simulated climate mode
**User story:** As a demo operator, I want the climate screen to work without a vehicle, so that I can demonstrate the UI on any AAOS emulator.
**Status:** Implemented
**Acceptance criteria:**
1. WHILE `isLive` is false THE SYSTEM SHALL apply every climate event to local `ClimateUiState` only and SHALL not call the repository.
2. WHEN the connection becomes non-live THE SYSTEM SHALL reset `capabilities` to defaults, `temperatureStepCelsius` to 0.5, `temperatureStepFahrenheit` to 1, and `minTemperatureFahrenheit` to null, and SHALL keep all other state values.
**Evidence:** `…/climate/ClimateViewModel.kt` (`onEvent`, `applyConnection`)

### REQ-CLIM-022: Control feedback
**User story:** As a driver, I want tactile or audible confirmation of each control press, so that I do not need to look at the screen.
**Status:** Implemented
**Acceptance criteria:**
1. WHEN a step control is pressed (temperature +/-, fan +/-, fan track, scale drag step, seat/level buttons, unit chip) THE SYSTEM SHALL call `ClimateHaptics.tick()`: Compose `SegmentTick`, a vibrator `EFFECT_TICK` when a motor exists, and an 18 ms, 92 Hz click sound.
2. WHEN a toggle is switched THE SYSTEM SHALL call `toggle(on)`: `ToggleOn`/`ToggleOff`, `EFFECT_HEAVY_CLICK` (on) or `EFFECT_CLICK` (off), and the 28 ms/72 Hz or 18 ms/92 Hz click.
3. WHEN a different airflow segment is selected THE SYSTEM SHALL call `confirm()`.
4. THE SYSTEM SHALL play click sounds with `AudioAttributes.USAGE_ASSISTANT` and `CONTENT_TYPE_SONIFICATION` at volume 0.85.
5. IF vibration or audio fails THEN THE SYSTEM SHALL ignore the error.
**Evidence:** `…/climate/ClimateHaptics.kt` (`ClimateHaptics`, `buildClickTrack`, `rememberClimateHaptics`), call sites in `…/climate/components/*.kt` and `ClimateControlScreen.kt`

### REQ-CLIM-023: Temperature-driven colour and cool effect
**User story:** As an occupant, I want the screen tint to reflect cool or warm settings, so that the state is clear without reading numbers.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL map each zone temperature to a fraction `(C − min) / (max − min)` clamped to [0, 1] (`temperatureToFraction`).
2. THE SYSTEM SHALL derive the screen colour scheme from the average of the two zone fractions and each dial's scheme from its own fraction (`climateColorScheme`), keeping light canvases light and dark canvases dark.
3. THE SYSTEM SHALL animate the left/right edge glow colours with `defaultEffectsSpec`.
4. WHERE the screen is not compact THE SYSTEM SHALL draw a snowflake overlay per zone with intensity `(0.42 − own) / 0.42` clamped to [0, 1], and 0 when the zone is warmer than the other zone (`zoneCoolIntensity`, `ClimateCoolThresholdFraction = 0.42f`).
**Evidence:** `…/design/theme/DynamicClimateColor.kt` (`temperatureToFraction`, `zoneCoolIntensity`, `climateColorScheme`, `climateAmbientColor`), `…/climate/components/CoolSnowflakeOverlay.kt`, `…/climate/ClimateControlScreen.kt`, `test/…/theme/DynamicClimateColorTest.kt` (all three tests)

### REQ-CLIM-024: Temperature dial sizing
**User story:** As a driver, I want the temperature dial to fit the zone at any panel density, so that no control is clipped.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL size buttons at 76 dp with 24 dp spacing and a card up to 400 dp wide.
2. IF the card would be narrower than 220 dp THEN THE SYSTEM SHALL reduce spacing to 8 dp; IF still narrower than 148 dp THEN THE SYSTEM SHALL shrink buttons down to 48 dp.
3. IF the zone height cannot fit the 100 dp scale plus 52 sp readout line THEN THE SYSTEM SHALL scale the readout (font 22–44 sp, line 28–52 sp) and canvas (48–100 dp) to fit.
**Evidence:** `…/climate/TemperatureDialMetrics.kt` (`temperatureDialMetrics` and constants), `test/…/climate/TemperatureScaleTest.kt` (`dialFitsTheZoneFrom220To320Dpi`)

### 4.2 Vehicle info (`REQ-VEH-`)

### REQ-VEH-001: Energy level (state of charge or fuel)
**User story:** As a driver, I want to see remaining battery or fuel as a percentage, so that I can plan charging or refuelling.
**Status:** Implemented
**Acceptance criteria:**
1. WHEN `EV_BATTERY_LEVEL` and `INFO_EV_BATTERY_CAPACITY` are known and capacity ≥ 500 Wh THE SYSTEM SHALL show `round(remaining / capacity × 100)` clamped to 0–100.
2. IF capacity is unknown or below 500 Wh and the battery value is ≤ 100 THEN THE SYSTEM SHALL treat the value as a percentage; IF it is above 100 THEN THE SYSTEM SHALL report no battery percentage.
3. WHEN `FUEL_LEVEL` and `INFO_FUEL_CAPACITY` (> 1 mL) are known THE SYSTEM SHALL compute fuel percentage the same way; a value ≤ 100 without capacity SHALL be used as-is.
4. IF the battery percentage is unavailable and the fuel percentage is available THEN THE SYSTEM SHALL label the energy "Fuel"; otherwise "Battery".
5. WHILE live THE SYSTEM SHALL show the battery percentage, else the fuel percentage, else 0.
6. IF the battery or fuel reading is negative THEN THE SYSTEM SHALL treat it as unavailable.
7. WHILE simulated THE SYSTEM SHALL show 82 % with energy title "Energy".
**Evidence:** `…/design/data/car/VehicleSignals.kt` (`batteryPercent`, `fuelPercent`, `toDisplay`), `…/vehicle/VehicleLiveState.kt` (`withLiveSignals`), `…/vehicle/VehicleContract.kt` (`VehicleUiState`), `test/…/data/car/VehicleSignalsTest.kt` (`batteryPercentUsesCapacityWhenPackIsKnown`, `batteryPercentAcceptsDirectPercentageWithoutCapacity`, `batteryPercentIgnoresWattHoursWithoutCapacity`, `fuelPercentUsesTankCapacity`, `chargingFollowsPositiveRateAndFuelBecomesEnergyWhenNoBattery`), `test/…/vehicle/VehicleLiveStateTest.kt` (`livePackCapacityComesFromWattHours`)

### REQ-VEH-002: Energy gauge
**User story:** As a driver, I want a gauge showing charge and range, so that I can read energy state at a glance.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL draw a 270° arc starting at 135° whose fill is `percent / 100`, and a thinner range ring whose fill is `rangeMiles / RangeCapacityMiles` (`RangeCapacityMiles = 300`) clamped to [0, 1].
2. THE SYSTEM SHALL colour the arc cyan `0xFF00E5FF` while charging, `primary` at ≥ 60 %, `tertiary` at ≥ 30 %, and `error` below 30 %.
3. THE SYSTEM SHALL show header status "<N> kW" while charging, "Low" below 30 %, and "Ready" otherwise, and title "Charging" while charging.
4. WHILE charging THE SYSTEM SHALL pulse the gauge scale between 0.92 and 1.08 over 1400 ms and run an orbiting highlight over 2200 ms.
5. THE SYSTEM SHALL animate the percentage counter with a directional vertical slide.
**Evidence:** `…/vehicle/components/VehicleEnergyCockpit.kt` (`VehicleEnergyCockpit`, `GaugeWithStats`, `EnergyHeader`, `AnimatedStatCounter`, `GaugeStartDegrees`, `GaugeSweepDegrees`), `…/vehicle/TripMeter.kt` (`RangeCapacityMiles`)

### REQ-VEH-003: Range summary (remaining, average, capacity)
**User story:** As a driver, I want remaining range, average consumption, and pack capacity, so that I understand how far I can go.
**Status:** Implemented
**Acceptance criteria:**
1. WHEN `RANGE_REMAINING` (metres) is known THE SYSTEM SHALL show it converted to the display distance unit and rounded (e.g. "100 mi").
2. IF range is unavailable while live THEN THE SYSTEM SHALL show "—".
3. WHILE simulated THE SYSTEM SHALL show average consumption `round(1000 / efficiencyMpkWh)` Wh/mi (or ÷ 1.609344 for Wh/km when the range label contains "km"); default efficiency 3.8 mi/kWh gives "263 Wh/mi".
4. WHILE live THE SYSTEM SHALL show average consumption as "—".
5. WHEN `INFO_EV_BATTERY_CAPACITY` ≥ 500 Wh is known THE SYSTEM SHALL show capacity in kWh to one decimal; WHILE simulated without a reported capacity it SHALL estimate `rangeMiles / efficiency / (percent / 100)` (77.0 kWh for defaults); WHILE live without capacity it SHALL show "—".
6. THE SYSTEM SHALL show a "RANGE" bar whose fill and percentage equal the state-of-charge fraction.
**Evidence:** `…/vehicle/VehicleInfoReadout.kt` (`toInfoReadout`, `estimatedPackKwh`, `averageWattHours`, `formatPackKwh`), `…/vehicle/components/VehicleEnergyCockpit.kt` (`EnergyRangeSummary`, `RangeInstrument`), `…/data/car/VehicleSignals.kt` (`formatDistanceKm`), `test/…/vehicle/VehicleInfoReadoutTest.kt` (`demoReadoutUsesRangeEfficiencyAndDriveGear`, `liveReadoutUsesReportedSpeedGearAndCapacity`)

### REQ-VEH-004: Charging detection
**User story:** As a driver, I want the screen to show when the car is charging and at what rate, so that I can monitor charging.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL convert `EV_BATTERY_INSTANTANEOUS_CHARGE_RATE` to kW by dividing by 1,000,000 when |raw| ≥ 1,000,000 (milliwatts) and by 1,000 otherwise (watts).
2. WHEN the converted rate is above 0.2 kW THE SYSTEM SHALL treat the vehicle as charging.
3. WHEN `EV_CHARGE_PORT_CONNECTED` is true and the vehicle is not charging THE SYSTEM SHALL set the charge-port label to "Connected".
4. WHILE live THE SYSTEM SHALL show the charge rate only when it is above 0 kW, otherwise 0.
**Evidence:** `…/data/car/VehicleSignals.kt` (`chargeRateKilowatts`, `toDisplay`), `test/…/data/car/VehicleSignalsTest.kt` (`chargeRateTreatsLargeValuesAsMilliwatts`, `chargeRateTreatsSmallerValuesAsWatts`, `chargingFollowsPositiveRateAndFuelBecomesEnergyWhenNoBattery`)

### REQ-VEH-005: Tire pressure display
**User story:** As a driver, I want each tire's pressure and status, so that I can spot under- or over-inflation.
**Status:** Implemented
**Acceptance criteria:**
1. WHEN `TIRE_PRESSURE` values exist for all four wheel areas (`WHEEL_LEFT_FRONT`, `WHEEL_RIGHT_FRONT`, `WHEEL_LEFT_REAR`, `WHEEL_RIGHT_REAR`) THE SYSTEM SHALL show a corner card per tire with pressure converted from kPa to integer psi (`kPa / 6.894757`).
2. THE SYSTEM SHALL classify each tire as Critical when it is flagged critically low or below 23 psi, Warning when below 27 psi or above 42 psi, and Normal otherwise (`tireStatus`).
3. WHILE a tire is Warning or Critical THE SYSTEM SHALL show warning icons, use the status accent colour (`0xFFE0B03A` Warning, `0xFFE4572E` Critical), and pulse that tire's bar on the top-down image between 0.7 and 1.0 alpha over 1200 ms.
4. THE SYSTEM SHALL show the outside temperature label (with a space before "°") as each corner's temperature, or "—" when unknown, and the text "Just now" as the update time.
5. IF any of the four wheel readings is missing THEN THE SYSTEM SHALL show the fixed reference readout: FL 36 psi "45 °C" Normal "28 sec ago", FR 39 psi "44 °C" Critical "19 sec ago", RL 35 psi "38 °C" Normal "10 min ago", RR 37 psi "40 °C" Warning "3 sec ago".
**Evidence:** `…/vehicle/TireWireframe.kt` (`toTireWireframe`, `tireStatus`, `kiloPascalsToPsi`, `ReferenceWireframe`), `…/vehicle/components/TireWireframeGraphic.kt` (`TireCornerCard`, `TopDownCar`, `drawTireBar`), `…/vehicle/components/VehicleLivePanel.kt` (`VehicleLivePanel`), `test/…/vehicle/TireWireframeTest.kt` (`missingTiresUseTheReferenceReadout`, `liveCornersConvertKilopascalsToPsi`, `statusBandsFollowPlacardPressure`)

### REQ-VEH-006: Critically-low tire flag
**User story:** As a driver, I want dangerously low tires flagged, so that I stop before damage occurs.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL read `CRITICALLY_LOW_TIRE_PRESSURE` per wheel area into the repository reading.
2. WHEN tire signals are converted for display THE SYSTEM SHALL set `criticallyLow` to true when pressure is ≤ 0 kPa or < 180 kPa (`isTireCritical`), based on pressure only.
3. THE SYSTEM SHALL sort displayed tires front-left, front-right, rear-left, rear-right, other.
**Evidence:** `…/data/car/CarVehicleRepository.kt` (`apply` `CRITICALLY_LOW_TIRE_PRESSURE`, `publish`), `…/data/car/VehicleSignals.kt` (`toDisplay`, `isTireCritical`, `wheelOrder`), `test/…/data/car/VehicleSignalsTest.kt` (`displayFormatsTiresSpeedAndRangeFromCanonicalUnits`)

### REQ-VEH-007: Gear indicator
**User story:** As a driver, I want to see the selected gear, so that I know the transmission state.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL map `GEAR_SELECTION` values `GEAR_PARK`, `GEAR_REVERSE`, `GEAR_NEUTRAL`, `GEAR_DRIVE` to "P", "R", "N", "D"; any other value SHALL produce no gear label.
2. THE SYSTEM SHALL show a gear bar in the order N, D, R, P and slide a highlight to the selected gear with `slowSpatialSpec`.
3. WHILE simulated without a gear label THE SYSTEM SHALL highlight "D"; WHILE live without a gear label it SHALL highlight none.
4. THE SYSTEM SHALL expose the bar to accessibility as "Gear <label>" or "Gear unknown".
**Evidence:** `…/data/car/CarVehicleRepository.kt` (`gearKind`), `…/data/car/VehicleSignals.kt` (`VehicleGearKind.label`), `…/vehicle/VehicleInfoReadout.kt` (`VehicleGearOrder`, `selectedGear`), `…/vehicle/components/VehicleInfoCard.kt` (`GearBar`), `test/…/vehicle/VehicleInfoReadoutTest.kt`

### REQ-VEH-008: Drive-mode selector
**User story:** As a driver, I want to pick Eco, Comfort, or Sport, so that the display reflects my driving style.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL show a three-segment selector with Eco ("Max range"), Comfort ("Balanced"), and Sport ("Peak power"), defaulting to Comfort.
2. WHEN the user selects a mode THE SYSTEM SHALL send `VehicleEvent.SelectDriveMode` and update `driveMode` in both live and simulated mode.
3. WHILE simulated THE SYSTEM SHALL also set `regenLevel` to the mode default: Eco → High, Comfort → Standard, Sport → Low.
4. THE SYSTEM SHALL not write any vehicle property for drive mode.
5. WHEN the drive mode changes THE SYSTEM SHALL change the screen accent (Eco `0xFF3DDC84`, Comfort `0xFF7EB6FF`, Sport `0xFFFF6A3D` in the info card) and the SUV image bob period (3200 / 2200 / 1100 ms) and amplitude (1.5 / 2.5 / 4 dp).
6. WHEN the connection drops from live to not live THE SYSTEM SHALL keep the selected drive mode.
**Evidence:** `…/vehicle/VehicleContract.kt` (`DriveMode`), `…/vehicle/VehicleViewModel.kt` (`SelectDriveMode`), `…/vehicle/VehicleDriveModeLayout.kt` (`defaultRegenLevel`), `…/vehicle/components/MorphingDriveModeSelector.kt`, `…/vehicle/components/VehicleInfoCard.kt` (`driveAccent`, `AnimatedSuvArt`), `test/…/vehicle/VehicleLiveStateTest.kt` (`liveSignalsReplaceDemoValuesAndKeepDriveMode`, `disconnectRestoresDemoWithoutDroppingDriveMode`)

### REQ-VEH-009: Demo battery cycling
**User story:** As a demo operator, I want to tap the gauge to cycle battery states, so that I can show low, mid, and charging visuals.
**Status:** Implemented
**Acceptance criteria:**
1. WHILE simulated, WHEN the user taps the energy gauge, THE SYSTEM SHALL cycle the battery to 62 % (from ≥ 90), 45 % (from ≥ 70), or 88 % (otherwise).
2. WHEN the battery is cycled THE SYSTEM SHALL set range to `(percent × 2.9).toInt()` miles with label "<N> mi" and set charging to true when the new percentage is below 70.
3. WHILE live THE SYSTEM SHALL ignore gauge taps and SHALL drop the demo-only events `CycleBatteryDemo`, `ToggleCharging`, `CycleRegenLevel`, and `SelectSystem`.
**Evidence:** `…/vehicle/VehicleScreen.kt` (`onGaugeClick`), `…/vehicle/VehicleViewModel.kt` (`CycleBatteryDemo`, `isDemoOnly`)

### REQ-VEH-010: Live and demo state transition
**User story:** As a driver, I want real values to replace demo values as soon as the car reports them, so that I never act on fake data once live data exists.
**Status:** Implemented
**Acceptance criteria:**
1. WHILE `VehicleSignals.connected` is false and the UI was never live THE SYSTEM SHALL keep the demo state unchanged.
2. WHEN signals become connected THE SYSTEM SHALL set `isLive = true` and replace energy, range, odometer, charging, speed, gear, outside temperature, parking brake, charge port, tires, and pack capacity with live-derived values.
3. WHEN signals change from connected to disconnected THE SYSTEM SHALL restore a default `VehicleUiState`, keeping only `driveMode` and `selectedSystemId`.
4. WHILE simulated THE SYSTEM SHALL start at 82 %, 240 mi ("240 mi"), Comfort, 18,420 mi odometer, not charging, 11 kW.
**Evidence:** `…/vehicle/VehicleLiveState.kt` (`withLiveSignals`), `…/vehicle/VehicleContract.kt` (`VehicleUiState`), `test/…/vehicle/VehicleLiveStateTest.kt` (`liveSignalsReplaceDemoValuesAndKeepDriveMode`, `disconnectRestoresDemoWithoutDroppingDriveMode`, `demoStateIgnoresEmptySnapshot`)

### REQ-VEH-011: Display unit conversion
**User story:** As a driver, I want distances, speed, temperature, and pressure in the car's configured units, so that values match the instrument cluster.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL use `DISTANCE_DISPLAY_UNITS` (`VehicleUnit.MILE` → mi, `KILOMETER` → km), defaulting to kilometres when the vehicle is connected but does not report a unit.
2. THE SYSTEM SHALL use `VEHICLE_SPEED_DISPLAY_UNITS` (`MILES_PER_HOUR` / `KILOMETERS_PER_HOUR`) for speed, defaulting to mph only when distance is miles; it SHALL convert m/s × 2.236936 (mph) or × 3.6 (km/h) and round.
3. THE SYSTEM SHALL use `TIRE_PRESSURE_DISPLAY_UNITS` (`PSI`, `BAR`, `KILOPASCAL`), defaulting to kPa, formatting psi and kPa as integers and bar to one decimal (US locale).
4. THE SYSTEM SHALL show outside temperature in °F when `HVAC_TEMPERATURE_DISPLAY_UNITS` is `FAHRENHEIT`, otherwise °C, rounded (e.g. "21°C").
5. THE SYSTEM SHALL use conversion constants `METERS_PER_MILE = 1609.344`, `KM_PER_MILE = 1.609344`, `KPA_PER_PSI = 6.894757`, `KPA_PER_BAR = 100`.
**Evidence:** `…/data/car/CarVehicleRepository.kt` (`publish`, `distanceUnit`, `speedIsMph`, `pressureUnit`), `…/data/car/VehicleSignals.kt` (`formatDistanceKm`, `formatSpeed`, `formatTemperature`, `formatPressure`), `test/…/data/car/VehicleSignalsTest.kt` (`displayFormatsTiresSpeedAndRangeFromCanonicalUnits`)

### REQ-VEH-012: Vehicle dynamic colour scheme
**User story:** As a driver, I want the vehicle screen colours to reflect drive mode, low battery, and charging, so that state changes are visible peripherally.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL use a dark cockpit scheme keyed to drive mode (Eco primary `0xFF66BB6A`, Comfort `0xFF64B5F6`, Sport `0xFFFF7043`) with light content colours.
2. WHILE battery is below 30 % THE SYSTEM SHALL blend toward a red tint by `(0.3 − fraction) / 0.3`.
3. WHILE charging THE SYSTEM SHALL blend primary 55 % toward `0xFF00E5FF`.
4. WHEN the scheme changes THE SYSTEM SHALL cross-fade the screen tint with `defaultEffectsSpec`.
**Evidence:** `…/design/theme/VehicleDynamicColor.kt` (`vehicleColorScheme`, `batteryToFraction`), `…/vehicle/VehicleScreen.kt`

### REQ-VEH-013: Vehicle screen layout
**User story:** As a driver, I want energy, tires, and drive information side by side, so that I can see everything without scrolling.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL lay out three equal-weight columns: energy cockpit, tire panel, and drive/info card.
2. THE SYSTEM SHALL apply `CarDesignTokens.ContentPadding = 32.dp` and `TouchTargetSpacing = 24.dp` between columns (8 dp when compact).
**Evidence:** `…/vehicle/VehicleScreen.kt` (`Row` with three `weight(1f)` children), `…/design/theme/CarDesignTokens.kt`

### REQ-VEH-014: Secondary vehicle signal labels
**User story:** As a driver, I want speed, odometer, outside temperature, parking brake, and charge-port status, so that I have a full vehicle summary.
**Status:** Inferred (labels are computed and stored in `VehicleUiState`, but only `gearLabel` and `outsideTempLabel` are rendered by `VehicleScreen`; the display intent is inferred from the label fields and the unused `VehicleLiveSummary`.)
**Acceptance criteria:**
1. WHEN `PERF_VEHICLE_SPEED` is available THE SYSTEM SHALL derive `speedLabel` (e.g. "22 mph").
2. WHEN `PERF_ODOMETER` is available THE SYSTEM SHALL derive `odometerLabel` in the display distance unit (e.g. "10 mi").
3. WHEN `PARKING_BRAKE_ON` is available THE SYSTEM SHALL derive `parkingBrakeLabel` "On" or "Off".
4. WHEN `ENV_OUTSIDE_TEMPERATURE` is available THE SYSTEM SHALL derive `outsideTempLabel`.
**Evidence:** `…/data/car/VehicleSignals.kt` (`toDisplay`), `…/vehicle/VehicleLiveState.kt` (`withLiveSignals`), `…/vehicle/components/VehicleLivePanel.kt` (`VehicleLiveSummary`, `liveRows`; not called), `test/…/data/car/VehicleSignalsTest.kt` (`displayFormatsTiresSpeedAndRangeFromCanonicalUnits`)

### 4.3 Navigation and launch (`REQ-NAV-`)

### REQ-NAV-001: Navigation rail
**User story:** As a driver, I want a rail to switch between Climate and Vehicle, so that I can reach either screen with one tap.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL show a `NavigationRail` with one item per `DashboardWidget` entry: "Climate" (icon `Icons.Default.AcUnit`) and "Vehicle" (icon `Icons.Default.DirectionsCar`), in that order.
2. WHEN the user taps a rail item THE SYSTEM SHALL show that screen and mark the item selected.
3. WHEN the screen changes THE SYSTEM SHALL cross-fade old and new content (`fadeIn() togetherWith fadeOut()`).
4. THE SYSTEM SHALL host both screens with `showBack = false` and a no-op `onBack`.
**Evidence:** `…/design/MainActivity.kt` (`NavigationRail`, `AnimatedContent`), `…/presentation/ivi/dashboard/model/DashboardWidget.kt`

### REQ-NAV-002: Default screen
**User story:** As a driver, I want the app to open on Climate, so that the most-used controls are first.
**Status:** Implemented
**Acceptance criteria:**
1. WHEN `MainActivity` is created without a recognised action THE SYSTEM SHALL show `DashboardWidget.Climate`.
**Evidence:** `…/design/MainActivity.kt` (`selectedScreen = mutableStateOf(DashboardWidget.Climate)`, `applyIntent`)

### REQ-NAV-003: Intent-based screen launch
**User story:** As the system or an assistant, I want to open a specific screen by intent, so that voice or shortcuts land on the right controls.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL accept intent actions `com.test.design.action.OPEN_CLIMATE` (`ACTION_OPEN_CLIMATE`) and `com.test.design.action.OPEN_VEHICLE` (`ACTION_OPEN_VEHICLE`) with category `DEFAULT`.
2. WHEN `OPEN_CLIMATE` is received at creation or via `onNewIntent` THE SYSTEM SHALL select Climate; WHEN `OPEN_VEHICLE` is received THE SYSTEM SHALL select Vehicle.
3. WHILE the activity is running THE SYSTEM SHALL deliver new intents to the existing instance (`launchMode="singleTask"`, `taskAffinity="com.test.design.main"`).
**Evidence:** `app/src/main/AndroidManifest.xml` (second `intent-filter`, `launchMode`), `…/design/MainActivity.kt` (`ACTION_OPEN_CLIMATE`, `ACTION_OPEN_VEHICLE`, `onNewIntent`, `applyIntent`), `README.md` (adb examples)

### REQ-NAV-004: Launcher and home entry
**User story:** As a driver, I want to launch the app from the car launcher, so that I can open it without adb.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL export `MainActivity` with action `MAIN` and categories `HOME`, `DEFAULT`, `LAUNCHER`, and `APP_AUTOMOTIVE`.
2. THE SYSTEM SHALL use the label `@string/app_name` ("Design").
**Evidence:** `app/src/main/AndroidManifest.xml` (first `intent-filter`), `app/src/main/res/values/strings.xml`

### REQ-NAV-005: Configuration changes
**User story:** As a driver, I want the selected screen to stay put on rotation or density change, so that I do not lose my place.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL handle `orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden|density|uiMode` changes without recreating the activity.
2. THE SYSTEM SHALL keep climate and vehicle state in `ViewModel`s scoped to the activity.
**Evidence:** `app/src/main/AndroidManifest.xml` (`android:configChanges`), `…/design/MainActivity.kt` (`by viewModels()`)

### 4.4 Car property integration (`REQ-CAR-`)

### REQ-CAR-001: Car service connection lifecycle
**User story:** As the system, I want each repository to connect to the Car service once and disconnect cleanly, so that resources and callbacks are not leaked.
**Status:** Implemented
**Acceptance criteria:**
1. WHEN `ClimateViewModel` or `VehicleViewModel` is created THE SYSTEM SHALL call its repository's `start()`, which calls `Car.createCar(context, mainHandler, Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER, listener)` at most once (`AtomicBoolean started`).
2. IF `Car.createCar` throws THEN THE SYSTEM SHALL log a warning, allow a later retry (`started = false`), and publish a non-live state.
3. WHEN the listener reports `ready = true` THE SYSTEM SHALL obtain `Car.PROPERTY_SERVICE`; IF it is null or throws THEN THE SYSTEM SHALL publish a non-live state.
4. WHEN the listener reports `ready = false` THE SYSTEM SHALL unregister callbacks, clear discovered areas and readings, and publish a non-live state.
5. WHEN the ViewModel is cleared THE SYSTEM SHALL call `stop()`, which unregisters callbacks, disconnects the `Car`, and publishes a non-live state.
**Evidence:** `…/data/car/CarClimateRepository.kt` (`start`, `stop`, `onCarReady`, `onCarDisconnected`), `…/data/car/CarVehicleRepository.kt` (same functions), `…/climate/ClimateViewModel.kt` (`init`, `onCleared`), `…/vehicle/VehicleViewModel.kt` (`init`, `onCleared`)

### REQ-CAR-002: HVAC area discovery
**User story:** As the system, I want to discover which HVAC properties and areas the VHAL exposes, so that writes target real areas.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL read area IDs from `getCarPropertyConfig(propId).areaIds` for `HVAC_TEMPERATURE_SET`, `HVAC_FAN_SPEED`, `HVAC_FAN_DIRECTION`, `HVAC_AC_ON`, `HVAC_RECIRC_ON`, `HVAC_AUTO_ON`, `HVAC_POWER_ON`, `HVAC_DUAL_ON`, `HVAC_DEFROSTER`, `HVAC_SEAT_TEMPERATURE`, `HVAC_SEAT_VENTILATION`, `HVAC_STEERING_WHEEL_HEAT`, and `HVAC_TEMPERATURE_DISPLAY_UNITS`.
2. IF reading a config throws or returns null THEN THE SYSTEM SHALL treat that property as having no areas.
3. THE SYSTEM SHALL pick the driver temperature area as `SEAT_ROW_1_LEFT` if present, else the first area; the passenger area as `SEAT_ROW_1_RIGHT` if present, else the first area that is not the driver area.
4. THE SYSTEM SHALL restrict A/C, recirculation, power, and fan-speed areas to row-1 seats (`SEAT_ROW_1_LEFT`, `SEAT_ROW_1_RIGHT`, `SEAT_ROW_1_CENTER`) when any are present, else use all areas.
5. IF no HVAC control is accessible after discovery THEN THE SYSTEM SHALL log "No HVAC properties accessible (permission or VHAL)" and stay non-live.
**Evidence:** `…/data/car/CarClimateRepository.kt` (`discoverAreas`, `areaIds`, `cabinSeatAreas`, `preferredSeat`, `onCarReady`)

### REQ-CAR-003: HVAC range and step discovery
**User story:** As the system, I want temperature, fan, and heat ranges from the VHAL, so that the UI limits match the vehicle.
**Status:** Implemented
**Acceptance criteria:**
1. WHEN the `HVAC_TEMPERATURE_SET` `configArray` has ≥ 6 elements THE SYSTEM SHALL read `minC = [0]/10`, `maxC = [1]/10`, `stepC = [2]/10` (5 if ≤ 0), `minF = [3]/10`, `stepF = [5]/10` (10 if ≤ 0).
2. WHEN it has 3–5 elements THE SYSTEM SHALL read only the Celsius min, max, and step; WHEN it has 1–2 elements THE SYSTEM SHALL read `[0]` as the Celsius step × 10.
3. IF the `configArray` has no Celsius range THEN THE SYSTEM SHALL use the area min/max values, else defaults 16 °C and 30 °C.
4. THE SYSTEM SHALL read fan speed min/max (default 1..5), seat heat, seat vent, and steering heat min/max (default 0..3) from the first relevant area, clamping minimums and maximums to ≥ 0.
5. THE SYSTEM SHALL default the Celsius step to `DEFAULT_TEMP_STEP_C = 0.5f` and the Fahrenheit step to `DEFAULT_TEMP_STEP_F = 1f`.
**Evidence:** `…/data/car/CarClimateRepository.kt` (`temperatureConfig`, `TempConfig`, `floatRange`, `intRange`, `discoverAreas`, companion constants)

### REQ-CAR-004: HVAC initial snapshot
**User story:** As a driver, I want the climate screen to show current vehicle values on connect, so that the UI matches the cabin.
**Status:** Implemented
**Acceptance criteria:**
1. WHEN HVAC is connected and has at least one control THE SYSTEM SHALL register callbacks, then read every discovered property once and publish `isLive = true` with capabilities.
2. IF an individual read fails THEN THE SYSTEM SHALL use the fallback: 22 °C per zone, fan 1, A/C off, power on, recirculation off, defrost off, heat/vent levels 0, Celsius, Auto off.
3. WHERE `HVAC_DUAL_ON` exists and its read fails THE SYSTEM SHALL report sync off; WHERE it does not exist THE SYSTEM SHALL report sync on.
4. THE SYSTEM SHALL report `maxFanSpeed`, `maxSeatHeatLevel`, `maxSeatVentLevel`, and `maxSteeringHeatLevel` as at least 1.
**Evidence:** `…/data/car/CarClimateRepository.kt` (`onCarReady`, `readAll`)

### REQ-CAR-005: HVAC change subscription
**User story:** As a driver, I want the climate screen to follow changes made from other controls, so that physical buttons and the app agree.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL register `CarPropertyEventCallback` at `SENSOR_RATE_ONCHANGE` for each HVAC property that has discovered areas.
2. IF a registration throws THEN THE SYSTEM SHALL log a warning and continue with the remaining properties.
3. WHEN a change event arrives THE SYSTEM SHALL post it to the main thread and apply it only if the connection is live and the area matches the area tracked for that control.
4. WHEN a temperature change event arrives THE SYSTEM SHALL apply the value without clamping; WHEN fan-speed or heat events arrive THE SYSTEM SHALL clamp to [0, max].
5. WHEN `onErrorEvent` is received THE SYSTEM SHALL log "HVAC property error propId=… area=…" and change no state.
**Evidence:** `…/data/car/CarClimateRepository.kt` (`registerCallbacks`, `callback`, `applyPropertyValue`)

### REQ-CAR-006: HVAC write error handling
**User story:** As the system, I want failed writes not to crash the app, so that one rejected property does not break the screen.
**Status:** Implemented
**Acceptance criteria:**
1. WHEN a `setFloatProperty`, `setIntProperty`, or `setBooleanProperty` call throws THE SYSTEM SHALL log a warning with property ID, area, and value, and SHALL not change UI state.
2. IF the property manager is null or the target area was not discovered THEN THE SYSTEM SHALL skip the write.
3. WHILE live THE SYSTEM SHALL update the UI for written values only when the VHAL change event arrives. The one exception is airflow selection, which also updates `airflowMode` locally at once.
**Evidence:** `…/data/car/CarClimateRepository.kt` (`setFloat`, `setInt`, `setBoolean`, all `set…` functions), `…/climate/ClimateViewModel.kt` (`onEvent`)

### REQ-CAR-007: Vehicle property discovery and subscription
**User story:** As the system, I want to read only vehicle properties the car publishes, so that unsupported values are hidden instead of faked.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL request value properties `EV_BATTERY_LEVEL`, `INFO_EV_BATTERY_CAPACITY`, `RANGE_REMAINING`, `PERF_ODOMETER`, `EV_BATTERY_INSTANTANEOUS_CHARGE_RATE`, `EV_CHARGE_PORT_CONNECTED`, `FUEL_LEVEL`, `INFO_FUEL_CAPACITY`, `PERF_VEHICLE_SPEED`, `GEAR_SELECTION`, `PARKING_BRAKE_ON`, `ENV_OUTSIDE_TEMPERATURE`, `TIRE_PRESSURE`, `CRITICALLY_LOW_TIRE_PRESSURE`, and unit properties `DISTANCE_DISPLAY_UNITS`, `VEHICLE_SPEED_DISPLAY_UNITS`, `TIRE_PRESSURE_DISPLAY_UNITS`, `HVAC_TEMPERATURE_DISPLAY_UNITS`.
2. THE SYSTEM SHALL treat a property as present when it is in `propertyList` (or the list cannot be read) and `getCarPropertyConfig` returns non-null.
3. IF no value property is present THEN THE SYSTEM SHALL log "No standard vehicle properties accessible" and publish `connected = false`.
4. THE SYSTEM SHALL subscribe at `SENSOR_RATE_UI` for continuous properties and `SENSOR_RATE_ONCHANGE` for others.
5. THE SYSTEM SHALL read tire properties for every area ID, and other properties for the first area ID (or 0).
6. WHEN a value event has a status other than `CarPropertyValue.STATUS_AVAILABLE` THE SYSTEM SHALL ignore it.
7. WHEN an applied event changes a tracked value THE SYSTEM SHALL publish a new `VehicleSignals` snapshot on the main thread.
**Evidence:** `…/data/car/CarVehicleRepository.kt` (`VALUE_PROPERTIES`, `UNIT_PROPERTIES`, `WANTED`, `accessibleProperties`, `register`, `readAll`, `isAvailable`, `apply`, `publish`)

---

## 5. Non-functional requirements (`REQ-NFR-`)

### REQ-NFR-001: Platform and SDK levels
**User story:** As an OEM integrator, I want explicit platform targets, so that I know which AAOS builds can run the app.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL build with `compileSdk = release(37)`, `minSdk = 34`, `targetSdk = 36`, `applicationId = "com.test.design"`, `versionCode = 1`, `versionName = "1.0"`.
2. THE SYSTEM SHALL require the feature `android.hardware.type.automotive` (`required="true"`).
3. THE SYSTEM SHALL compile against `app/libs/android.car.jar` as `compileOnly` and declare `uses-library android.car` with `required="false"`.
**Evidence:** `app/build.gradle.kts` (`android`, `defaultConfig`, `dependencies`), `app/src/main/AndroidManifest.xml`

### REQ-NFR-002: Language and UI toolkit versions
**User story:** As a maintainer, I want pinned toolchain versions, so that builds are reproducible.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL use AGP `9.2.1`, Kotlin Compose plugin `2.2.10`, Compose BOM `2026.05.01`, Material 3 `1.5.0-alpha24`, graphics-shapes `1.1.0`, lifecycle `2.8.7`, activity-compose `1.8.0`, core-ktx `1.10.1`.
2. THE SYSTEM SHALL compile Java sources at source and target compatibility `JavaVersion.VERSION_11`.
3. THE SYSTEM SHALL build release variants with `isMinifyEnabled = false` and optimization disabled.
**Evidence:** `gradle/libs.versions.toml`, `app/build.gradle.kts`

### REQ-NFR-003: Car permissions and privileged install
**User story:** As an OEM integrator, I want the required car permissions declared and allowlisted, so that live HVAC and vehicle data are accessible.
**Status:** Implemented (criterion 3 is Inferred from `scripts/install-climate-privapp.ps1` and `README.md`, not from app code)
**Acceptance criteria:**
1. THE SYSTEM SHALL declare `android.permission.VIBRATE` and the car permissions `CONTROL_CAR_CLIMATE`, `CONTROL_CAR_DISPLAY_UNITS`, `READ_CAR_DISPLAY_UNITS`, `CAR_ENERGY`, `CAR_INFO`, `CAR_MILEAGE`, `CAR_TIRES`, `CAR_SPEED`, `CAR_POWERTRAIN`, `CAR_EXTERIOR_ENVIRONMENT` (all `android.car.permission.*`).
2. THE SYSTEM SHALL ship a `privapp-permissions` allowlist for `com.test.design` listing the same ten car permissions.
3. WHERE live data is required THE SYSTEM SHALL be platform-signed and installed at `/system/priv-app/Design/Design.apk` with the allowlist at `/system/etc/permissions/privapp-permissions-com.test.design.xml` on a rooted userdebug device.
4. IF permissions are missing THEN THE SYSTEM SHALL fall back to simulated mode without requesting permissions at runtime.
**Evidence:** `app/src/main/AndroidManifest.xml` (`uses-permission`), `scripts/privapp-permissions-com.test.design.xml`, `scripts/install-climate-privapp.ps1`, `…/data/car/CarClimateRepository.kt` (`onCarReady` "permission or VHAL" log)

### REQ-NFR-004: Touch targets and sizing tokens
**User story:** As a driver, I want large touch targets, so that I can hit controls while the car moves.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL define `CarDesignTokens.MinTouchTarget = 76.dp`, `TouchTargetSpacing = 24.dp`, `ContentPadding = 32.dp`, `PrimaryIcon = 44.dp`, `SecondaryIcon = 36.dp`, `TertiaryIcon = 24.dp`.
2. THE SYSTEM SHALL apply `carTouchTarget()` (minimum 76 × 76 dp) to the unit chip, temperature +/- buttons, airflow segments, drive-mode segments, and the energy gauge.
3. THE SYSTEM SHALL size seat-heat buttons at `MinTouchTarget` (76 dp).
4. THE SYSTEM SHALL size comfort toggles and level buttons at 52 dp, fan +/- buttons at 44 dp, and the airflow bar at 88 dp tall (`MinTouchTarget + 12.dp`).
**Evidence:** `…/design/theme/CarDesignTokens.kt`, `…/design/theme/CarModifiers.kt` (`carTouchTarget`), `…/climate/components/ClimateComfortControls.kt` (`ComfortButtonSize = 52.dp`, `SeatHeatButtonSize`), `…/climate/components/ClimateFanSpeedCard.kt` (`FanStepButton` size 44 dp), `…/climate/components/MorphingAirflowSegmentedButton.kt`

### REQ-NFR-005: Accessibility semantics
**User story:** As a user of a screen reader or voice access, I want controls to announce their purpose and state, so that I can operate them without sight.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL give comfort toggles role `Switch` and content description "<name> on|off" (e.g. "Front defrost on").
2. THE SYSTEM SHALL give level buttons role `Button` and content description "<name>, level N|off", and seat-heat buttons "<seat> seat heat, <Off|Low|Med|High|N>".
3. THE SYSTEM SHALL label temperature buttons "Increase temperature" / "Decrease temperature", fan buttons "Increase fan speed" / "Decrease fan speed", and the fan track "Fan off" or "Fan speed N of M".
4. THE SYSTEM SHALL label airflow segment icons with the mode label, the gear bar "Gear <X>", and images "SUV side profile" and "Vehicle top view".
**Evidence:** `…/climate/components/ClimateComfortControls.kt`, `…/climate/components/ClimateTemperatureSection.kt`, `…/climate/components/ClimateFanSpeedCard.kt`, `…/climate/components/MorphingAirflowSegmentedButton.kt`, `…/vehicle/components/VehicleInfoCard.kt`, `…/vehicle/components/TireWireframeGraphic.kt`

### REQ-NFR-006: Driver distraction
**User story:** As an OEM, I want the app allowed while driving with reduced motion, so that it meets driver-distraction rules.
**Status:** Inferred (manifest metadata and motion fallback code exist, but nothing in the app sets `DrivingUxState` to `Driving` or `Restricted`)
**Acceptance criteria:**
1. THE SYSTEM SHALL declare `MainActivity` with meta-data `distractionOptimized = true`.
2. WHILE `DrivingUxState` is `Driving` or `Restricted` THE SYSTEM SHALL use `AppMotionScheme.Standard`; WHILE `Parked` it SHALL use the selected scheme (default `Expressive`).
3. THE SYSTEM SHALL default `DrivingUxState` to `Parked` and persist it in `SavedStateHandle` under key `driving_ux_state`.
**Evidence:** `app/src/main/AndroidManifest.xml` (`distractionOptimized`), `…/design/core/motion/AppMotionScheme.kt` (`resolveMotionScheme`), `…/design/core/driving/DrivingUxViewModel.kt`, `…/design/presentation/DesignAppShell.kt` (`LocalDrivingUxUpdater` provided, never consumed)

### REQ-NFR-007: Theming
**User story:** As a driver, I want the app to follow the system light/dark setting, so that it matches day and night modes.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL support `AppThemeMode` values `System`, `Light`, `Dark`, defaulting to `System`.
2. WHILE `System` THE SYSTEM SHALL follow `isSystemInDarkTheme()`; `Light` and `Dark` SHALL override it.
3. THE SYSTEM SHALL persist the selected mode in SharedPreferences `design_theme`, key `app_theme_mode`, and react to external changes of that key.
4. THE SYSTEM SHALL use `CarTypography` and the app light/dark colour schemes in `AppTheme`, and update system-bar styles to match.
5. THE SYSTEM SHALL render vehicle cockpit surfaces dark regardless of app theme (`vehicleColorScheme`).
**Evidence:** `…/design/core/theme/AppThemeMode.kt`, `…/design/core/theme/ThemeModeViewModel.kt`, `…/design/theme/AppTheme.kt`, `…/design/theme/VehicleDynamicColor.kt`, `test/…/core/theme/AppThemeModeTest.kt` (all three tests)

### REQ-NFR-008: Motion
**User story:** As a designer, I want animations to use Material 3 motion tokens, so that motion is consistent and swappable.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL default the motion scheme to `AppMotionScheme.Expressive`, stored in `SavedStateHandle` key `app_motion_scheme`.
2. THE SYSTEM SHALL use `slowSpatialSpec` for segmented-indicator slides and battery arc fill, `defaultSpatialSpec` for counters, thumbs, and range ring, and `defaultEffectsSpec` for colour changes.
**Evidence:** `…/design/core/motion/MotionSchemeViewModel.kt`, `…/design/theme/AppTheme.kt`, component files listed in REQ-CLIM-010, REQ-CLIM-005, REQ-VEH-002

### REQ-NFR-009: Window rendering
**User story:** As a driver, I want the app to render correctly on the AAOS emulator, so that the demo does not show a black screen.
**Status:** Implemented
**Acceptance criteria:**
1. WHEN `MainActivity` is created THE SYSTEM SHALL set the window format to `PixelFormat.OPAQUE` and the background to `R.color.canvas_background`.
2. THE SYSTEM SHALL draw edge-to-edge with transparent status and navigation bars.
3. THE SYSTEM SHALL remove `androidx.startup.InitializationProvider` from the merged manifest.
**Evidence:** `…/design/MainActivity.kt` (`onCreate`), `app/src/main/AndroidManifest.xml` (`provider tools:node="remove"`)

### REQ-NFR-010: Property update threading and rate
**User story:** As the system, I want property updates handled on the main thread at UI rate, so that state stays consistent without flooding recomposition.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL post every property change event to the main looper before applying it.
2. THE SYSTEM SHALL subscribe continuous vehicle properties at `SENSOR_RATE_UI` and all HVAC and on-change properties at `SENSOR_RATE_ONCHANGE`.
3. THE SYSTEM SHALL collect UI state with `collectAsStateWithLifecycle`.
**Evidence:** `…/data/car/CarClimateRepository.kt` (`callback`, `registerCallbacks`), `…/data/car/CarVehicleRepository.kt` (`callback`, `register`), `…/design/MainActivity.kt`

### REQ-NFR-011: Build and test gate
**User story:** As a maintainer, I want one command to build and test, so that CI and local checks agree.
**Status:** Implemented
**Acceptance criteria:**
1. THE SYSTEM SHALL build and unit-test with `./gradlew assembleDebug testDebugUnitTest` using JDK 17.
2. WHEN code is pushed to `main`, a pull request is opened, or the workflow is dispatched THE SYSTEM SHALL run that command in GitHub Actions and upload `app/build/outputs/apk/debug/app-debug.apk` as artifact `app-debug-apk`.
**Evidence:** `.github/workflows/build-apk.yml`, `README.md`, `AGENTS.md`

---

## 6. Constraints and dependencies

- **Build tooling**: Gradle wrapper, AGP 9.2.1, JDK 17 (CI and `AGENTS.md`), SDK platform `android-37.0` and build-tools `37.0.0` (`AGENTS.md`). Single module `:app` (`settings.gradle.kts`).
- **Runtime libraries**: Jetpack Compose (BOM 2026.05.01), Material 3 1.5.0-alpha24 (alpha), material-icons-extended, androidx.graphics.shapes, lifecycle-viewmodel-compose, lifecycle-runtime-compose.
- **Platform library**: `android.car` must be on the device boot classpath or registered as a shared library.
- **Signing and install**: Live data needs platform signing with AOSP keys (`platform.pk8`, `platform.x509.pem` in `.tmp-aosp-keys`), `adb root`, `adb remount`, and a reboot (`scripts/install-climate-privapp.ps1`).
- **Emulator**: `AGENTS.md` lists AVDs `cloud_avd` (API 34) and `android17_avd` (API 37). The API 37 image needs KVM and AVX.
- **Known lint state**: `./gradlew lintDebug` fails on pre-existing errors (e.g. `MissingSuperCall`), per `AGENTS.md`.
- **Tests**: JUnit 4.13.2 unit tests only. No `app/src/androidTest` directory exists, although Compose UI test and Espresso dependencies are declared.

---

## 7. Traceability

| Requirement ID | Title | Status | Source file(s) | Symbols | Tests |
|---|---|---|---|---|---|
| REQ-CLIM-001 | Dual-zone temperature display | Implemented | `ClimateControlScreen.kt`, `ClimateContract.kt`, `theme/WindowLayout.kt` | `DualZoneTemperatureBand`, `ClimateUiState`, `useSideBySide` | `WindowLayoutInfoTest` |
| REQ-CLIM-002 | Step temperature with +/- | Implemented | `ClimateTemperatureSection.kt`, `ClimateContract.kt`, `ClimateViewModel.kt` | `stepTemperature`, `snapTemperature`, `adjustZoneTemperature` | — |
| REQ-CLIM-003 | Drag the temperature scale | Implemented | `TemperatureScale.kt`, `ClimateContract.kt`, `TemperatureDialMetrics.kt` | `TemperatureScale`, `temperatureScaleDragSteps`, `temperatureScaleLabelFits` | `TemperatureScaleTest` |
| REQ-CLIM-004 | Temperature range and step limits | Implemented | `ClimateContract.kt`, `ClimateViewModel.kt`, `CarClimateRepository.kt` | `ClimateUiState`, `applyConnection`, `snapToStep` | — |
| REQ-CLIM-005 | Temperature display format | Implemented | `ClimateContract.kt`, `AnimatedTemperatureCounter.kt` | `formatTemperature`, `celsiusToFahrenheitDisplay` | — |
| REQ-CLIM-006 | Toggle temperature unit | Implemented | `ClimateControlScreen.kt`, `ClimateViewModel.kt`, `CarClimateRepository.kt` | `ToggleTemperatureUnit`, `setTemperatureUnit` | — |
| REQ-CLIM-007 | Sync zones | Implemented | `ClimateViewModel.kt`, `CarClimateRepository.kt`, `ClimateComfortControls.kt` | `ToggleSync`, `setSyncEnabled` | — |
| REQ-CLIM-008 | Fan speed control | Implemented | `ClimateFanSpeedCard.kt`, `ClimateContract.kt`, `ClimateViewModel.kt` | `FanSpeedControl`, `fanSpeedFromTrackPosition` | `FanSpeedTrackTest` |
| REQ-CLIM-009 | Fan speed write to VHAL | Implemented | `CarClimateRepository.kt` | `setFanSpeed`, `ensurePowerOn` | — |
| REQ-CLIM-010 | Airflow direction | Implemented | `MorphingAirflowSegmentedButton.kt`, `HvacConstants.kt`, `CarClimateRepository.kt` | `HvacFanDirection`, `setAirflowMode`, `directionToAirflow` | — |
| REQ-CLIM-011 | Auto climate | Implemented | `ClimateControlScreen.kt`, `CarClimateRepository.kt` | `onToggleAuto`, `setAirflowMode` | — |
| REQ-CLIM-012 | A/C toggle | Implemented | `ClimateControlScreen.kt`, `CarClimateRepository.kt`, `theme/MorphingRoundedShape.kt` | `ToggleAc`, `setAcEnabled`, `rememberClimateDialShape` | — |
| REQ-CLIM-013 | HVAC power | Implemented | `ClimateComfortControls.kt`, `CarClimateRepository.kt` | `TogglePower`, `setPower`, `ensurePowerOn` | — |
| REQ-CLIM-014 | Recirculation and fresh air | Implemented | `ClimateComfortControls.kt`, `CarClimateRepository.kt` | `ToggleRecirculation`, `setRecirculation` | — |
| REQ-CLIM-015 | Front and rear defrost | Implemented | `ClimateComfortControls.kt`, `CarClimateRepository.kt`, `HvacConstants.kt` | `setFrontDefrost`, `setRearDefrost`, `HvacWindowArea` | — |
| REQ-CLIM-016 | Seat heat | Implemented | `ClimateComfortControls.kt`, `ClimateViewModel.kt`, `CarClimateRepository.kt` | `CycleSeatHeat`, `nextLevel`, `setSeatHeatLevel` | — |
| REQ-CLIM-017 | Seat-heat presentation | Implemented | `SeatHeatAppearance.kt`, `ClimateComfortControls.kt` | `seatHeatStateLabel`, `seatHeatColors` | `SeatHeatAppearanceTest` |
| REQ-CLIM-018 | Steering-wheel heat | Implemented | `ClimateComfortControls.kt`, `CarClimateRepository.kt` | `CycleSteeringHeat`, `setSteeringHeatLevel` | — |
| REQ-CLIM-019 | Seat ventilation | Implemented | `ClimateComfortControls.kt`, `CarClimateRepository.kt` | `CycleSeatVent`, `setSeatVentLevel` | — |
| REQ-CLIM-020 | Capability-driven visibility | Implemented | `ClimateContract.kt`, `CarClimateRepository.kt`, `ClimateControlScreen.kt` | `ClimateCapabilities`, `toCapabilities` | — |
| REQ-CLIM-021 | Simulated climate mode | Implemented | `ClimateViewModel.kt` | `onEvent`, `applyConnection` | — |
| REQ-CLIM-022 | Control feedback | Implemented | `ClimateHaptics.kt` | `tick`, `toggle`, `confirm`, `buildClickTrack` | — |
| REQ-CLIM-023 | Temperature colour and cool effect | Implemented | `theme/DynamicClimateColor.kt`, `CoolSnowflakeOverlay.kt` | `climateColorScheme`, `zoneCoolIntensity` | `DynamicClimateColorTest` |
| REQ-CLIM-024 | Temperature dial sizing | Implemented | `TemperatureDialMetrics.kt` | `temperatureDialMetrics` | `TemperatureScaleTest.dialFitsTheZoneFrom220To320Dpi` |
| REQ-VEH-001 | Energy level | Implemented | `data/car/VehicleSignals.kt`, `VehicleLiveState.kt` | `batteryPercent`, `fuelPercent`, `withLiveSignals` | `VehicleSignalsTest`, `VehicleLiveStateTest` |
| REQ-VEH-002 | Energy gauge | Implemented | `VehicleEnergyCockpit.kt`, `TripMeter.kt` | `GaugeWithStats`, `RangeCapacityMiles` | — |
| REQ-VEH-003 | Range summary | Implemented | `VehicleInfoReadout.kt`, `VehicleEnergyCockpit.kt` | `toInfoReadout`, `estimatedPackKwh`, `averageWattHours` | `VehicleInfoReadoutTest` |
| REQ-VEH-004 | Charging detection | Implemented | `data/car/VehicleSignals.kt` | `chargeRateKilowatts`, `toDisplay` | `VehicleSignalsTest` |
| REQ-VEH-005 | Tire pressure display | Implemented | `TireWireframe.kt`, `TireWireframeGraphic.kt`, `VehicleLivePanel.kt` | `toTireWireframe`, `tireStatus`, `ReferenceWireframe` | `TireWireframeTest` |
| REQ-VEH-006 | Critically-low tire flag | Implemented | `CarVehicleRepository.kt`, `data/car/VehicleSignals.kt` | `isTireCritical`, `wheelOrder` | `VehicleSignalsTest` |
| REQ-VEH-007 | Gear indicator | Implemented | `CarVehicleRepository.kt`, `VehicleInfoReadout.kt`, `VehicleInfoCard.kt` | `gearKind`, `VehicleGearOrder`, `GearBar` | `VehicleInfoReadoutTest` |
| REQ-VEH-008 | Drive-mode selector | Implemented | `VehicleContract.kt`, `VehicleViewModel.kt`, `MorphingDriveModeSelector.kt`, `VehicleDriveModeLayout.kt` | `DriveMode`, `SelectDriveMode`, `defaultRegenLevel` | `VehicleLiveStateTest` |
| REQ-VEH-009 | Demo battery cycling | Implemented | `VehicleScreen.kt`, `VehicleViewModel.kt` | `CycleBatteryDemo`, `isDemoOnly` | — |
| REQ-VEH-010 | Live and demo transition | Implemented | `VehicleLiveState.kt`, `VehicleContract.kt` | `withLiveSignals` | `VehicleLiveStateTest` |
| REQ-VEH-011 | Display unit conversion | Implemented | `CarVehicleRepository.kt`, `data/car/VehicleSignals.kt` | `publish`, `formatSpeed`, `formatPressure` | `VehicleSignalsTest` |
| REQ-VEH-012 | Vehicle dynamic colour | Implemented | `theme/VehicleDynamicColor.kt` | `vehicleColorScheme` | — |
| REQ-VEH-013 | Vehicle screen layout | Implemented | `VehicleScreen.kt` | `VehicleScreen` | — |
| REQ-VEH-014 | Secondary signal labels | Inferred | `data/car/VehicleSignals.kt`, `VehicleLiveState.kt` | `toDisplay`, `withLiveSignals` | `VehicleSignalsTest` |
| REQ-NAV-001 | Navigation rail | Implemented | `MainActivity.kt`, `DashboardWidget.kt` | `NavigationRail`, `DashboardWidget` | — |
| REQ-NAV-002 | Default screen | Implemented | `MainActivity.kt` | `selectedScreen` | — |
| REQ-NAV-003 | Intent-based launch | Implemented | `MainActivity.kt`, `AndroidManifest.xml` | `ACTION_OPEN_CLIMATE`, `ACTION_OPEN_VEHICLE`, `applyIntent` | — |
| REQ-NAV-004 | Launcher and home entry | Implemented | `AndroidManifest.xml` | `intent-filter` | — |
| REQ-NAV-005 | Configuration changes | Implemented | `AndroidManifest.xml`, `MainActivity.kt` | `configChanges`, `viewModels()` | — |
| REQ-CAR-001 | Car connection lifecycle | Implemented | `CarClimateRepository.kt`, `CarVehicleRepository.kt` | `start`, `stop`, `onCarReady`, `onCarDisconnected` | — |
| REQ-CAR-002 | HVAC area discovery | Implemented | `CarClimateRepository.kt` | `discoverAreas`, `cabinSeatAreas`, `preferredSeat` | — |
| REQ-CAR-003 | HVAC range discovery | Implemented | `CarClimateRepository.kt` | `temperatureConfig`, `floatRange`, `intRange` | — |
| REQ-CAR-004 | HVAC initial snapshot | Implemented | `CarClimateRepository.kt` | `readAll` | — |
| REQ-CAR-005 | HVAC change subscription | Implemented | `CarClimateRepository.kt` | `registerCallbacks`, `applyPropertyValue` | — |
| REQ-CAR-006 | HVAC write error handling | Implemented | `CarClimateRepository.kt`, `ClimateViewModel.kt` | `setFloat`, `setInt`, `setBoolean` | — |
| REQ-CAR-007 | Vehicle property discovery | Implemented | `CarVehicleRepository.kt` | `WANTED`, `accessibleProperties`, `register`, `isAvailable` | — |
| REQ-NFR-001 | Platform and SDK levels | Implemented | `app/build.gradle.kts`, `AndroidManifest.xml` | `minSdk`, `targetSdk`, `compileSdk` | — |
| REQ-NFR-002 | Toolchain versions | Implemented | `gradle/libs.versions.toml`, `app/build.gradle.kts` | `[versions]` | — |
| REQ-NFR-003 | Car permissions and privapp | Implemented (crit. 3 Inferred) | `AndroidManifest.xml`, `scripts/privapp-permissions-com.test.design.xml`, `scripts/install-climate-privapp.ps1` | `uses-permission`, `privapp-permissions` | — |
| REQ-NFR-004 | Touch targets | Implemented | `theme/CarDesignTokens.kt`, `theme/CarModifiers.kt` | `MinTouchTarget`, `carTouchTarget` | — |
| REQ-NFR-005 | Accessibility semantics | Implemented | climate and vehicle component files | `semantics`, `contentDescription` | — |
| REQ-NFR-006 | Driver distraction | Inferred | `AndroidManifest.xml`, `core/motion/AppMotionScheme.kt`, `core/driving/DrivingUxViewModel.kt` | `distractionOptimized`, `resolveMotionScheme` | — |
| REQ-NFR-007 | Theming | Implemented | `core/theme/*`, `theme/AppTheme.kt` | `AppThemeMode`, `ThemeModeViewModel`, `AppTheme` | `AppThemeModeTest` |
| REQ-NFR-008 | Motion | Implemented | `core/motion/MotionSchemeViewModel.kt`, `theme/AppTheme.kt` | `AppMotionScheme.Expressive` | — |
| REQ-NFR-009 | Window rendering | Implemented | `MainActivity.kt`, `AndroidManifest.xml` | `PixelFormat.OPAQUE`, `enableEdgeToEdge` | — |
| REQ-NFR-010 | Update threading and rate | Implemented | `CarClimateRepository.kt`, `CarVehicleRepository.kt` | `mainHandler.post`, `SENSOR_RATE_UI` | — |
| REQ-NFR-011 | Build and test gate | Implemented | `.github/workflows/build-apk.yml` | `assembleDebug testDebugUnitTest` | — |

Tests not tied to a reachable requirement: `TripMeterTest` (covers `TripMeter.kt` helpers used by the unreachable `TripMeterGraphics.kt`, except `RangeCapacityMiles` and `rangeBand`), `TireChassisLayoutTest` (covers `chassisMapMetrics`, which no screen calls), and `VehicleLiveStateTest.tireFillTracksPressureUpToVisualFull` (`tireFillFraction` is not called by any composable).

---

## 8. Open questions and gaps

- **Q-001 (fake data while live):** `toTireWireframe` shows the hard-coded `ReferenceWireframe` (36/39/35/37 psi, one tire marked Critical, made-up tire temperatures and "sec ago" times) whenever any of the four wheel readings is missing, including in live mode. Live users could read invented tire data as real. Should missing live tires show "—" instead?
- **Q-002 (no simulated indicator):** `SimulatedBadge` exists ("so customer demos never oversell live VHAL / Car APIs") but is never shown. Neither screen tells the user whether values are live or simulated. Is a visible badge required?
- **Q-003 (critically-low flag ignored):** The repository reads `CRITICALLY_LOW_TIRE_PRESSURE`, but `VehicleSignals.toDisplay` replaces it with `kPa < 180` (`isTireCritical`). A VHAL "critically low" signal above 180 kPa is dropped. Which source should win?
- **Q-004 (driving restrictions not wired):** `distractionOptimized=true` is declared, but no code reads `CarUxRestrictionsManager` or calls `LocalDrivingUxUpdater`. `DrivingUxState` stays `Parked`, so the Standard-motion fallback never runs. No control is disabled while driving. Which restrictions are required?
- **Q-005 (trip data missing):** `README.md` lists "trip" for the Vehicle screen. The odometer drum and range road graphics (`OdometerGraphic`, `RangeGraphic`, `VehicleLiveSummary`) are not reachable. Speed, odometer, parking-brake, and charge-port labels are computed but not shown. Should they be displayed?
- **Q-006 (drive mode does not reach the vehicle):** Drive-mode selection changes only colours and animation. No VHAL property is written. Is this a display-only preference or should it control a vehicle mode?
- **Q-007 (unit toggle may revert):** In live mode without `hasTemperatureUnit`, the unit chip changes `temperatureUnit` locally, but the next `applyConnection` overwrites it with `connection.temperatureUnit` (Celsius by default). Should the local choice persist?
- **Q-008 (unit write area):** `setTemperatureUnit` writes `HVAC_TEMPERATURE_DISPLAY_UNITS` to area `0`, not the discovered `tempDisplayUnits` area. Is area 0 always correct?
- **Q-009 (stale climate values when disconnected):** When the HVAC connection drops, `applyConnection` resets capabilities and steps but keeps the last live temperatures, fan, and toggles. The UI then edits them as simulated values without telling the user.
- **Q-010 (airflow optimistic update):** `SelectAirflow` updates `airflowMode` before the VHAL confirms; all other live controls wait for the change event. If the write fails, the UI shows a mode the car did not accept.
- **Q-011 (errors are silent):** `onErrorEvent` and failed `set…` calls are only logged. No UI feedback exists for rejected writes or unavailable properties.
- **Q-012 (Auto-only vehicles):** `hasAirflowControls` is true when only `hasAuto` is true, but `availableAirflowModes` is then empty, so the blower card shows an empty airflow area (the segmented button returns early). Expected behaviour is undefined.
- **Q-013 (Fresh air tap does nothing):** Tapping "Fresh" while fresh air is already active is ignored. Confirm this is intended rather than a toggle.
- **Q-014 (touch targets below token):** Comfort buttons (52 dp) and fan +/- buttons (44 dp) are smaller than `MinTouchTarget = 76.dp`, which the token file attributes to AAOS sizing guidelines.
- **Q-015 (accessibility gaps):** Temperature buttons do not name the zone ("Increase temperature" for both). The A/C `Switch` has no own label (the "A/C" text is a sibling). Navigation rail icons have `contentDescription = null` (text labels are present).
- **Q-016 (state not restored after process death):** The selected screen is a plain `mutableStateOf` in the activity and climate `activeZone` is not saved. After process death the app returns to Climate.
- **Q-017 (HOME category):** `MainActivity` declares `android.intent.category.HOME`, which lets it act as the device home app. Confirm this is intended for production builds.
- **Q-018 (fan off semantics):** With the default `minFan = 1`, selecting fan 0 turns HVAC power off instead of writing 0. The UI then shows "Off" but power is also off, which changes other controls' state.
- **Q-019 (seat-heat defaults differ):** Simulated driver seat heat starts at level 1 (`ClimateUiState`), while the repository fallback is 0. Confirm which default is intended.
- **Q-020 (docs disagree with code):** `AGENTS.md` describes `component` and `template` modules and repo `itsabhinnav/demo` / branch `main`; `settings.gradle.kts` includes only `:app`. `docs/motion/vehicle.md` describes Motion Studio, drive-mode layout reweighting, a systems panel, and a trip stats card; none is reachable (`VehicleMotionStudio.kt` does not exist, and `layoutProfile()` is unused). `docs/motion/climate.md` describes zone selector cards and fan "bars"; the zone selector is unused and the fan control is a track.
- **Q-021 (no UI tests):** UI behaviour (control visibility, intents, live/simulated switching) has no automated tests. `app/src/androidTest` does not exist, and no test covers `ClimateViewModel`, `VehicleViewModel`, or either repository.
- **Q-022 (range fraction uses miles):** The range ring always uses `rangeMiles / 300`, even when the display unit is kilometres. Confirm that the 300-mile capacity applies to all markets.
