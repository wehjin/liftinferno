package com.rubyhuntersky.liftlog.story

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class HistoryTest {

    private val now = Date().time

    @Test
    fun addMovement() {
        val movement = Movement(now, Direction.PullUps, Force.Lbs(100), Distance.Reps(5))
        val history = History(emptySet()).add(movement)
        assertEquals(1, history.movements.size)
    }
}