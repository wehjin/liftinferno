package com.rubyhuntersky.liftlog.vision

sealed class Magnitude {
    data class Reps(val count: Int) : Magnitude()
    data class Time(val seconds: Int) : Magnitude()
}