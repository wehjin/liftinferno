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


private fun revise1(
    vision: LoggingVision.Loaded,
    @Suppress("UNUSED_PARAMETER") action: LoggingAction.AddMovement,
    edge: Edge
): Revision<LoggingVision> {
    val movement = Movement(Direction.Dips, Force.Lbs(100), Distance.Reps(5))
    val movementWish =
        movementStory(movement, edge).toWish {
            it.map(LoggingAction::ReceiveMovement).getOrElse(LoggingAction::Ignore)
        }
    return revision(vision, movementWish)
}

private fun revise1(
    vision: LoggingVision.Loaded,
    @Suppress("UNUSED_PARAMETER") action: LoggingAction.ReceiveMovement,
    @Suppress("UNUSED_PARAMETER") edge: Edge
): Revision<LoggingVision> {
    // TODO Revise the vision to hold movements instead of days.
    val newDays = listOf(vision.days.first().addMovement(action.movement, Date().time))
    return revision(vision.copy(days = newDays))
}

private fun reviseLogging(
    vision: LoggingVision,
    action: LoggingAction,
    edge: Edge
): Revision<LoggingVision> = when {
    vision is LoggingVision.Loaded
            && action is LoggingAction.AddMovement -> revise1(vision, action, edge)
    vision is LoggingVision.Loaded
            && action is LoggingAction.ReceiveMovement -> revise1(vision, action, edge)
    else -> fallbackRevision(vision, action)
}

fun loggingStory(edge: Edge): Story<LoggingVision, LoggingAction, Void> {
    val initial = LoggingVision.Loaded(fetchDays(), emptyList())
    return storyOf("logging", initial, ::reviseLogging, { StoryEnding.None }, edge)
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