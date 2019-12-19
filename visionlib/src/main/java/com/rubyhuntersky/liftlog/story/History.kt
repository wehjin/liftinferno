package com.rubyhuntersky.liftlog.story

import com.rubyhuntersky.liftlog.millitime.milliTimePerHalfHour
import com.rubyhuntersky.liftlog.millitime.milliDateOfTime
import com.rubyhuntersky.tomedb.Owner
import java.util.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
data class History(
    val movements: Set<Owner<Date>>
) {
    val logDays: List<LogDay> by lazy {
        val groupedByDay = movements.groupBy { milliDateOfTime(milliTime = it[Movement.WHEN]!!.time) }
        val sorted = groupedByDay.entries.sortedBy { it.key }
        val rounds = sorted.map { (_, movements) ->
            toRounds(movements)
        }
        rounds.map {
            LogDay(it)
        }
    }
}

@ExperimentalTime
private fun toRounds(dayMovements: List<Owner<Date>>): List<Round> {
    val (milliTime, preround, rounds) = dayMovements.sortedBy { it[Movement.WHEN] }.fold(
        initial = Triple(0L, emptyList<Owner<Date>>(), emptyList<Round>()),
        operation = { (milliTime, preround, rounds), move ->
            val movementMilliTime = move[Movement.WHEN]!!.time
            val elapsed = movementMilliTime - milliTime
            if (elapsed > milliTimePerHalfHour) {
                if (preround.isEmpty()) {
                    Triple(movementMilliTime, listOf(move), rounds)
                } else {
                    val nextRounds = rounds + Round(milliTime, preround)
                    Triple(movementMilliTime, listOf(move), nextRounds)
                }
            } else {
                val newPreround = preround + move
                Triple(movementMilliTime, newPreround, rounds)
            }
        }
    )
    val lastRound = if (preround.isNotEmpty()) Round(milliTime, preround) else null
    return lastRound?.let { rounds + it } ?: rounds
}
