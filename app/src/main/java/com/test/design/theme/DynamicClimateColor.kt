package com.test.design.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

private val CoolPrimary = Color(0xFF4FC3F7)
private val CoolSecondary = Color(0xFF0288D1)
private val CoolTertiary = Color(0xFF80DEEA)
private val CoolContainer = Color(0xFF1B3A4B)

private val WarmPrimary = Color(0xFFFF7043)
private val WarmSecondary = Color(0xFFE64A19)
private val WarmTertiary = Color(0xFFFFAB91)
private val WarmContainer = Color(0xFF4A1C12)

/**
 * Interpolates an expressive color scheme between cool (blue) and warm (red) hues
 * based on normalized temperature [0f, 1f], preserving [base] canvas roles for light/dark.
 */
fun climateColorScheme(
    temperatureFraction: Float,
    base: ColorScheme,
): ColorScheme {
    val fraction = temperatureFraction.coerceIn(0f, 1f)
    val darkCanvas = base.background.luminance() < 0.5f
    return if (darkCanvas) {
        darkClimateColorScheme(fraction, base)
    } else {
        lightClimateColorScheme(fraction, base)
    }
}

private fun darkClimateColorScheme(fraction: Float, base: ColorScheme): ColorScheme {
    val surfaceBlend = 0.85f
    val coolSurface = Color(0xFF0D1B2A)
    val warmSurface = Color(0xFF1A0A08)

    return base.copy(
        primary = lerp(CoolPrimary, WarmPrimary, fraction),
        onPrimary = Color.White,
        primaryContainer = lerp(CoolContainer, WarmContainer, fraction),
        onPrimaryContainer = lerp(Color(0xFFB3E5FC), Color(0xFFFFCCBC), fraction),
        inversePrimary = lerp(CoolSecondary, WarmSecondary, fraction),
        secondary = lerp(CoolSecondary, WarmSecondary, fraction),
        onSecondary = Color.White,
        secondaryContainer = lerp(Color(0xFF01579B), Color(0xFFBF360C), fraction),
        onSecondaryContainer = lerp(Color(0xFFB3E5FC), Color(0xFFFFCCBC), fraction),
        tertiary = lerp(CoolTertiary, WarmTertiary, fraction),
        onTertiary = Color(0xFF00363D),
        tertiaryContainer = lerp(Color(0xFF004D5A), Color(0xFF5D2108), fraction),
        onTertiaryContainer = lerp(Color(0xFFB2EBF2), Color(0xFFFFCCBC), fraction),
        surface = lerp(base.surface, lerp(coolSurface, warmSurface, fraction), surfaceBlend),
        surfaceVariant = lerp(
            base.surfaceVariant,
            lerp(Color(0xFF1E3A4F), Color(0xFF3E1A12), fraction),
            surfaceBlend,
        ),
        surfaceTint = lerp(CoolPrimary, WarmPrimary, fraction),
        outline = lerp(
            base.outline,
            lerp(Color(0xFF4A6A7A), Color(0xFF8A4A3A), fraction),
            0.5f,
        ),
        outlineVariant = lerp(
            base.outlineVariant,
            lerp(Color(0xFF2A4A5A), Color(0xFF5A2A1A), fraction),
            0.5f,
        ),
        surfaceBright = lerp(
            base.surfaceBright,
            lerp(Color(0xFF2A4A5A), Color(0xFF5A2A1A), fraction),
            surfaceBlend,
        ),
        surfaceDim = lerp(
            base.surfaceDim,
            lerp(Color(0xFF0A1520), Color(0xFF150808), fraction),
            surfaceBlend,
        ),
        surfaceContainer = lerp(
            base.surfaceContainer,
            lerp(Color(0xFF152535), Color(0xFF2A1010), fraction),
            surfaceBlend,
        ),
        surfaceContainerHigh = lerp(
            base.surfaceContainerHigh,
            lerp(Color(0xFF1E3040), Color(0xFF351818), fraction),
            surfaceBlend,
        ),
        surfaceContainerHighest = lerp(
            base.surfaceContainerHighest,
            lerp(Color(0xFF283A4A), Color(0xFF402020), fraction),
            surfaceBlend,
        ),
        surfaceContainerLow = lerp(
            base.surfaceContainerLow,
            lerp(Color(0xFF101E2A), Color(0xFF1E0C0C), fraction),
            surfaceBlend,
        ),
        surfaceContainerLowest = lerp(
            base.surfaceContainerLowest,
            lerp(Color(0xFF080F18), Color(0xFF100606), fraction),
            surfaceBlend,
        ),
    )
}

private val LightCoolPrimary = Color(0xFF0D47A1)
private val LightWarmPrimary = Color(0xFFD84315)
private val LightCoolInk = Color(0xFF014A75)
private val LightWarmInk = Color(0xFF8C2A0A)
private val LightCoolWash = Color(0xFFE7F4FB)
private val LightWarmWash = Color(0xFFFFF1EB)

/**
 * Daytime climate: pastel temperature wash on light sheets, with saturated ink
 * so dials, tracks, and active controls stay readable.
 */
