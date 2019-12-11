package com.rubyhuntersky.liftlog.vision

sealed class Distance {
    data class Reps(val count: Int) : Distance() {
        override fun toString() = "$count"
    }

    data class Time(val seconds: Int) : Distance() {
        override fun toString() = "${seconds}s"
    }
}