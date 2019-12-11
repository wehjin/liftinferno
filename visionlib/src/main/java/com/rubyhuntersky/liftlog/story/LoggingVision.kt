package com.rubyhuntersky.liftlog.story

sealed class LoggingVision {
    data class Logging(
        val days: List<LogDay>,
        val options: List<MoveOption>
    ) : LoggingVision()

    data class LoadFailed(
        val reason: String
    ) : LoggingVision()

    object Loading : LoggingVision()
}