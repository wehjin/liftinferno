package com.rubyhuntersky.liftlog.story

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class HistoryTest {

    private val now = Date().time
    private val yesterday =
        now - Duration.convert(1.0, DurationUnit.DAYS, DurationUnit.MILLISECONDS).toLong()
    private val hourAgo =
        now - Duration.convert(1.0, DurationUnit.HOURS, DurationUnit.MILLISECONDS).toLong()

    @Test
    fun addMovement() {
        val movement = Movement(now, Direction.PullUps, Force.Lbs(100), Distance.Reps(5))
        val history = History(emptySet()).add(movement)
        assertEquals(1, history.movements.size)
    }

    @Test
    fun daySpotting() {
        val movement1 = Movement(now, Direction.PullUps, Force.Lbs(100), Distance.Reps(3))
        val movement2 = Movement(yesterday, Direction.PullUps, Force.Lbs(100), Distance.Reps(1))
        val history = History(setOf(movement1, movement2))
        val days = history.logDays
        assertEquals(2, days.size)
    }

    @Test
    fun largeGapRoundDetection() {
        val movement1 = Movement(now, Direction.PullUps, Force.Lbs(100), Distance.Reps(3))
        val movement2 = Movement(hourAgo, Direction.PullUps, Force.Lbs(100), Distance.Reps(1))
        val history = History(setOf(movement1, movement2))
        val rounds = history.logDays.first().rounds
        assertEquals(2, rounds.size)
    }
}