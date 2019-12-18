@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.rubyhuntersky.liftlog.story

import com.rubyhuntersky.tomedb.Owner
import com.rubyhuntersky.tomedb.Tomic
import com.rubyhuntersky.tomedb.ownerList
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
        data class ReceiveMovement(val movement: Owner<Date>) : Action()
        data class Ignore(val ignore: Any) : Action()
    }

    operator fun invoke(edge: Edge, tomic: Tomic): Story<Vision, Void> = storyOf(edge, "logging") {
        val start = Vision.Loaded(
            history = History(tomic.ownerList(Movement.WHEN).toSet()),
            options = emptyList()
        )
        on(Vision.Loaded::class.java, Action.AddMovement::class.java) {
            val addMovement = MovementStory(
                edge,
                tomic,
                Direction.Squats,
                Force.Lbs(100),
                Distance.Reps(5)
            )
            val wish = addMovement.toWish(
                onSuccess = Action::ReceiveMovement,
                onFailure = Action::Ignore
            )
            give(vision, wish)
        }
        on(Vision.Loaded::class.java, Action.ReceiveMovement::class.java) {
            val newMovements = tomic.ownerList(Movement.WHEN).toSet()
            val newHistory = vision.history.copy(movements = newMovements)
            val newVision = vision.copy(history = newHistory)
            give(newVision)
        }
        start
    }
}
