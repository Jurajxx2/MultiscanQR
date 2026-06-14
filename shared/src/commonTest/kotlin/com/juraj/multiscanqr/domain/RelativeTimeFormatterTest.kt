package com.juraj.multiscanqr.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeTimeFormatterTest {

    private val now = 1_750_000_000_000L

    @Test
    fun formatsBuckets() {
        assertEquals("just now", RelativeTimeFormatter.format(now, now - 10_000))
        assertEquals("5 min ago", RelativeTimeFormatter.format(now, now - 5 * 60_000))
        assertEquals("3 h ago", RelativeTimeFormatter.format(now, now - 3 * 3_600_000))
        assertEquals("2 d ago", RelativeTimeFormatter.format(now, now - 2 * 86_400_000))
    }

    @Test
    fun clampsFutureTimestampsToJustNow() {
        assertEquals("just now", RelativeTimeFormatter.format(now, now + 60_000))
    }
}
