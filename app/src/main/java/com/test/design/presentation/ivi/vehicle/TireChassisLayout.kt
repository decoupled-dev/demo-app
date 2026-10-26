package com.test.design.presentation.ivi.vehicle

/**
 * Places the car, tires, and pressure labels for the live chassis map.
 *
 * On a fixed-pixel panel, raising density from 220 dpi to 320 dpi shrinks the
 * window in dp. Slots are derived from the measured width and height so the
 * labels stay beside the tires instead of covering them.
 */
internal data class PlacedRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

internal data class ChassisMapMetrics(
    val body: PlacedRect,
    val frontLeftTire: PlacedRect,
    val frontRightTire: PlacedRect,
    val rearLeftTire: PlacedRect,
    val rearRightTire: PlacedRect,
    val frontLeftLabel: PlacedRect,
    val frontRightLabel: PlacedRect,
    val rearLeftLabel: PlacedRect,
    val rearRightLabel: PlacedRect,
    val labelFontSp: Float,
    val valueFontSp: Float,
) {
    val tires: List<PlacedRect>
        get() = listOf(frontLeftTire, frontRightTire, rearLeftTire, rearRightTire)

    val labels: List<PlacedRect>
        get() = listOf(frontLeftLabel, frontRightLabel, rearLeftLabel, rearRightLabel)
}

internal fun PlacedRect.intersects(other: PlacedRect): Boolean =
    left < other.right && right > other.left && top < other.bottom && bottom > other.top

internal fun chassisMapMetrics(widthDp: Float, heightDp: Float): ChassisMapMetrics {
    val width = widthDp.takeIf { it.isFinite() && it > 0f } ?: 360f
    val height = heightDp.takeIf { it.isFinite() && it > 0f } ?: 420f

    var margin = (minOf(width, height) * 0.025f).coerceIn(2f, 8f)
    var gap = (width * 0.02f).coerceIn(4f, 12f)
    val minContent = 28f * 2f + 18f * 2f + 36f
    while (width - margin * 2f - gap * 4f < minContent && gap > 2f) gap -= 1f
    while (width - margin * 2f - gap * 4f < minContent && margin > 0f) margin -= 1f

    val available = (width - margin * 2f - gap * 4f).coerceAtLeast(minContent)
    val preferredSum = 76f * 2f + 52f * 2f + 160f
    val scale = (available / preferredSum).coerceAtMost(1f)
    val labelW = 76f * scale
    val tireW = 52f * scale
    val bodyW = available - labelW * 2f - tireW * 2f

    val innerH = (height - margin * 2f).coerceAtLeast(80f)
    val rowGap = gap.coerceAtMost(innerH * 0.08f)
    val rowH = (innerH - rowGap) / 2f
    val tireH = minOf(rowH * 0.78f, tireW * 1.85f).coerceAtLeast(minOf(tireW, rowH * 0.7f))
    val labelH = minOf(44f * scale, rowH * 0.55f).coerceAtLeast(minOf(24f, rowH * 0.4f))

    val labelLeft = margin
    val tireLeft = labelLeft + labelW + gap
    val bodyLeft = tireLeft + tireW + gap
    val tireRight = bodyLeft + bodyW + gap
    val labelRight = tireRight + tireW + gap

    val frontCenterY = margin + rowH / 2f
    val rearCenterY = margin + rowH + rowGap + rowH / 2f
    val bodyTop = margin + innerH * 0.04f
    val bodyH = innerH * 0.92f

    val valueFontSp = (labelW / 4.8f).coerceIn(10f, 18f)
    val labelFontSp = (valueFontSp * 0.72f).coerceIn(9f, 13f)

    return ChassisMapMetrics(
        body = PlacedRect(bodyLeft, bodyTop, bodyW, bodyH),
        frontLeftTire = tireSlot(tireLeft, frontCenterY, tireW, tireH),
        frontRightTire = tireSlot(tireRight, frontCenterY, tireW, tireH),
        rearLeftTire = tireSlot(tireLeft, rearCenterY, tireW, tireH),
        rearRightTire = tireSlot(tireRight, rearCenterY, tireW, tireH),
        frontLeftLabel = labelSlot(labelLeft, frontCenterY, labelW, labelH),
        frontRightLabel = labelSlot(labelRight, frontCenterY, labelW, labelH),
        rearLeftLabel = labelSlot(labelLeft, rearCenterY, labelW, labelH),
        rearRightLabel = labelSlot(labelRight, rearCenterY, labelW, labelH),
        labelFontSp = labelFontSp,
        valueFontSp = valueFontSp,
    )
}

private fun tireSlot(left: Float, centerY: Float, width: Float, height: Float) =
    PlacedRect(left, centerY - height / 2f, width, height)

private fun labelSlot(left: Float, centerY: Float, width: Float, height: Float) =
    PlacedRect(left, centerY - height / 2f, width, height)
