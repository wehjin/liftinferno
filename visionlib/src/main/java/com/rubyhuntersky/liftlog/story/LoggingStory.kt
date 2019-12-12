package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*
import java.util.concurrent.TimeUnit


sealed class LoggingVision : Revisable<LoggingVision, LoggingAction> {

    data class Logging(val days: List<LogDay>, val options: List<MoveOption>) : LoggingVision() {
        fun buildAddAction(): LoggingAction = LoggingAction.AddMovement
        override fun revise(action: LoggingAction): LoggingVision {
            require(action is LoggingAction.AddMovement)
            return this
        }
    }
}

sealed class LoggingAction {
    object AddMovement : LoggingAction()
}

@ExperimentalCoroutinesApi
fun loggingStory(): Story<LoggingVision, LoggingAction> {
    return storyOf(initial = LoggingVision.Logging(fetchDays(), emptyList()))
}

private fun fetchDays(): List<LogDay> {
    val now = Date()
    val tenMinutesAgo = now.time - TimeUnit.MINUTES.toMillis(10)
    return listOf(
        LogDay(
            rounds = listOf(
                Round(
                    epoch = tenMinutesAgo,
                    movements = listOf(
                        Movement(
                            direction = Direction.PullUps,
                            force = Force.Lbs(110),
                            distance = Distance.Reps(6)
                        ),
                        Movement(
                            direction = Direction.Squats,
                            force = Force.Lbs(110),
                            distance = Distance.Reps(6)
                        ),
                        Movement(
                            direction = Direction.Dips,
                            force = Force.Lbs(110),
                            distance = Distance.Reps(6)
                        )
                    )
                ),
                Round(
                    epoch = tenMinutesAgo,
                    movements = listOf(
                        Movement(
                            direction = Direction.PullUps,
                            force = Force.Lbs(110),
                            distance = Distance.Reps(6)
                        ),
                        Movement(
                            direction = Direction.Squats,
                            force = Force.Lbs(110),
                            distance = Distance.Reps(6)
                        ),
                        Movement(
                            direction = Direction.Dips,
                            force = Force.Lbs(110),
                            distance = Distance.Reps(6)
                        )
                    )
                )
            )
        )
    )
}