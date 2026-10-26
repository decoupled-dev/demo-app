package com.test.design.data.media

import android.car.Car
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.test.design.presentation.ivi.media.MediaUiState
import com.test.design.presentation.ivi.media.Track
import com.test.design.presentation.ivi.media.extrapolatedPositionMs
import com.test.design.presentation.ivi.media.formatClockMs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Follows the car's active playback source the same way the launcher media widget does.
 * [android.car.media.CarMediaManager] names the source. Playback metadata comes from that
 * package's active [MediaController] (MediaSessionManager). MediaBrowser is only a fallback;
 * many sources reject browse connections while still publishing a session.
 *
 * [nowPlaying] is null when the platform media manager is unavailable. Callers keep the
 * simulated player in that case.
 */
class CarActiveMediaClient(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val started = AtomicBoolean(false)
    private val bound = AtomicBoolean(false)

    private val _nowPlaying = MutableStateFlow<MediaUiState?>(null)
    val nowPlaying: StateFlow<MediaUiState?> = _nowPlaying.asStateFlow()

    private var car: Car? = null
    private var mediaManager: Any? = null
    private var sourceListener: Any? = null
    private var listenerInterface: Class<*>? = null
    private val sessionManager = appContext.getSystemService(MediaSessionManager::class.java)
    private var activeControllers: List<MediaController> = emptyList()
    private var sessionsRegistered = false
    private var browser: MediaBrowser? = null
    private var browserController: MediaController? = null
    private var controller: MediaController? = null
    private var sourceComponent: ComponentName? = null
    private var metadata: MediaMetadata? = null
    private var playbackState: PlaybackState? = null
    private var queue: List<MediaSession.QueueItem>? = null

    private val sessionCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            this@CarActiveMediaClient.metadata = metadata
            publish()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            playbackState = state
            publish()
        }

        override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) {
            this@CarActiveMediaClient.queue = queue
            publish()
        }

        override fun onSessionDestroyed() {
            if (controller != null) {
                controller?.unregisterCallback(this)
                controller = null
            }
            rebind()
        }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        activeControllers = controllers.orEmpty()
        Log.i(TAG, "Active sessions: ${activeControllers.joinToString { it.packageName }}")
        rebind()
    }

    fun isLive(): Boolean = bound.get() || controller != null

    fun start() {
        if (!started.compareAndSet(false, true)) return
        registerActiveSessions()
        try {
            car = Car.createCar(
                appContext,
                mainHandler,
                Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER,
            ) { created, ready ->
                if (ready) bindMediaManager(created) else onCarDisconnected()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Car.createCar failed", t)
            started.set(false)
            bound.set(false)
            _nowPlaying.value = null
        }
    }

    fun refreshPosition() {
        if (!bound.get()) return
        val state = playbackState ?: return
        if (state.state != PlaybackState.STATE_PLAYING) return
        publish()
    }

    fun togglePlayback() {
        val controls = controller?.transportControls ?: return
        val playing = playbackState?.state == PlaybackState.STATE_PLAYING ||
            playbackState?.state == PlaybackState.STATE_BUFFERING
        if (playing) controls.pause() else controls.play()
    }

    fun skipNext() {
        controller?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        controller?.transportControls?.skipToPrevious()
    }

    /** Points Media Center's browse slot at the source that is actually playing. */
    fun showPlaybackSourceInBrowse() {
        val component = sourceComponent ?: return
        val manager = mediaManager ?: return
        runCatching {
            manager.javaClass
                .getMethod("setMediaSource", ComponentName::class.java, Int::class.javaPrimitiveType)
                .invoke(manager, component, MODE_BROWSE)
        }.onFailure { error ->
            Log.w(TAG, "Could not select browse source $component", error)
        }
    }

    fun skipTo(trackId: String) {
        val controls = controller?.transportControls ?: return
        val queueId = trackId.toLongOrNull()
        if (queueId != null && queueId != MediaSession.QueueItem.UNKNOWN_ID.toLong()) {
            controls.skipToQueueItem(queueId)
        } else if (trackId.isNotBlank()) {
            controls.playFromMediaId(trackId, null)
        }
    }

    private fun bindMediaManager(created: Car) {
        try {
            val serviceName = mediaServiceName()
            val manager = created.getCarManager(serviceName)
            if (manager == null) {
                Log.w(TAG, "Car media service '$serviceName' is unavailable")
                onCarDisconnected()
                return
            }
            mediaManager = manager
            val listenerClass = Class.forName("$MANAGER_CLASS\$MediaSourceChangedListener")
            listenerInterface = listenerClass
            val listener = Proxy.newProxyInstance(
                listenerClass.classLoader,
                arrayOf(listenerClass),
            ) { proxy, method, args ->
                if (method.declaringClass == Any::class.java) {
                    return@newProxyInstance when (method.name) {
                        "toString" -> "ActiveMediaSourceListener"
                        "hashCode" -> System.identityHashCode(proxy)
                        "equals" -> proxy === args?.getOrNull(0)
                        else -> null
                    }
                }
                if (method.name == "onMediaSourceChanged") {
                    val component = args?.getOrNull(0) as? ComponentName
                    mainHandler.post { onPlaybackSource(component) }
                }
                null
            }
            sourceListener = listener
            manager.javaClass
                .getMethod("addMediaSourceListener", listenerClass, Int::class.javaPrimitiveType)
                .invoke(manager, listener, MODE_PLAYBACK)
            val current = manager.javaClass
                .getMethod("getMediaSource", Int::class.javaPrimitiveType)
                .invoke(manager, MODE_PLAYBACK) as? ComponentName
            bound.set(true)
            onPlaybackSource(current)
            Log.i(TAG, "Bound to active playback source $current")
        } catch (t: Throwable) {
            Log.w(TAG, "CarMediaManager bind failed", t)
            onCarDisconnected()
        }
    }

    private fun onPlaybackSource(component: ComponentName?) {
        if (!bound.get()) return
        if (component == null) {
            disconnectBrowser()
            sourceComponent = null
            rebind()
            return
        }
        if (component == sourceComponent && (controller != null || browser?.isConnected == true)) {
            rebind()
            return
        }
        disconnectBrowser()
        sourceComponent = component
        rebind()
        val connection = object : MediaBrowser.ConnectionCallback() {
            override fun onConnected() {
                val currentBrowser = browser ?: return
                if (!currentBrowser.isConnected) return
                browserController = MediaController(appContext, currentBrowser.sessionToken)
                Log.i(TAG, "MediaBrowser connected for $component")
                rebind()
            }

            override fun onConnectionSuspended() {
                browserController = null
                Log.w(TAG, "MediaBrowser suspended for $component")
                rebind()
            }

            override fun onConnectionFailed() {
                browserController = null
                Log.w(TAG, "MediaBrowser connection failed for $component")
                rebind()
            }
        }
        val nextBrowser = MediaBrowser(appContext, component, connection, null)
        browser = nextBrowser
        nextBrowser.connect()
    }

    private fun registerActiveSessions() {
        if (sessionsRegistered) return
        val manager = sessionManager ?: return
        try {
            manager.addOnActiveSessionsChangedListener(sessionsListener, null, mainHandler)
            activeControllers = manager.getActiveSessions(null).orEmpty()
            sessionsRegistered = true
            Log.i(TAG, "Active sessions: ${activeControllers.joinToString { it.packageName }}")
            rebind()
        } catch (t: Throwable) {
            Log.w(TAG, "MediaSessionManager active sessions unavailable", t)
        }
    }

    private fun rebind() {
        val chosen = selectController()
        if (chosen == null) {
            swapController(null)
            publish()
            return
        }
        if (controller?.sessionToken == chosen.sessionToken) {
            metadata = chosen.metadata
            playbackState = chosen.playbackState
            queue = chosen.queue
            publish()
            return
        }
        swapController(chosen)
    }

    private fun selectController(): MediaController? {
        val packageName = sourceComponent?.packageName
        if (packageName != null) {
            activeControllers.firstOrNull { it.packageName == packageName && it.hasNowPlaying() }
                ?.let { return it }
        }
        activeControllers.firstOrNull { it.hasNowPlaying() }?.let { return it }
        if (packageName != null) {
            activeControllers.firstOrNull { it.packageName == packageName }?.let { return it }
        }
        val browsed = browserController
        if (browsed != null && browsed.hasNowPlaying()) return browsed
        return browsed
    }

    private fun swapController(next: MediaController?) {
        if (controller != null && controller !== next) {
            controller?.unregisterCallback(sessionCallback)
        }
        controller = next
        if (next != null) {
            next.registerCallback(sessionCallback, mainHandler)
            metadata = next.metadata
            playbackState = next.playbackState
            queue = next.queue
            Log.i(
                TAG,
                "Session ${next.packageName} state=${playbackState?.state} title=${titleOf(metadata)}",
            )
        } else {
            metadata = null
            playbackState = null
            queue = null
        }
        publish()
    }

    private fun publish() {
        if (!bound.get()) {
            _nowPlaying.value = null
            return
        }
        val component = sourceComponent
        val meta = metadata
        val state = playbackState
        val playing = state?.state == PlaybackState.STATE_PLAYING ||
            state?.state == PlaybackState.STATE_BUFFERING
        val durationMs = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val positionMs = if (state == null) {
            0L
        } else {
            extrapolatedPositionMs(
                positionMs = state.position,
                lastUpdateElapsedRealtimeMs = state.lastPositionUpdateTime,
                nowElapsedRealtimeMs = SystemClock.elapsedRealtime(),
                speed = state.playbackSpeed,
                isPlaying = state.state == PlaybackState.STATE_PLAYING,
            )
        }.let { position ->
            if (durationMs > 0L) position.coerceAtMost(durationMs) else position
        }
        val title = titleOf(meta)
            ?: if (component == null) "No active source" else "Nothing playing"
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
        val mediaId = meta?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) ?: title
        val sourceLabel = component?.let(::appLabel).orEmpty()
        val tracks = queue.orEmpty().map { item -> queueTrack(item) }
        val current = Track(
            id = mediaId,
            title = title,
            artist = artist,
            album = album,
            durationLabel = if (durationMs > 0L) formatClockMs(durationMs) else "0:00",
        )
        _nowPlaying.value = MediaUiState(
            currentTrack = current,
            queue = if (tracks.isEmpty()) listOf(current) else tracks,
            isPlaying = playing,
            progress = if (durationMs > 0L) {
                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            },
            elapsedLabel = formatClockMs(positionMs),
            isLive = true,
            sourceLabel = sourceLabel,
            sourceComponent = component?.flattenToString() ?: controller?.packageName.orEmpty(),
            artwork = artworkOf(meta),
        )
    }

    private fun onCarDisconnected() {
        bound.set(false)
        disconnectBrowser()
        unregisterListener()
        mediaManager = null
        runCatching { car?.disconnect() }
        car = null
        sourceComponent = null
        rebind()
        if (controller == null) {
            _nowPlaying.value = null
        }
    }

    private fun unregisterListener() {
        val manager = mediaManager ?: return
        val listener = sourceListener ?: return
        val listenerClass = listenerInterface ?: return
        runCatching {
            manager.javaClass
                .getMethod("removeMediaSourceListener", listenerClass, Int::class.javaPrimitiveType)
                .invoke(manager, listener, MODE_PLAYBACK)
        }
        sourceListener = null
    }

    private fun disconnectBrowser() {
        browserController = null
        val current = browser
        browser = null
        if (current != null) {
            runCatching { current.disconnect() }
        }
    }

    private fun appLabel(component: ComponentName): String {
        val raw = runCatching {
            val pm = appContext.packageManager
            pm.getApplicationLabel(
                pm.getApplicationInfo(component.packageName, PackageManager.GET_META_DATA),
            ).toString()
        }.getOrNull().orEmpty()
        return friendlySourceName(raw.ifBlank { component.packageName })
    }

    private fun mediaServiceName(): String = runCatching {
        Car::class.java.getField("CAR_MEDIA_SERVICE").get(null) as? String
    }.getOrNull() ?: CAR_MEDIA_SERVICE

    companion object {
        private const val TAG = "CarActiveMedia"
        private const val MANAGER_CLASS = "android.car.media.CarMediaManager"
        private const val CAR_MEDIA_SERVICE = "car_media"
        private const val MODE_PLAYBACK = 0
        private const val MODE_BROWSE = 1
    }
}

