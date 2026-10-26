package com.test.design.data.media

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

/** Where a home-widget tap should open the playing source. */
internal sealed interface MediaScreenChoice {
    /** The source package's own media screen (Spotify, Media App, and similar). */
    data class SourceApp(val packageName: String) : MediaScreenChoice

    /** Media Center, showing [component] when the source has no screen of its own. */
    data class MediaCenter(val component: String?) : MediaScreenChoice
}

/**
 * Prefer the active source's own media activity. Sources that only publish a
 * browser service (Bluetooth, and many radio services) open in Media Center.
 */
internal fun chooseMediaScreen(source: String, sourceHasTemplateActivity: Boolean): MediaScreenChoice {
    val packageName = mediaPackageOf(source)
    return if (packageName != null && sourceHasTemplateActivity) {
        MediaScreenChoice.SourceApp(packageName)
    } else {
        MediaScreenChoice.MediaCenter(source.trim().takeIf { it.contains('/') })
    }
}

internal fun mediaPackageOf(source: String): String? {
    val trimmed = source.trim()
    if (trimmed.isEmpty()) return null
    val slash = trimmed.indexOf('/')
    val packageName = if (slash >= 0) trimmed.substring(0, slash) else trimmed
    return packageName.takeIf { it.isNotBlank() && !it.contains(' ') }
}

private const val ACTION_MEDIA_TEMPLATE = "android.car.intent.action.MEDIA_TEMPLATE"
private const val EXTRA_MEDIA_COMPONENT = "android.car.intent.extra.MEDIA_COMPONENT"
private const val MEDIA_CENTER_PACKAGE = "com.android.car.media"
private val MEDIA_CENTER_ACTIVITIES = listOf(
    "com.android.car.media.MediaDispatcherActivity",
    "com.android.car.media.MediaActivity",
)

/**
 * Opens the playing source from the Scalable UI home media widget.
 * Returns true when the source app's own screen was started.
 */
fun launchSourceTemplate(context: Context, source: String): Boolean {
    val choice = chooseMediaScreen(source, sourceDeclaresTemplate(context, source))
    val target = choice as? MediaScreenChoice.SourceApp ?: return false
    val component = source.trim().takeIf { it.contains('/') }
    return startMediaIntent(
        context,
        mediaTemplateIntent(component).setPackage(target.packageName),
    )
}

/** Opens Media Center on the playing source, or its default screen when none is active. */
fun launchMediaCenter(context: Context, source: String): Boolean {
    val component = (chooseMediaScreen(source, sourceHasTemplateActivity = false) as MediaScreenChoice.MediaCenter)
        .component
    val app = context.applicationContext
    val dispatched = mediaTemplateIntent(component).setPackage(MEDIA_CENTER_PACKAGE)
    if (app.packageManager.resolveActivity(dispatched, PackageManager.MATCH_DEFAULT_ONLY) != null &&
        startMediaIntent(app, dispatched)
    ) {
        return true
    }
    return MEDIA_CENTER_ACTIVITIES.any { className ->
        startMediaIntent(
            app,
            mediaTemplateIntent(component).setClassName(MEDIA_CENTER_PACKAGE, className),
        )
    }
}

private fun sourceDeclaresTemplate(context: Context, source: String): Boolean {
    val packageName = mediaPackageOf(source) ?: return false
    val probe = Intent(ACTION_MEDIA_TEMPLATE).setPackage(packageName)
    return context.applicationContext.packageManager
        .resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY) != null
}

private fun mediaTemplateIntent(component: String?): Intent =
    Intent(ACTION_MEDIA_TEMPLATE).apply {
        if (component != null) putExtra(EXTRA_MEDIA_COMPONENT, component)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }

private fun startMediaIntent(context: Context, intent: Intent): Boolean {
    val app = context.applicationContext
    return runCatching { app.startActivity(intent) }
        .onFailure { error -> Log.w(TAG, "Could not open ${intent.`package` ?: intent.component}", error) }
        .isSuccess
}

private const val TAG = "ActiveMediaSource"
