package com.rubyhuntersky.liftlog.vision

data class LogDay(
    val rounds: List<Round>
) {
    val startEpoch: Long by lazy { rounds.minBy { it.epoch }!!.epoch }
}