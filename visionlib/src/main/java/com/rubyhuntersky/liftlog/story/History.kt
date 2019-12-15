package com.rubyhuntersky.liftlog.story

data class History(
    val movements: Set<Movement>
) {
    fun add(movement: Movement) = copy(movements = movements + movement)
}