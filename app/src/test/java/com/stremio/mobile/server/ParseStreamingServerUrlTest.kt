package com.stremio.mobile.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseStreamingServerUrlTest {
    @Test
    fun `keeps a plain LAN url`() {
        assertEquals("http://192.168.1.10:11470", parseStreamingServerUrl("http://192.168.1.10:11470"))
    }

    @Test
    fun `trims whitespace and trailing slashes`() {
        assertEquals("http://192.168.1.10:11470", parseStreamingServerUrl("  http://192.168.1.10:11470//  "))
        assertEquals("https://pc.example.ts.net/stremio", parseStreamingServerUrl("https://pc.example.ts.net/stremio/"))
    }

    @Test
    fun `lowercases scheme and host`() {
        assertEquals("http://mypc:11470", parseStreamingServerUrl("HTTP://MyPC:11470"))
    }

    @Test
    fun `drops default ports`() {
        assertEquals("http://pc", parseStreamingServerUrl("http://pc:80"))
        assertEquals("https://pc", parseStreamingServerUrl("https://pc:443/"))
        assertEquals("https://pc:80", parseStreamingServerUrl("https://pc:80"))
    }

    @Test
    fun `removes dot segments`() {
        assertEquals("http://pc:11470/b", parseStreamingServerUrl("http://pc:11470/a/../b"))
        assertEquals("http://pc:11470", parseStreamingServerUrl("http://pc:11470/./"))
    }

    @Test
    fun `rejects blank and non-http input`() {
        assertNull(parseStreamingServerUrl(""))
        assertNull(parseStreamingServerUrl("   "))
        assertNull(parseStreamingServerUrl("192.168.1.10:11470"))
        assertNull(parseStreamingServerUrl("ftp://pc:11470"))
        assertNull(parseStreamingServerUrl("http://"))
    }

    @Test
    fun `rejects invalid ports and hosts`() {
        assertNull(parseStreamingServerUrl("http://192.168.1.10:114700"))
        assertNull(parseStreamingServerUrl("http://192.168.1.10:0"))
        assertNull(parseStreamingServerUrl("http://192.168.1.300:11470"))
    }

    @Test
    fun `rejects query, fragment and user info`() {
        assertNull(parseStreamingServerUrl("http://pc:11470/?x=1"))
        assertNull(parseStreamingServerUrl("http://pc:11470/#/"))
        assertNull(parseStreamingServerUrl("http://user:pass@pc:11470"))
    }
}
