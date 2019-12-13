package com.rubyhuntersky.liftlog.story

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class LoggingVisionTest {

    @Test
    fun liftLogExists() {
        val vision = LoggingVision.Loaded(emptyList(), emptyList())
        assertNotNull(vision)
    }
}