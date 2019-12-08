package com.rubyhuntersky.liftlog.vision

sealed class MainVision {
    data class Log(
        val past: List<LogDay>,
        val present: LogDay?,
        val options: List<MoveOption>
    ) : MainVision()

    data class LoadFailed(
        val reason: String
    ) : MainVision()

    object Loading : MainVision()
}