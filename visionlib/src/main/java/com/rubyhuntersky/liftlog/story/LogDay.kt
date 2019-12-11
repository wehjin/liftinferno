package com.rubyhuntersky.liftlog.story

import java.util.*

data class LogDay(
    val rounds: List<Round>
) {
    val startTime: Long by lazy { rounds.minBy { it.epoch }!!.epoch }

}
