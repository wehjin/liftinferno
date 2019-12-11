package com.rubyhuntersky.liftlog.vision

sealed class Force {
    data class Lbs(val value: Int) : Force() {
        override fun toString() = "$value lbs"
    }
}