package com.rubyhuntersky.liftlog.vision

data class Movement(
    val direction: Direction,
    val force: Force,
    val distance: Distance
) {
    override fun toString() = "${direction.name} $force × $distance"
}