private fun MediaController.hasNowPlaying(): Boolean {
    if (!titleOf(metadata).isNullOrBlank()) return true
    return when (playbackState?.state) {
        PlaybackState.STATE_PLAYING,
        PlaybackState.STATE_BUFFERING,
        PlaybackState.STATE_PAUSED,
        PlaybackState.STATE_CONNECTING,
        -> true
        else -> false
    }
}

private fun titleOf(metadata: MediaMetadata?): String? {
    if (metadata == null) return null
    val keys = arrayOf(
        MediaMetadata.METADATA_KEY_DISPLAY_TITLE,
        MediaMetadata.METADATA_KEY_TITLE,
        MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
    )
    return keys.firstNotNullOfOrNull { key ->
        metadata.getString(key)?.takeIf { it.isNotBlank() }
            ?: metadata.getText(key)?.toString()?.takeIf { it.isNotBlank() }
    }
}

private fun artworkOf(metadata: MediaMetadata?): Bitmap? {
    if (metadata == null) return null
    val bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        ?: return null
    return if (bitmap.config == Bitmap.Config.HARDWARE) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        bitmap
    }
}

private fun queueTrack(item: MediaSession.QueueItem): Track {
    val description = item.description
    val id = if (item.queueId != MediaSession.QueueItem.UNKNOWN_ID.toLong()) {
        item.queueId.toString()
    } else {
        description.mediaId ?: item.queueId.toString()
    }
    return Track(
        id = id,
        title = description.title?.toString().orEmpty().ifBlank { "Track" },
        artist = description.subtitle?.toString().orEmpty(),
        album = "",
        durationLabel = "",
    )
}
