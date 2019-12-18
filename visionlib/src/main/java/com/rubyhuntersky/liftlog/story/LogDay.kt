package com.rubyhuntersky.liftlog.story

import com.rubyhuntersky.tomedb.Owner
import java.util.*

data class LogDay(
    val rounds: List<Round>
) {
    val startTime: Long by lazy { rounds.minBy { it.epoch }!!.epoch }

    fun addMovement(movement: Owner<Date>, time: Long): LogDay {
        return copy(rounds = rounds + Round(time, listOf(movement)))
    }
}
