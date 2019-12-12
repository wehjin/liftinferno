package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*
import java.util.concurrent.TimeUnit

sealed class LoggingVision : Revisable<LoggingVision, LoggingAction> {

    data class Logging(val days: List<LogDay>, val options: List<MoveOption>) : LoggingVision() {

        fun buildAddMovementAction(): LoggingAction = LoggingAction.AddMovement

        @ExperimentalCoroutinesApi
        override fun revise(action: LoggingAction, edge: Edge): LoggingVision {
            require(action is LoggingAction.AddMovement)
            val movement = Movement(Direction.Dips, Force.Lbs(100), Distance.Reps(5))
            edge.project(movementStory(movement, edge)) { it is MovementVision.Dismissed }
            return this
        }
    }
}

sealed class LoggingAction {
    object AddMovement : LoggingAction()
}

@ExperimentalCoroutinesApi
fun loggingStory(edge: Edge): Story<LoggingVision, LoggingAction> {
    val initial = LoggingVision.Logging(fetchDays(), emptyList())
    return storyOf("logging", initial, edge)
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