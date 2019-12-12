package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.ExperimentalCoroutinesApi


sealed class MovementVision : Revisable<MovementVision, MovementAction> {

    data class Adding(val movement: Movement) : MovementVision() {
        override fun revise(action: MovementAction, edge: Edge): MovementVision {
            require(action is MovementAction.Cancel)
            return Dismissed(null)
        }
    }

    data class Dismissed(val movement: Movement?) : MovementVision() {
        override fun revise(action: MovementAction, edge: Edge): MovementVision = this
    }
}

sealed class MovementAction {
    object Cancel : MovementAction()
}

@ExperimentalCoroutinesApi
fun movementStory(movement: Movement, edge: Edge): Story<MovementVision, MovementAction> {
    return storyOf("add-movement", MovementVision.Adding(movement), edge)
}

