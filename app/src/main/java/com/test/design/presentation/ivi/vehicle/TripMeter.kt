package com.test.design.presentation.ivi.vehicle

/** Miles that fill the range graphic. Matches the energy gauge capacity. */
internal const val RangeCapacityMiles = 300

internal enum class RangeBand { Empty, Low, Mid, Good }

internal data class DistanceReadout(
    val whole: Int,
    val unit: String,
    val available: Boolean,
)

internal data class RangeRoad(
    val horizonY: Float,
    val bottom: Float,
    val nearLeft: Float,
    val nearRight: Float,
    val farLeft: Float,
    val farRight: Float,
    val fillFarY: Float,
    val fillFarLeft: Float,
    val fillFarRight: Float,
)

internal fun distanceReadout(
    label: String?,
    fallbackWhole: Int,
    fallbackUnit: String = "mi",
    missingLabelIsUnavailable: Boolean = false,
): DistanceReadout {
    if (label == "—") return DistanceReadout(0, fallbackUnit, available = false)
    if (label.isNullOrBlank()) {
        return if (missingLabelIsUnavailable) {
            DistanceReadout(0, fallbackUnit, available = false)
        } else {
            DistanceReadout(fallbackWhole.coerceAtLeast(0), fallbackUnit, available = true)
        }
    }
    val match = DistanceLabel.find(label.trim())
        ?: return DistanceReadout(fallbackWhole.coerceAtLeast(0), fallbackUnit, available = false)
    val whole = match.groupValues[1].replace(",", "").toIntOrNull() ?: fallbackWhole
    return DistanceReadout(whole.coerceAtLeast(0), match.groupValues[2], available = true)
}

internal fun odometerPlaces(whole: Int): Int = if (whole >= 1_000_000) 7 else 6

internal fun odometerDigits(whole: Int, places: Int = 6): List<Int> {
    val safePlaces = places.coerceIn(1, 8)
    var rest = whole.coerceAtLeast(0)
    val digits = IntArray(safePlaces)
    for (index in safePlaces - 1 downTo 0) {
        digits[index] = rest % 10
        rest /= 10
    }
    return digits.toList()
}

/** Leading zeros stay dim. A zero reading keeps the last drum lit. */
internal fun odometerDigitDimmed(digits: List<Int>, index: Int): Boolean {
    if (digits.isEmpty()) return false
    val firstSignificant = digits.indexOfFirst { it != 0 }.let { found ->
        if (found == -1) digits.lastIndex else found
    }
    return index < firstSignificant
}

internal fun rangeFillFraction(miles: Int, capacityMiles: Int = RangeCapacityMiles): Float {
    if (miles <= 0 || capacityMiles <= 0) return 0f
    return (miles.toFloat() / capacityMiles).coerceIn(0f, 1f)
}

internal fun rangeBand(fraction: Float): RangeBand = when {
    fraction <= 0f -> RangeBand.Empty
    fraction < 0.2f -> RangeBand.Low
    fraction < 0.5f -> RangeBand.Mid
    else -> RangeBand.Good
}

/**
 * Perspective road for the range graphic.
 * The fill grows from the near edge (the car) toward the horizon.
 */
internal fun rangeRoad(width: Float, height: Float, fraction: Float): RangeRoad {
    val safeWidth = width.coerceAtLeast(1f)
    val safeHeight = height.coerceAtLeast(1f)
    val horizonY = safeHeight * 0.28f
    val bottom = safeHeight * 0.94f
    val nearLeft = safeWidth * 0.08f
    val nearRight = safeWidth * 0.92f
    val farLeft = safeWidth * 0.40f
    val farRight = safeWidth * 0.60f
    val clamped = fraction.coerceIn(0f, 1f)
    val fillFarY = bottom - (bottom - horizonY) * clamped
    val span = (bottom - horizonY).coerceAtLeast(1f)
    val fromHorizon = ((fillFarY - horizonY) / span).coerceIn(0f, 1f)
    return RangeRoad(
        horizonY = horizonY,
        bottom = bottom,
        nearLeft = nearLeft,
        nearRight = nearRight,
        farLeft = farLeft,
        farRight = farRight,
        fillFarY = fillFarY,
        fillFarLeft = farLeft + (nearLeft - farLeft) * fromHorizon,
        fillFarRight = farRight + (nearRight - farRight) * fromHorizon,
    )
}

private val DistanceLabel = Regex("""(\d[\d,]*)\s*([A-Za-z]+)""")
