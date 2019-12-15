@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.rubyhuntersky.liftlog.story

import java.util.*
import java.util.concurrent.TimeUnit

object LoggingStory {

    sealed class Vision {
        data class Loaded(
            val days: List<LogDay>,
            val options: List<MoveOption>
        ) : Vision() {
            fun addAction() = Action.AddMovement as Any
        }
    }

    private sealed class Action {
        object AddMovement : Action()
        data class ReceiveMovement(val movement: Movement) : Action()
        data class Ignore(val ignore: Any) : Action()
    }

    operator fun invoke(edge: Edge): Story<Vision, Void> = storyOf(edge, "logging") {
        on(Vision.Loaded::class.java, Action.AddMovement::class.java) {
            val suggested = Movement(Direction.Dips, Force.Lbs(100), Distance.Reps(5))
            val addMovement = MovementStory(suggested, edge)
            give(vision, addMovement.toWish(Action::ReceiveMovement, Action::Ignore))
        }
        on(Vision.Loaded::class.java, Action.ReceiveMovement::class.java) {
            val newDays = listOf(vision.days.first().addMovement(action.movement, Date().time))
            give(vision.copy(days = newDays))
        }
        Vision.Loaded(fetchDays(), emptyList())
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