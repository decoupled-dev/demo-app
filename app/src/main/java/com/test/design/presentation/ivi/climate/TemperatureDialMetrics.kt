package com.test.design.presentation.ivi.climate

/**
 * Sizes for one HVAC temperature zone.
 *
 * On a fixed-pixel panel, raising density from 220 dpi to 320 dpi shrinks the
 * window in dp. The dial uses the zone's actual width and height instead of a
 * fixed card, and keeps degree labels inside the rounded corners.
 */
data class TemperatureDialMetrics(
    val buttonSize: Float,
    val iconSize: Float,
    val spacing: Float,
    val cardWidth: Float,
    val sidePadding: Float,
    val topPadding: Float,
    val bottomPadding: Float,
    val canvasHeight: Float,
    val readoutFontSp: Float,
    val readoutLineSp: Float,
)

private const val ButtonPreferred = 76f
private const val ButtonMin = 48f
private const val SpacingPreferred = 24f
private const val SpacingTight = 8f
private const val CardMax = 400f
private const val CardMin = 148f
private const val CanvasPreferred = 100f
private const val ReadoutLinePreferred = 52f
private const val ReadoutFontPreferred = 44f
private const val ZoneLabelBlock = 44f
private const val TopPadding = 16f
private const val BottomPadding = 12f
private const val ReadoutGap = 4f
private const val DialCornerRadius = 72f

/** Horizontal inset of a rounded corner at [distanceFromEdge] below the top. */
fun roundedCornerInset(distanceFromEdge: Float, radius: Float): Float {
    if (radius <= 0f || distanceFromEdge >= radius) return 0f
    val remaining = radius - distanceFromEdge.coerceAtLeast(0f)
    val inside = radius * radius - remaining * remaining
    if (inside <= 0f) return radius
    return radius - kotlin.math.sqrt(inside)
}

fun temperatureDialMetrics(
    zoneWidthDp: Float,
    zoneHeightDp: Float,
    showZoneLabel: Boolean,
): TemperatureDialMetrics {
    val width = zoneWidthDp.takeIf { it.isFinite() && it > 0f }
        ?: (ButtonPreferred * 2f + SpacingPreferred * 2f + 280f)
    val height = zoneHeightDp.takeIf { it.isFinite() && it > 0f }

    var spacing = SpacingPreferred
    var button = ButtonPreferred
    fun cardOf(): Float = width - button * 2f - spacing * 2f

    var card = cardOf()
    if (card < 220f) {
        spacing = SpacingTight
        card = cardOf()
    }
    if (card < CardMin) {
        button = ((width - CardMin - spacing * 2f) / 2f).coerceIn(ButtonMin, ButtonPreferred)
        card = cardOf()
    }
    if (card > CardMax) card = CardMax
    if (button * 2f + spacing * 2f + card > width) {
        card = (width - button * 2f - spacing * 2f).coerceAtLeast(96f)
    }

    val cornerInset = roundedCornerInset(TopPadding, DialCornerRadius)
    val sideCap = card * 0.16f
    val sidePadding = if (sideCap <= 8f) {
        sideCap.coerceAtLeast(0f)
    } else {
        (cornerInset + 20f).coerceIn(8f, sideCap)
    }

    var canvas = CanvasPreferred
    var readoutLine = ReadoutLinePreferred
    var readoutFont = ReadoutFontPreferred
    if (height != null) {
        val chrome = (if (showZoneLabel) ZoneLabelBlock else 0f) +
            TopPadding + BottomPadding + ReadoutGap
        val available = (height - chrome).coerceAtLeast(48f + 28f)
        val preferred = CanvasPreferred + ReadoutLinePreferred
        if (preferred > available) {
            val scale = available / preferred
            readoutLine = (ReadoutLinePreferred * scale).coerceIn(28f, ReadoutLinePreferred)
            readoutFont = (ReadoutFontPreferred * scale).coerceIn(22f, ReadoutFontPreferred)
            canvas = (available - readoutLine).coerceIn(48f, CanvasPreferred)
        }
    }

    return TemperatureDialMetrics(
        buttonSize = button,
        iconSize = button * (44f / 76f),
        spacing = spacing,
        cardWidth = card,
        sidePadding = sidePadding,
        topPadding = TopPadding,
        bottomPadding = BottomPadding,
        canvasHeight = canvas,
        readoutFontSp = readoutFont,
        readoutLineSp = readoutLine,
    )
}

/** True when a scale label's box clears the label drawn to its left. */
fun temperatureScaleLabelFits(
    centerPx: Float,
    labelWidthPx: Float,
    previousRightPx: Float,
    gapPx: Float,
): Boolean = centerPx - labelWidthPx / 2f >= previousRightPx + gapPx
