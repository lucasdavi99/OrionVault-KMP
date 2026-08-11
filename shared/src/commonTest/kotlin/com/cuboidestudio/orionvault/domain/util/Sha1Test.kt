package com.cuboidestudio.orionvault.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals

class Sha1Test {
    @Test
    fun `hash of empty string matches FIPS vector`() {
        assertEquals("DA39A3EE5E6B4B0D3255BFEF95601890AFD80709", Sha1.hex(""))
    }

    @Test
    fun `hash of abc matches FIPS vector`() {
        assertEquals("A9993E364706816ABA3E25717850C26C9CD0D89D", Sha1.hex("abc"))
    }

    @Test
    fun `hash of password matches known HIBP vector`() {
        assertEquals("5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8", Sha1.hex("password"))
    }

    @Test
    fun `handles unicode input without crashing`() {
        val digest = Sha1.hex("senha-com-emoji-😀-e-ç")
        assertEquals(40, digest.length)
    }
}
