package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.ExperimentalCoroutinesApi


sealed class MovementVision {
    data class Adding(val movement: Movement) : MovementVision()
    data class Dismissed(val movement: Movement?) : MovementVision()
}

sealed class MovementAction {
    object Cancel : MovementAction()
}


@Suppress("UNUSED_PARAMETER")
private fun revise1(
    vision: MovementVision.Adding,
    action: MovementAction.Cancel,
    edge: Edge
): MovementVision = MovementVision.Dismissed(null)

@Suppress("UNUSED_PARAMETER")
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
    vision is MovementVision.Adding
            && action is MovementAction.Cancel -> revise1(vision, action, edge)
    vision is MovementVision.Dismissed
            && action is MovementAction.Cancel -> revise1(vision, action, edge)
    else -> fallbackRevision(vision, action)
}


@ExperimentalCoroutinesApi
fun movementStory(movement: Movement, edge: Edge): Story<MovementVision, MovementAction> =
    storyOf(
        name = "add-movement",
        initial = MovementVision.Adding(movement) as MovementVision,
        revise = ::reviseMovement,
        edge = edge
    )


