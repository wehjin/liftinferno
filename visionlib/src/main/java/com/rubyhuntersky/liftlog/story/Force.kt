package com.rubyhuntersky.liftlog.story

sealed class Force {
    data class Lbs(val value: Int) : Force() {
        override fun toString() = "$value lbs"
    }
}