package com.rubyhuntersky.liftlog.vision

sealed class MainVision {
    data class Log(
        val days: List<LogDay>,
        val options: List<MoveOption>
    ) : MainVision()

    data class LoadFailed(
        val reason: String
    ) : MainVision()

    object Loading : MainVision()
}