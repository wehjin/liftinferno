@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.rubyhuntersky.liftlog.story

import java.util.*
import java.util.concurrent.TimeUnit

sealed class LoggingVision {
    data class Loaded(
        val days: List<LogDay>,
        val options: List<MoveOption>
    ) : LoggingVision() {
        fun buildAddMovementAction(): LoggingAction = LoggingAction.AddMovement
    }
}

sealed class LoggingAction {
    object AddMovement : LoggingAction()
    data class ReceiveMovement(val movement: Movement) : LoggingAction()
    data class Ignore(val ignore: Any) : LoggingAction()
}

fun loggingStory(edge: Edge): Story<LoggingVision, LoggingAction, Void> {
    return storyOf(edge, "logging") {
        take(LoggingVision.Loaded::class.java, LoggingAction.AddMovement::class.java) {
            val suggested = Movement(Direction.Dips, Force.Lbs(100), Distance.Reps(5))
            val addMovement = movementStory(suggested, edge)
            give(vision, addMovement.toWish(LoggingAction::ReceiveMovement, LoggingAction::Ignore))
        }
        take(LoggingVision.Loaded::class.java, LoggingAction.ReceiveMovement::class.java) {
            val newDays = listOf(vision.days.first().addMovement(action.movement, Date().time))
            give(vision.copy(days = newDays))
        }
        LoggingVision.Loaded(fetchDays(), emptyList())
    }
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