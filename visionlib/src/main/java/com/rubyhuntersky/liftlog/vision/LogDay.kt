package com.rubyhuntersky.liftlog.vision

data class LogDay(
    val epoch: Long,
    val rounds: List<Round>
)