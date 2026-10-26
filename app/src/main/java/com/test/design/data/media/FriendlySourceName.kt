package com.test.design.data.media

/**
 * Turns a media-source package id into a short name for the widget.
 * A real application label such as "Spotify" is left as-is.
 */
fun friendlySourceName(raw: String): String {
    val label = raw.trim()
    if (label.isEmpty() || !label.looksLikePackageName()) return label
    val token = label.split('.')
        .filter { it.isNotEmpty() }
        .lastOrNull { it.lowercase() !in PACKAGE_NOISE }
        ?: return label
    return SOURCE_ALIASES[token.lowercase()]
        ?: token.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun String.looksLikePackageName(): Boolean = contains('.') && !contains(' ')

private val PACKAGE_NOISE = setOf(
    "com",
    "org",
    "net",
    "android",
    "google",
    "app",
    "apps",
    "music",
    "media",
)

private val SOURCE_ALIASES = mapOf(
    "spotify" to "Spotify",
    "youtube" to "YouTube",
    "youtubemusic" to "YouTube Music",
    "bluetooth" to "Bluetooth",
    "radio" to "Radio",
)
