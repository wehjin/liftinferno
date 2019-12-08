package com.rubyhuntersky.liftlog.vision

sealed class Load {
    data class Lbs(val value: Int) : Load()
}