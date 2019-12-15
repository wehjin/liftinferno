package com.rubyhuntersky.liftlog.story

data class Movement(
    val milliTime: Long,
    val direction: Direction,
    val force: Force,
    val distance: Distance
) {
    fun setReps(count: Int) = copy(distance = Distance.Reps(count))
    fun setLbs(value: Int) = copy(force = Force.Lbs(value))

    override fun toString() = "${direction.name} $force × $distance"
}