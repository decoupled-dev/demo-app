package com.test.design.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveMediaSourceTest {

    @Test
    fun sourceWithItsOwnScreenOpensThatApp() {
        val spotify = "com.spotify.music/com.spotify.music.automotive.SpotifyMediaBrowserService"
        val choice = chooseMediaScreen(spotify, sourceHasTemplateActivity = true)
        assertEquals(MediaScreenChoice.SourceApp("com.spotify.music"), choice)
    }

    @Test
    fun packageOnlySourceStillOpensThatApp() {
        val choice = chooseMediaScreen("com.android.car.media", sourceHasTemplateActivity = true)
        assertEquals(MediaScreenChoice.SourceApp("com.android.car.media"), choice)
    }

    @Test
    fun sourceWithoutAScreenOpensMediaCenterOnThatComponent() {
        val bluetooth = "com.android.bluetooth/com.android.bluetooth.avrcpcontroller.BluetoothMediaBrowserService"
        val choice = chooseMediaScreen(bluetooth, sourceHasTemplateActivity = false)
        assertEquals(MediaScreenChoice.MediaCenter(bluetooth), choice)
    }

    @Test
    fun missingSourceOpensMediaCenter() {
        val choice = chooseMediaScreen("", sourceHasTemplateActivity = false)
        assertEquals(MediaScreenChoice.MediaCenter(null), choice)
        assertNull(mediaPackageOf("  "))
    }
}