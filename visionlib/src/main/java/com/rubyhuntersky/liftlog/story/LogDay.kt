package com.rubyhuntersky.liftlog.story

data class LogDay(
    val rounds: List<Round>
) {
    val startTime: Long by lazy { rounds.minBy { it.epoch }!!.epoch }

    fun addMovement(movement: Movement, time: Long): LogDay {
        return copy(rounds = rounds + Round(time, listOf(movement)))
    }
}
