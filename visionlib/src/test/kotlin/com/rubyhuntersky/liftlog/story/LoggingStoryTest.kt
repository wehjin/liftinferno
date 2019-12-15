package com.rubyhuntersky.liftlog.story

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class LoggingStoryTest {

    @Test
    fun liftLogExists() {
        val vision = LoggingStory.Vision.Loaded(emptyList(), emptyList())
        assertNotNull(vision)
    }
}