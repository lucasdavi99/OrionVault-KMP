package com.cuboidestudio.orionvault.network

import kotlin.test.Test
import kotlin.test.assertEquals

class BreachCheckApiClientTest {
    @Test
    fun `parses well formed range response`() {
        val body = "003D68EB55068C33ACE09247EE4C639306:3\r\n0018A45C4D1DEF81644B54AB7F969B88D65:1\n"
        val entries = parseHibpRange(body)
        assertEquals(2, entries.size)
        assertEquals(HibpRangeEntry("003D68EB55068C33ACE09247EE4C639306", 3), entries[0])
        assertEquals(HibpRangeEntry("0018A45C4D1DEF81644B54AB7F969B88D65", 1), entries[1])
    }

    @Test
    fun `skips malformed lines leniently`() {
        val body = "GOODLINE:5\nno-colon-here\nANOTHERGOOD:2\n:notanumber\n"
        val entries = parseHibpRange(body)
        assertEquals(listOf(HibpRangeEntry("GOODLINE", 5), HibpRangeEntry("ANOTHERGOOD", 2)), entries)
    }

    @Test
    fun `empty body yields no entries`() {
        assertEquals(emptyList(), parseHibpRange(""))
    }
}
