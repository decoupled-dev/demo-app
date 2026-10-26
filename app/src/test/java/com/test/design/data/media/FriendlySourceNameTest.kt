package com.test.design.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class FriendlySourceNameTest {

    @Test
    fun packageNameBecomesReadableSource() {
        assertEquals("Spotify", friendlySourceName("com.android.spotify"))
        assertEquals("Spotify", friendlySourceName("com.spotify.music"))
        assertEquals("YouTube", friendlySourceName("com.google.android.youtube"))
        assertEquals("Bluetooth", friendlySourceName("com.android.bluetooth"))
    }

    @Test
    fun realApplicationLabelIsKept() {
        assertEquals("Spotify", friendlySourceName("Spotify"))
        assertEquals("YouTube Music", friendlySourceName("YouTube Music"))
    }
}
