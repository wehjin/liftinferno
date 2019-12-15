package com.rubyhuntersky.liftlog.story

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

@ExperimentalTime
data class History(
    val movements: Set<Movement>
) {
    fun add(movement: Movement) = copy(movements = movements + movement)

    val logDays: List<LogDay> by lazy {
        val groupedByDay = movements.groupBy { toMilliDate(it.milliTime) }
        val sorted = groupedByDay.entries.sortedBy { it.key }
        val rounds = sorted.map { (_, movements) -> toRounds(movements) }
        rounds.map {
            LogDay(it)
        }
    }
}

@ExperimentalTime
private fun toRounds(dayMovements: List<Movement>): List<Round> {
    val (milliTime, preround, rounds) = dayMovements.sortedBy(Movement::milliTime)
        .fold(
            initial = Triple(0L, emptyList<Movement>(), emptyList<Round>()),
            operation = { (milliTime, preround, rounds), movement ->
                val elapsed = movement.milliTime - milliTime
                if (elapsed > milliTimePerHalfHour) {
                    if (preround.isEmpty()) {
                        Triple(movement.milliTime, listOf(movement), rounds)
                    } else {
                        val nextRounds = rounds + Round(milliTime, preround)
                        Triple(movement.milliTime, listOf(movement), nextRounds)
                    }
                } else {
                    val newPreround = preround + movement
                    Triple(movement.milliTime, newPreround, rounds)
                }
            }
        )
    val lastRound = if (preround.isNotEmpty()) Round(milliTime, preround) else null
    return lastRound?.let { rounds + it } ?: rounds
}

@ExperimentalTime
fun toMilliDate(milliTime: Long): Long = milliTime / milliTimePerDay

@ExperimentalTime
private val milliTimePerDay =
    Duration.convert(1.0, DurationUnit.DAYS, DurationUnit.MILLISECONDS).toLong()

@ExperimentalTime
private val milliTimePerHalfHour =
    Duration.convert(30.0, DurationUnit.MINUTES, DurationUnit.MILLISECONDS).toLong()