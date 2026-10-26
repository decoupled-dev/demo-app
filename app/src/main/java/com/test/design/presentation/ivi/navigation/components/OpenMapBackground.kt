package com.test.design.presentation.ivi.navigation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.Text
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/** San Francisco demo center — matches mock IVI navigation destination. */
val DefaultMapCenter = MapCoordinate(37.7749, -122.4194)

/** WGS84 point used by the in-app map. Independent of any maps vendor SDK. */
data class MapCoordinate(
    val latitude: Double,
    val longitude: Double,
)

internal val MapPlaceholderColor = Color(0xFF1A1C1E)

private const val OpenFreeMapDarkStyle = "https://tiles.openfreemap.org/styles/dark"

private const val RouteSourceId = "demo-route-source"
private const val RouteLayerId = "demo-route-layer"
private const val VehicleSourceId = "demo-vehicle-source"
private const val VehicleLayerId = "demo-vehicle-layer"
private const val DestinationSourceId = "demo-destination-source"
private const val DestinationLayerId = "demo-destination-layer"

private val DemoRoutePoints = listOf(
    MapCoordinate(37.7599, -122.4148),
    MapCoordinate(37.7655, -122.4190),
    MapCoordinate(37.7710, -122.4225),
    MapCoordinate(37.7749, -122.4194),
    MapCoordinate(37.7792, -122.4149),
)

private val RouteLayerIds = listOf(RouteLayerId, VehicleLayerId, DestinationLayerId)

/** Forces Compose chrome into its own layer so it stays above the map texture view. */
fun Modifier.mapChromeLayer(): Modifier = graphicsLayer { alpha = 0.99f }

/**
 * OpenStreetMap background via MapLibre and the free OpenFreeMap dark style.
 * No API key. Pair overlay chrome with [mapChromeLayer] so Compose siblings
 * draw above this texture-backed [MapView].
 */
@Composable
fun OpenMapBackground(
    modifier: Modifier = Modifier,
    center: MapCoordinate = DefaultMapCenter,
    zoom: Double = 14.5,
    showRoute: Boolean = true,
    interactive: Boolean = true,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var attachMap by remember { mutableStateOf(false) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        withFrameNanos { }
        attachMap = true
    }

    DisposableEffect(lifecycle, mapView) {
        val view = mapView
        if (view == null) {
            return@DisposableEffect onDispose { }
        }
        val destroyed = AtomicBoolean(false)
        val observer = LifecycleEventObserver { _, event ->
            if (destroyed.get()) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_START -> view.onStart()
                Lifecycle.Event.ON_RESUME -> view.onResume()
                Lifecycle.Event.ON_PAUSE -> view.onPause()
                Lifecycle.Event.ON_STOP -> view.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) view.onStart()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            if (destroyed.compareAndSet(false, true)) {
                view.onPause()
                view.onStop()
                view.onDestroy()
            }
        }
    }

    LaunchedEffect(map, center.latitude, center.longitude, zoom) {
        val readyMap = map ?: return@LaunchedEffect
        readyMap.cameraPosition = CameraPosition.Builder()
            .target(LatLng(center.latitude, center.longitude))
            .zoom(zoom)
            .build()
    }

    LaunchedEffect(map, interactive) {
        val readyMap = map ?: return@LaunchedEffect
        readyMap.uiSettings.apply {
            isRotateGesturesEnabled = interactive
            isScrollGesturesEnabled = interactive
            isTiltGesturesEnabled = interactive
            isZoomGesturesEnabled = interactive
        }
    }

    LaunchedEffect(map, styleReady, showRoute) {
        val readyMap = map ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        readyMap.getStyle { style ->
            val visibility = if (showRoute) Property.VISIBLE else Property.NONE
            RouteLayerIds.forEach { id ->
                style.getLayer(id)?.setProperties(PropertyFactory.visibility(visibility))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MapPlaceholderColor),
    ) {
        if (attachMap) {
            AndroidView(
                factory = { viewContext ->
                    MapLibre.getInstance(viewContext.applicationContext)
                    val options = MapLibreMapOptions.createFromAttributes(viewContext, null)
                        .textureMode(true)
                    MapView(viewContext, options).apply {
                        onCreate(null)
                        getMapAsync { readyMap ->
                            readyMap.setMinZoomPreference(3.0)
                            readyMap.setMaxZoomPreference(19.0)
                            readyMap.uiSettings.apply {
                                isCompassEnabled = false
                                isLogoEnabled = true
                                isAttributionEnabled = true
                                isScrollGesturesEnabled = interactive
                                isZoomGesturesEnabled = interactive
                                isRotateGesturesEnabled = interactive
                                isTiltGesturesEnabled = interactive
                            }
                            readyMap.setStyle(Style.Builder().fromUri(OpenFreeMapDarkStyle)) { style ->
                                if (showRoute) addDemoRoute(style)
                                map = readyMap
                                styleReady = true
                            }
                        }
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = "© OpenStreetMap · OpenFreeMap",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .mapChromeLayer()
                .padding(12.dp),
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 11.sp,
        )
    }
}

private fun addDemoRoute(style: Style) {
    style.addSource(GeoJsonSource(RouteSourceId, lineStringGeoJson(DemoRoutePoints)))
    style.addLayer(
        LineLayer(RouteLayerId, RouteSourceId).withProperties(
            PropertyFactory.lineColor("#4EA1FF"),
            PropertyFactory.lineWidth(5f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )

    val vehicle = DemoRoutePoints.first()
    val destination = DemoRoutePoints.last()
    style.addSource(GeoJsonSource(VehicleSourceId, pointGeoJson(vehicle)))
    style.addLayer(
        CircleLayer(VehicleLayerId, VehicleSourceId).withProperties(
            PropertyFactory.circleRadius(7f),
            PropertyFactory.circleColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(3f),
            PropertyFactory.circleStrokeColor("#4EA1FF"),
        ),
    )
    style.addSource(GeoJsonSource(DestinationSourceId, pointGeoJson(destination)))
    style.addLayer(
        CircleLayer(DestinationLayerId, DestinationSourceId).withProperties(
            PropertyFactory.circleRadius(8f),
            PropertyFactory.circleColor("#4EA1FF"),
            PropertyFactory.circleStrokeWidth(2f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
        ),
    )
}

private fun lineStringGeoJson(points: List<MapCoordinate>): String {
    val coordinates = points.joinToString(",") { point ->
        "[${formatCoord(point.longitude)},${formatCoord(point.latitude)}]"
    }
    return """{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coordinates]}}"""
}

private fun pointGeoJson(point: MapCoordinate): String =
    """{"type":"Feature","geometry":{"type":"Point","coordinates":[${formatCoord(point.longitude)},${formatCoord(point.latitude)}]}}"""

private fun formatCoord(value: Double): String = String.format(Locale.US, "%.6f", value)
