package com.arnav.sculpt.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideModeTest {

    @Test
    fun next_cyclesThroughEveryModeAndWrapsAround() {
        assertEquals(GuideMode.FRAME, GuideMode.OFF.next())
        assertEquals(GuideMode.BODY, GuideMode.FRAME.next())
        assertEquals(GuideMode.OFF, GuideMode.BODY.next())
    }

    @Test
    fun fromOrdinal_restoresSavedModes() {
        GuideMode.entries.forEach { assertEquals(it, GuideMode.fromOrdinal(it.ordinal)) }
    }

    @Test
    fun fromOrdinal_fallsBackToTheBodyOutlineForUnknownValues() {
        assertEquals(GuideMode.BODY, GuideMode.fromOrdinal(99))
        assertEquals(GuideMode.BODY, GuideMode.fromOrdinal(-1))
    }
}
