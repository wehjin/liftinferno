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
    data class SetForce(val force: Int?) : MovementAction()
    data class SetDistance(val distance: Int?) : MovementAction()
    data class SetDirection(val direction: Direction) : MovementAction()
    object Add : MovementAction()
}

fun movementStory(init: Movement, edge: Edge): Story<MovementVision, MovementAction, Movement> {
    return newStoryOf(edge, "add-movement") {
        take(MovementVision.Interacting::class.java, MovementAction.Add::class.java) {
            if (vision.isReadyToAdd) {
                val movement = Movement(
                    direction = vision.direction,
                    force = Force.Lbs(vision.force!!),
                    distance = Distance.Reps(vision.distance!!)
                )
                give(MovementVision.Dismissed(movement))
            } else give(vision)
        }
        take(MovementVision.Interacting::class.java, MovementAction.SetForce::class.java) {
            give(vision.copy(force = action.force))
        }
        take(MovementVision.Interacting::class.java, MovementAction.SetDistance::class.java) {
            give(vision.copy(distance = action.distance))
        }
        take(MovementVision.Interacting::class.java, MovementAction.SetDirection::class.java) {
            give(vision.copy(direction = action.direction) as MovementVision)
        }
        take(MovementVision.Interacting::class.java, MovementAction.Cancel::class.java) {
            give(MovementVision.Dismissed(null) as MovementVision)
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


