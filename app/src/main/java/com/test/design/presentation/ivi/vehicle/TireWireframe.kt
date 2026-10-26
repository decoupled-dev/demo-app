package com.test.design.presentation.ivi.vehicle

import kotlin.math.round

internal enum class TireStatus { Normal, Warning, Critical }

internal data class TireCornerDisplay(
    val title: String,
    val psi: Int?,
    val temperature: String,
    val status: TireStatus,
    val updated: String,
)

internal data class TireWireframe(
    val frontLeft: TireCornerDisplay,
    val frontRight: TireCornerDisplay,
    val rearLeft: TireCornerDisplay,
    val rearRight: TireCornerDisplay,
)

/** Vertical span of each axle's tires in the top-down wireframe drawable, as a fraction of its height. */
internal val FrontAxleSpan = 0.145f..0.274f
internal val RearAxleSpan = 0.736f..0.867f

/** Aspect ratio (width / height) of the top-down wireframe drawable. */
internal const val TopWireframeAspect = 452f / 1053f

private val ReferenceWireframe = TireWireframe(
    frontLeft = TireCornerDisplay("Front\nLeft", 36, "45 °C", TireStatus.Normal, "28 sec ago"),
    frontRight = TireCornerDisplay("Front\nRight", 39, "44 °C", TireStatus.Critical, "19 sec ago"),
    rearLeft = TireCornerDisplay("Rear\nLeft", 35, "38 °C", TireStatus.Normal, "10 min ago"),
    rearRight = TireCornerDisplay("Rear\nRight", 37, "40 °C", TireStatus.Warning, "3 sec ago"),
)

internal fun VehicleUiState.toTireWireframe(): TireWireframe {
    val byId = tires.associateBy { it.id }
    val frontLeft = byId["fl"]
    val frontRight = byId["fr"]
    val rearLeft = byId["rl"]
    val rearRight = byId["rr"]
    if (frontLeft == null || frontRight == null || rearLeft == null || rearRight == null) {
        return ReferenceWireframe
    }
    val temperature = outsideTempLabel?.takeIf { it.isNotBlank() }?.spaceUnit() ?: "—"
    return TireWireframe(
        frontLeft = frontLeft.toCorner("Front\nLeft", temperature),
        frontRight = frontRight.toCorner("Front\nRight", temperature),
        rearLeft = rearLeft.toCorner("Rear\nLeft", temperature),
        rearRight = rearRight.toCorner("Rear\nRight", temperature),
    )
}

internal fun kiloPascalsToPsi(kiloPascals: Float): Int =
    round(kiloPascals / 6.894757f).toInt()

/** Placard ~35 psi: warn from 25% under or 20% over, critical from 35% under. */
internal fun tireStatus(psi: Int, criticallyLow: Boolean): TireStatus = when {
    criticallyLow || psi < 23 -> TireStatus.Critical
    psi < 27 || psi > 42 -> TireStatus.Warning
    else -> TireStatus.Normal
}

private fun TireReading.toCorner(title: String, temperature: String): TireCornerDisplay {
    val psi = kiloPascalsToPsi(kiloPascals)
    return TireCornerDisplay(
        title = title,
        psi = psi,
        temperature = temperature,
        status = tireStatus(psi, criticallyLow),
        updated = "Just now",
    )
}

/** "21°C" -> "21 °C" to match the readout typography. */
private fun String.spaceUnit(): String {
    val degree = indexOf('°')
    if (degree <= 0 || this[degree - 1] == ' ') return this
    return substring(0, degree) + " " + substring(degree)
}