private fun lightClimateColorScheme(fraction: Float, base: ColorScheme): ColorScheme {
    val wash = lerp(LightCoolWash, LightWarmWash, fraction)
    fun sheet(role: Color, amount: Float) = lerp(role, wash, amount)

    return base.copy(
        primary = lerp(LightCoolPrimary, LightWarmPrimary, fraction),
        onPrimary = Color.White,
        primaryContainer = lerp(Color(0xFFD7F0FA), Color(0xFFFFE3D6), fraction),
        onPrimaryContainer = lerp(LightCoolInk, LightWarmInk, fraction),
        inversePrimary = lerp(CoolPrimary, WarmPrimary, fraction),
        secondary = lerp(Color(0xFF0277BD), Color(0xFFE64A19), fraction),
        onSecondary = Color.White,
        secondaryContainer = lerp(Color(0xFFE3F2FB), Color(0xFFFFE8E0), fraction),
        onSecondaryContainer = lerp(Color(0xFF01579B), Color(0xFFBF360C), fraction),
        tertiary = lerp(Color(0xFF00695C), Color(0xFFEF6C00), fraction),
        onTertiary = Color.White,
        tertiaryContainer = lerp(Color(0xFFD4F1F4), Color(0xFFFFE0B2), fraction),
        onTertiaryContainer = lerp(Color(0xFF004D56), Color(0xFF8A3E00), fraction),
        surface = sheet(Color(0xFFF7F8FA), 0.55f),
        surfaceVariant = sheet(Color(0xFFE6EAEF), 0.35f),
        surfaceTint = lerp(LightCoolPrimary, LightWarmPrimary, fraction),
        outline = lerp(base.outline, lerp(Color(0xFF5C7A8A), Color(0xFF8A5A48), fraction), 0.28f),
        outlineVariant = lerp(
            base.outlineVariant,
            lerp(Color(0xFFC5D5DE), Color(0xFFE6CABB), fraction),
            0.4f,
        ),
        surfaceBright = sheet(Color.White, 0.2f),
        surfaceDim = sheet(Color(0xFFE8EBEE), 0.3f),
        surfaceContainer = sheet(Color(0xFFF3F5F7), 0.4f),
        surfaceContainerHigh = sheet(Color.White, 0.38f),
        surfaceContainerHighest = sheet(Color(0xFFE4E8EE), 0.32f),
        surfaceContainerLow = sheet(Color(0xFFF7F8FA), 0.62f),
        surfaceContainerLowest = sheet(Color.White, 0.16f),
    )
}

fun temperatureToFraction(celsius: Float, min: Float = 16f, max: Float = 30f): Float {
    val span = (max - min).takeIf { it > 0f } ?: return 0f
    return ((celsius - min) / span).coerceIn(0f, 1f)
}

fun temperatureToFraction(celsius: Int, min: Int = 16, max: Int = 30): Float =
    temperatureToFraction(celsius.toFloat(), min.toFloat(), max.toFloat())

/** Normalized fraction at/below which snowflakes begin (≈22°C for 16–30). */
const val ClimateCoolThresholdFraction = 0.42f

/**
 * Snow intensity for one cabin zone.
 * The warmer side of a dual-zone split always returns 0 so flakes only appear
 * on the cooler side (both sides may show when temperatures match).
 */
fun zoneCoolIntensity(ownFraction: Float, otherFraction: Float): Float {
    if (ownFraction > otherFraction) return 0f
    return ((ClimateCoolThresholdFraction - ownFraction) / ClimateCoolThresholdFraction)
        .coerceIn(0f, 1f)
}

/** Ambient glow / pill fill for a single zone temperature. */
fun climateAmbientColor(temperatureFraction: Float): Color =
    lerp(CoolPrimary, WarmPrimary, temperatureFraction.coerceIn(0f, 1f))

/** Temperature wash behind a dial. Light canvases stay pastel; dark canvases stay deep. */
fun climateDialFill(temperatureFraction: Float, lightCanvas: Boolean): Color {
    val fraction = temperatureFraction.coerceIn(0f, 1f)
    return if (lightCanvas) {
        lerp(Color(0xFFE3F2FB), Color(0xFFFFE8E0), fraction)
    } else {
        climatePillContainer(fraction)
    }
}

/**
 * Readable temperature accent. Neon glow on a dark card, deep ink on a light card.
 */
fun climateContentAccent(temperatureFraction: Float, lightCanvas: Boolean): Color {
    val fraction = temperatureFraction.coerceIn(0f, 1f)
    return if (lightCanvas) {
        lerp(Color(0xFF01579B), Color(0xFFBF360C), fraction)
    } else {
        climateAmbientColor(fraction)
    }
}

/** Numerals on a temperature dial. */
fun climateDialInk(temperatureFraction: Float, lightCanvas: Boolean): Color {
    val fraction = temperatureFraction.coerceIn(0f, 1f)
    return if (lightCanvas) {
        climateContentAccent(fraction, lightCanvas = true)
    } else {
        climateOnPill(fraction)
    }
}

fun climatePillContainer(temperatureFraction: Float): Color =
    lerp(CoolContainer, WarmContainer, temperatureFraction.coerceIn(0f, 1f))

fun climateOnPill(temperatureFraction: Float): Color =
    lerp(Color(0xFFB3E5FC), Color(0xFFFFCCBC), temperatureFraction.coerceIn(0f, 1f))

/** Font weight thickens as temperature rises (cool = light, warm = bold). */
fun climateTemperatureFontWeight(temperatureFraction: Float): Int =
    (280 + temperatureFraction.coerceIn(0f, 1f) * 520).toInt().coerceIn(280, 800)
