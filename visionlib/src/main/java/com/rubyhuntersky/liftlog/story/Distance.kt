package com.rubyhuntersky.liftlog.story

sealed class Distance {

    abstract val count: Int

    data class Reps(override val count: Int) : Distance() {
        override fun toString() = "$count"
    }

    data class Seconds(override val count: Int) : Distance() {
        override fun toString() = "${count}s"
    }
}