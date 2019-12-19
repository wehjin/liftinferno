package com.rubyhuntersky.liftlog.millitime

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun milliDateOfTime(milliTime: Long): Long = milliTime / milliTimePerDay

@ExperimentalTime
fun milliTimeFloor(milliTime: Long): Long = milliTimeOfDate(milliDateOfTime(milliTime))

@ExperimentalTime
fun milliTimeOfDate(milliDate: Long): Long = milliDate * milliTimePerDay

@ExperimentalTime
val milliTimePerDay = Duration.convert(
    value = 1.0,
    sourceUnit = DurationUnit.DAYS,
    targetUnit = DurationUnit.MILLISECONDS
).toLong()

@ExperimentalTime
val milliTimePerHour = Duration.convert(
    value = 1.0,
    sourceUnit = DurationUnit.HOURS,
    targetUnit = DurationUnit.MILLISECONDS
).toLong()

@ExperimentalTime
val milliTimePerHalfHour = Duration.convert(
    value = 30.0,
    sourceUnit = DurationUnit.MINUTES,
    targetUnit = DurationUnit.MILLISECONDS
).toLong()

@ExperimentalTime
val milliTimePerMinute = Duration.convert(
    value = 1.0,
    sourceUnit = DurationUnit.MINUTES,
    targetUnit = DurationUnit.MILLISECONDS
).toLong()

