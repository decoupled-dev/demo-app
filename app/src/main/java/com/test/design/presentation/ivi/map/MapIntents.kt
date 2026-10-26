package com.test.design.presentation.ivi.map

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.test.design.MainActivity
import com.test.design.presentation.ivi.navigation.components.MapCoordinate

/** Intent action to open the full-bleed map activity from in-app code or Scalable UI actions. */
const val ACTION_OPEN_MAP = "com.test.design.action.OPEN_MAP"

/** Optional boolean extra — when true, overlays the demo route on the map. */
const val EXTRA_SHOW_ROUTE = "com.test.design.extra.SHOW_ROUTE"

/** Optional double extra — initial map zoom level (defaults to 14.5). */
const val EXTRA_ZOOM = "com.test.design.extra.ZOOM"

/** When true, [MainActivity] opens the widget dashboard instead of driving home. */
const val EXTRA_OPEN_DASHBOARD = "com.test.design.extra.OPEN_DASHBOARD"

/** When true, [MapActivity] opens navigation (same as tapping Search maps). */
const val EXTRA_EXPAND_NAVIGATION = "com.test.design.extra.EXPAND_NAVIGATION"

/**
 * Launch configuration parsed from an incoming [Intent].
 *
 * Register this activity in AAOS Scalable UI via `config_default_activities`:
 * `map_panel;com.test.design/.presentation.ivi.map.MapActivity`
 */
data class MapLaunchConfig(
    val center: MapCoordinate? = null,
    val zoom: Double = 14.5,
    val showRoute: Boolean = false,
    val expandNavigation: Boolean = false,
) {
    companion object {
        fun default() = MapLaunchConfig()
    }
}

object MapIntents {

    fun openMap(
        context: Context,
        showRoute: Boolean = false,
        center: MapCoordinate? = null,
        zoom: Double = 14.5,
    ): Intent = Intent(context, MapActivity::class.java).apply {
        action = ACTION_OPEN_MAP
        if (context !is Activity) {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        putExtra(EXTRA_SHOW_ROUTE, showRoute)
        putExtra(EXTRA_ZOOM, zoom)
        putExtra(EXTRA_EXPAND_NAVIGATION, true)
        center?.let { geo ->
            data = Uri.parse("geo:${geo.latitude},${geo.longitude}")
        }
    }

    /** Opens the main app screen (driving home or widget dashboard). */
    fun openMain(
        context: Context,
        openDashboard: Boolean = false,
    ): Intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (openDashboard) {
            putExtra(EXTRA_OPEN_DASHBOARD, true)
        }
    }

    fun parseLaunchConfig(intent: Intent?): MapLaunchConfig {
        if (intent == null) return MapLaunchConfig.default()

        val showRoute = intent.getBooleanExtra(EXTRA_SHOW_ROUTE, false) ||
            intent.action == "androidx.car.app.action.NAVIGATE"
        val zoom = intent.getDoubleExtra(EXTRA_ZOOM, 14.5)
        val center = parseGeoCenter(intent.data)
        val expandNavigation = intent.getBooleanExtra(EXTRA_EXPAND_NAVIGATION, false) ||
            intent.action == ACTION_OPEN_MAP ||
            intent.action == Intent.ACTION_MAIN ||
            intent.action == Intent.ACTION_VIEW ||
            intent.action == "androidx.car.app.action.NAVIGATE"

        return MapLaunchConfig(
            center = center,
            zoom = zoom,
            showRoute = showRoute,
            expandNavigation = expandNavigation,
        )
    }

    private fun parseGeoCenter(uri: Uri?): MapCoordinate? {
        if (uri == null || uri.scheme != "geo") return null

        val schemeSpecific = uri.schemeSpecificPart ?: return null
        val coordinatePart = schemeSpecific.substringBefore("?").substringBefore(";")
        val parts = coordinatePart.split(",")
        if (parts.size < 2) return null

        val latitude = parts[0].trim().toDoubleOrNull() ?: return null
        val longitude = parts[1].trim().toDoubleOrNull() ?: return null
        if (latitude == 0.0 && longitude == 0.0) {
            return parseQueryCenter(uri)
        }
        return MapCoordinate(latitude, longitude)
    }

    private fun parseQueryCenter(uri: Uri): MapCoordinate? {
        val query = uri.getQueryParameter("q") ?: return null
        val coordinatePart = query.substringBefore("(").trim()
        val parts = coordinatePart.split(",")
        if (parts.size < 2) return null
        val latitude = parts[0].trim().toDoubleOrNull() ?: return null
        val longitude = parts[1].trim().toDoubleOrNull() ?: return null
        return MapCoordinate(latitude, longitude)
    }
}
