package com.rubyhuntersky.liftlog.vision

sealed class Motion {
    object PullUp : Motion()
    object Squat : Motion()
    object Dip : Motion()
    object Hinge : Motion()
    object PushUp : Motion()
    object Row : Motion()
}