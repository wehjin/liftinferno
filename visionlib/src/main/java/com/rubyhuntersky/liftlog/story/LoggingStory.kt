package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.ExperimentalCoroutinesApi
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
}

@ExperimentalCoroutinesApi
private fun revise1(
    vision: LoggingVision.Loaded,
    @Suppress("UNUSED_PARAMETER") action: LoggingAction.AddMovement,
    edge: Edge
): LoggingVision {
    val movement = Movement(Direction.Dips, Force.Lbs(100), Distance.Reps(5))
    edge.project(movementStory(movement, edge)) { it is MovementVision.Dismissed }
    return vision
}

@ExperimentalCoroutinesApi
private fun reviseLogging(
    vision: LoggingVision,
    action: LoggingAction,
    edge: Edge
): LoggingVision = when {
    vision is LoggingVision.Loaded
            && action is LoggingAction.AddMovement -> revise1(vision, action, edge)
    else -> fallbackRevision(vision, action)
}

@ExperimentalCoroutinesApi
fun loggingStory(edge: Edge): Story<LoggingVision, LoggingAction> {
    val initial = LoggingVision.Loaded(fetchDays(), emptyList())
    return storyOf("logging", initial, ::reviseLogging, edge)
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