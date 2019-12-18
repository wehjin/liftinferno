@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.rubyhuntersky.liftlog.story

import com.rubyhuntersky.tomedb.Owner
import com.rubyhuntersky.tomedb.Tomic
import com.rubyhuntersky.tomedb.modOwnersOf
import java.util.*
import kotlin.math.absoluteValue
import kotlin.random.Random


object MovementStory {

    sealed class Vision {
        data class Interacting(
            val startForce: Int,
            val startDistance: Int,
            val startDirection: Direction,
            val force: Int?,
            val distance: Int?,
            val direction: Direction
        ) : Vision() {
            val isReadyToAdd: Boolean by lazy {
                val componentsReady = force != null && distance != null
                componentsReady
            }

            fun addAction() = Action.Add as Any
            fun directionAction(direction: Direction) = Action.SetDirection(direction) as Any
            fun forceAction(force: Int?) = Action.SetForce(force) as Any
            fun distanceAction(distance: Int?) = Action.SetDistance(distance) as Any
        }

        data class Dismissed(val movement: Owner<Date>?) : Vision()
    }

    private sealed class Action {
        object Cancel : Action()
        object Add : Action()
        data class SetForce(val force: Int?) : Action()
        data class SetDistance(val distance: Int?) : Action()
        data class SetDirection(val direction: Direction) : Action()
    }

    operator fun invoke(
        edge: Edge,
        tomic: Tomic,
        direction: Direction,
        force: Force,
        distance: Distance
    ) = storyOf<Vision, Owner<Date>>(edge, "add-movement") {
        val start = Vision.Interacting(
            startForce = force.value,
            startDistance = distance.count,
            startDirection = direction,
            force = force.value,
            distance = distance.count,
            direction = direction
        )
        on(Vision.Interacting::class.java, Action.Cancel::class.java) {
            val newMovement = null
            give(Vision.Dismissed(newMovement))
        }
        on(Vision.Interacting::class.java, Action.Add::class.java) {
            tomic.modOwnersOf(Movement.WHEN) {
                if (vision.isReadyToAdd) {
                    val ent = Random.nextLong().absoluteValue
                    val date = Date()
                    mods = modMovement(
                        ent = ent,
                        date = date,
                        direction = vision.direction,
                        force = Force.Lbs(vision.force!!),
                        distance = Distance.Reps(vision.distance!!)
                    )
                    give(Vision.Dismissed(owners[ent]))
                } else give(vision)
            }
        }
        on(Vision.Interacting::class.java, Action.SetForce::class.java) {
            val newForce = action.force
            give(vision.copy(force = newForce))
        }
        on(Vision.Interacting::class.java, Action.SetDistance::class.java) {
            val newDistance = action.distance
            give(vision.copy(distance = newDistance))
        }
        on(Vision.Interacting::class.java, Action.SetDirection::class.java) {
            val newDirection = action.direction
            give(vision.copy(direction = newDirection))
        }
        on(Vision.Dismissed::class.java, Action.Cancel::class.java) {
            give(vision)
        }
        ending = {
            when (it) {
                is Vision.Interacting -> storyEndingNone()
                is Vision.Dismissed -> storyEnding(it.movement)
            }
        }
        cancel = { Action.Cancel }
        start
    }
}
