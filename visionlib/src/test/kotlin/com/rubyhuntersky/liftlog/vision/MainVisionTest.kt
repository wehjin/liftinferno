package com.rubyhuntersky.liftlog.vision

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class MainVisionTest {
    @Test
    fun liftLogExists() {
        val vision = MainVision.Log(emptyList(), null, emptyList())
        assertNotNull(vision)
    }

    @Test
    fun loadingFailedExists() {
        val vision = MainVision.LoadFailed(reason = "Needs new encabulator.")
        assertNotNull(vision)
    }

    @Test
    fun loadingExists() {
        val vision = MainVision.Loading
        assertNotNull(vision)
    }
}