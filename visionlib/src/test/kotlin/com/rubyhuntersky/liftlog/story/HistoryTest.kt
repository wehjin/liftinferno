package com.rubyhuntersky.liftlog.story

import com.rubyhuntersky.liftlog.millitime.milliTimeFloor
import com.rubyhuntersky.liftlog.millitime.milliTimePerDay
import com.rubyhuntersky.liftlog.millitime.milliTimePerHour
import com.rubyhuntersky.liftlog.millitime.milliTimePerMinute
import com.rubyhuntersky.tomedb.Tomic
import com.rubyhuntersky.tomedb.modOwnersOf
import com.rubyhuntersky.tomedb.tomicOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class HistoryTest {

    private val now = milliTimeFloor(Date().time) + 5 * milliTimePerHour
    private val yesterday = now - milliTimePerDay
    private val hourAgo = now - milliTimePerHour

    private fun start(name: String): Tomic {
        val dir = createTempDir("$name-", "historyTest").also { println("Location: $it") }
        return tomicOf(dir) { emptyList() }
    }

    @Test
    fun daySpotting() {
        val tomic = start("day-spotting")
        val movements = tomic.modOwnersOf(Movement.WHEN) {
            mods = listOf(
                modMovement(
                    ent = 1000L,
                    date = Date(now),
                    direction = Direction.PullUps,
                    force = Force.Lbs(100),
                    distance = Distance.Reps(3)
                ),
                modMovement(
                    ent = 1001L,
                    date = Date(yesterday),
                    direction = Direction.PullUps,
                    force = Force.Lbs(100),
                    distance = Distance.Reps(1)
                )
            ).flatten()
            owners.values.toSet()
        }
        val history = History(movements)
        val days = history.logDays
        assertEquals(2, days.size)
    }

    @Test
    fun largeGapRoundDetection() {
        val tomic = start("large-gap-round-detection")
        val movements = tomic.modOwnersOf(Movement.WHEN) {
            val time = now + 10 * milliTimePerMinute
            mods = listOf(
                modMovement(
                    ent = 1000L,
                    date = Date(time),
                    direction = Direction.PullUps,
                    force = Force.Lbs(100),
                    distance = Distance.Reps(3)
                ),
                modMovement(
                    ent = 1001L,
                    date = Date(time + 40 * milliTimePerMinute),
                    direction = Direction.PullUps,
                    force = Force.Lbs(100),
                    distance = Distance.Reps(1)
                )
            ).flatten()
            owners.values.toSet()
        }
        val history = History(movements)
        val days = history.logDays
        assertEquals(1, days.size)
        val rounds = days.first().rounds
        assertEquals(2, rounds.size)
    }
}