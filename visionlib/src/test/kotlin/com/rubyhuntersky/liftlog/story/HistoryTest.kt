package com.rubyhuntersky.liftlog.story

import com.rubyhuntersky.tomedb.Tomic
import com.rubyhuntersky.tomedb.modOwnersOf
import com.rubyhuntersky.tomedb.tomicOf
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
                    date = Date(hourAgo),
                    direction = Direction.PullUps,
                    force = Force.Lbs(100),
                    distance = Distance.Reps(1)
                )
            ).flatten()
            owners.values.toSet()
        }
        val history = History(movements)
        val rounds = history.logDays.first().rounds
        assertEquals(2, rounds.size)
    }
}