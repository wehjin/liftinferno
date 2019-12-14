@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.rubyhuntersky.liftlog.story


sealed class MovementVision {
    data class Interacting(
        val startForce: Int,
        val startDistance: Int,
        val startDirection: Direction,
        val force: Int?,
        val distance: Int?,
        val direction: Direction
    ) : MovementVision() {
        val isReadyToAdd: Boolean by lazy {
            val componentsReady = force != null && distance != null
            componentsReady
        }
    }

    data class Dismissed(val movement: Movement?) : MovementVision()
}

sealed class MovementAction {
    object Cancel : MovementAction()
    object Add : MovementAction()
    data class SetForce(val force: Int?) : MovementAction()
    data class SetDistance(val distance: Int?) : MovementAction()
    data class SetDirection(val direction: Direction) : MovementAction()
}

fun movementStory(init: Movement, edge: Edge): Story<MovementVision, MovementAction, Movement> {
    return storyOf(edge, "add-movement") {
        take(MovementVision.Interacting::class.java, MovementAction.Cancel::class.java) {
            val newMovement = null
            give(MovementVision.Dismissed(newMovement))
        }
        take(MovementVision.Interacting::class.java, MovementAction.Add::class.java) {
            if (vision.isReadyToAdd) {
                val newMovement = Movement(
                    direction = vision.direction,
                    force = Force.Lbs(vision.force!!),
                    distance = Distance.Reps(vision.distance!!)
                )
                give(MovementVision.Dismissed(newMovement))
            } else give(vision)
        }
        take(MovementVision.Interacting::class.java, MovementAction.SetForce::class.java) {
            val newForce = action.force
            give(vision.copy(force = newForce))
        }
        take(MovementVision.Interacting::class.java, MovementAction.SetDistance::class.java) {
            val newDistance = action.distance
            give(vision.copy(distance = newDistance))
        }
        take(MovementVision.Interacting::class.java, MovementAction.SetDirection::class.java) {
            val newDirection = action.direction
            give(vision.copy(direction = newDirection))
        }
        take(MovementVision.Dismissed::class.java, MovementAction.Cancel::class.java) {
            give(vision)
        }
        ending = {
            when (it) {
                is MovementVision.Interacting -> storyEndingNone()
                is MovementVision.Dismissed -> storyEnding(it.movement)
            }
        }
        MovementVision.Interacting(
            startForce = init.force.value,
            startDistance = init.distance.count,
            startDirection = init.direction,
            force = init.force.value,
            distance = init.distance.count,
            direction = init.direction
        )
    }
}



