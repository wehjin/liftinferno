@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.rubyhuntersky.liftlog.story

import java.util.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
object LoggingStory {

    sealed class Vision {
        @ExperimentalTime
        data class Loaded(
            val history: History,
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
            val suggested = Movement(
                milliTime = Date().time,
                direction = Direction.Dips,
                force = Force.Lbs(100),
                distance = Distance.Reps(5)
            )
            val addMovement = MovementStory(edge, suggested)
            give(vision, addMovement.toWish(Action::ReceiveMovement, Action::Ignore))
        }
        on(Vision.Loaded::class.java, Action.ReceiveMovement::class.java) {
            val newVision = vision.copy(history = vision.history.add(action.movement))
            give(newVision)
        }
        Vision.Loaded(History(emptySet()), emptyList())
    }
}
