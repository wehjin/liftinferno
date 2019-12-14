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

@Suppress("UNUSED_PARAMETER")
private fun interactingCancel(
    vision: MovementVision.Interacting,
    action: MovementAction.Cancel,
    edge: Edge
) = revision(MovementVision.Dismissed(null) as MovementVision)

@Suppress("UNUSED_PARAMETER")
private fun interactingAdd(
    vision: MovementVision.Interacting,
    action: MovementAction.Add,
    edge: Edge
) =
    if (vision.isReadyToAdd) {
        val direction = vision.direction
        val force = Force.Lbs(vision.force!!)
        val distance = Distance.Reps(vision.distance!!)
        val movement = Movement(direction, force, distance)
        revision(MovementVision.Dismissed(movement) as MovementVision)
    } else revision(vision)

@Suppress("UNUSED_PARAMETER")
private fun setDirection(
    vision: MovementVision.Interacting,
    action: MovementAction.SetDirection,
    edge: Edge
) = revision(vision.copy(direction = action.direction) as MovementVision)

@Suppress("UNUSED_PARAMETER")
private fun setForce(
    vision: MovementVision.Interacting,
    action: MovementAction.SetForce,
    edge: Edge
) = revision(vision.copy(force = action.force))

@Suppress("UNUSED_PARAMETER")
private fun setDistance(
    vision: MovementVision.Interacting,
    action: MovementAction.SetDistance,
    edge: Edge
) = revision(vision.copy(distance = action.distance))

private fun dismissedCancel(
    vision: MovementVision.Dismissed,
    @Suppress("UNUSED_PARAMETER") action: MovementAction.Cancel,
    @Suppress("UNUSED_PARAMETER") edge: Edge
) = revision(vision)

private fun reviseMovement(
    vision: MovementVision,
    action: MovementAction,
    edge: Edge
): Revision<MovementVision> = when {
    vision is MovementVision.Interacting
            && action is MovementAction.Add -> interactingAdd(vision, action, edge)
    vision is MovementVision.Interacting
            && action is MovementAction.SetForce -> setForce(vision, action, edge)
    vision is MovementVision.Interacting
            && action is MovementAction.SetDistance -> setDistance(vision, action, edge)
    vision is MovementVision.Interacting
            && action is MovementAction.SetDirection -> setDirection(vision, action, edge)
    vision is MovementVision.Interacting
            && action is MovementAction.Cancel -> interactingCancel(vision, action, edge)
    vision is MovementVision.Dismissed
            && action is MovementAction.Cancel -> dismissedCancel(vision, action, edge)
    else -> fallbackRevision(vision, action)
}


fun movementStory(movement: Movement, edge: Edge): Story<MovementVision, MovementAction, Movement> =
    storyOf(
        name = "add-movement",
        initial = MovementVision.Interacting(
            startForce = movement.force.value,
            startDistance = movement.distance.count,
            startDirection = movement.direction,
            force = movement.force.value,
            distance = movement.distance.count,
            direction = movement.direction
        ) as MovementVision,
        revise = ::reviseMovement,
        visionToEnding = { vision ->
            when (vision) {
                is MovementVision.Interacting -> storyEndingNone()
                is MovementVision.Dismissed -> storyEnding(vision.movement)
            }
        },
        edge = edge
    )


