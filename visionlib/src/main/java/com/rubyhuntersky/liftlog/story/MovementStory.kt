package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.ExperimentalCoroutinesApi


sealed class MovementVision {
    data class Interacting(
        val force: Int?,
        val distance: Int?,
        val direction: Direction?
    ) : MovementVision()

    data class Dismissed(val movement: Movement?) : MovementVision()
}

sealed class MovementAction {
    object Cancel : MovementAction()
    data class SetForce(val force: Int?) : MovementAction()
    data class SetDistance(val distance: Int?) : MovementAction()
    data class SetDirection(val direction: Direction?) : MovementAction()
}

private fun revise1(
    vision: MovementVision.Interacting,
    action: MovementAction.SetDirection,
    edge: Edge
): MovementVision = vision.copy(direction = action.direction)

private fun revise1(
    vision: MovementVision.Interacting,
    action: MovementAction.SetForce,
    edge: Edge
): MovementVision = vision.copy(force = action.force)

private fun revise1(
    vision: MovementVision.Interacting,
    action: MovementAction.SetDistance,
    edge: Edge
): MovementVision = vision.copy(distance = action.distance)

private fun revise1(
    vision: MovementVision.Interacting,
    action: MovementAction.Cancel,
    edge: Edge
): MovementVision = MovementVision.Dismissed(null)

private fun revise1(
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
            && action is MovementAction.SetForce -> revise1(vision, action, edge)
    vision is MovementVision.Interacting
            && action is MovementAction.SetDistance -> revise1(vision, action, edge)
    vision is MovementVision.Interacting
            && action is MovementAction.SetDirection -> revise1(vision, action, edge)
    vision is MovementVision.Interacting
            && action is MovementAction.Cancel -> revise1(vision, action, edge)
    vision is MovementVision.Dismissed
            && action is MovementAction.Cancel -> revise1(vision, action, edge)
    else -> fallbackRevision(vision, action)
}


@ExperimentalCoroutinesApi
fun movementStory(movement: Movement, edge: Edge): Story<MovementVision, MovementAction> =
    storyOf(
        name = "add-movement",
        initial = MovementVision.Interacting(
            force = movement.force.value,
            distance = movement.distance.count,
            direction = movement.direction
        ) as MovementVision,
        revise = ::reviseMovement,
        edge = edge
    )


