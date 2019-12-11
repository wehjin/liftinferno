package com.rubyhuntersky.liftlog.story

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class LoggingVisionTest {
    @Test
    fun liftLogExists() {
        val vision = LoggingVision.Logging(emptyList(), emptyList())
        assertNotNull(vision)
    }

    @Test
    fun loadingFailedExists() {
        val vision = LoggingVision.LoadFailed(reason = "Needs new encabulator.")
        assertNotNull(vision)
    }

    @Test
    fun loadingExists() {
        val vision = LoggingVision.Loading
        assertNotNull(vision)
    }
}