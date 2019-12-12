package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.ExperimentalCoroutinesApi


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

private fun interactingCancel(
    vision: MovementVision.Interacting,
    action: MovementAction.Cancel,
    edge: Edge
): MovementVision = MovementVision.Dismissed(null)

private fun interactingAdd(
    vision: MovementVision.Interacting,
    action: MovementAction.Add,
    edge: Edge
): MovementVision =
    if (vision.isReadyToAdd) {
        val direction = vision.direction
        val force = Force.Lbs(vision.force!!)
        val distance = Distance.Reps(vision.distance!!)
        val movement = Movement(direction, force, distance)
        MovementVision.Dismissed(movement)
    } else vision

private fun setDirection(
    vision: MovementVision.Interacting,
    action: MovementAction.SetDirection,
    edge: Edge
): MovementVision = vision.copy(direction = action.direction)

private fun setForce(
    vision: MovementVision.Interacting,
    action: MovementAction.SetForce,
    edge: Edge
): MovementVision = vision.copy(force = action.force)

private fun setDistance(
    vision: MovementVision.Interacting,
    action: MovementAction.SetDistance,
    edge: Edge
): MovementVision = vision.copy(distance = action.distance)

private fun dismissedCancel(
    vision: MovementVision.Dismissed,
    action: MovementAction.Cancel,
    edge: Edge
): MovementVision = vision


private fun reviseMovement(
    vision: MovementVision,
    action: MovementAction,
    edge: Edge
): MovementVision = when {
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


@ExperimentalCoroutinesApi
fun movementStory(movement: Movement, edge: Edge): Story<MovementVision, MovementAction> =
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
        edge = edge
    )


