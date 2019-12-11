package com.rubyhuntersky.liftlog.story

data class Movement(
    val direction: Direction,
    val force: Force,
    val distance: Distance
) {
    override fun toString() = "${direction.name} $force × $distance"
}