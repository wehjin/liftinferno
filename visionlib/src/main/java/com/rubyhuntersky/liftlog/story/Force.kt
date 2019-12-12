package com.rubyhuntersky.liftlog.story

sealed class Force {

    abstract val value: Int

    data class Lbs(override val value: Int) : Force() {
        override fun toString() = "$value lbs"
    }
}