package com.rubyhuntersky.liftlog.story

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class LoggingStoryTest {

    @Test
    fun liftLogExists() {
        val vision = LoggingStory.Vision.Loaded(History(emptySet()), emptyList())
        assertNotNull(vision)
    }
}