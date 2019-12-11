package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import java.util.*
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
fun loggingStory(): ConflatedBroadcastChannel<LoggingVision> {
    val initial = LoggingVision.Logging(fetchDays(), emptyList()) as LoggingVision
    return ConflatedBroadcastChannel(initial)
}

private fun fetchDays(): List<LogDay> {
    val now = Date()
    val tenMinutesAgo = now.time - TimeUnit.MINUTES.toMillis(10)
    return listOf(
        LogDay(
            rounds = listOf(
                Round(
                    epoch = tenMinutesAgo,
                    movements = listOf(
                        Movement(
                            direction = Direction.PullUps,
                            force = Force.Lbs(110),
                            distance = Distance.Reps(6)
                        ),
                        Movement(
                            direction = Direction.Squats,
                            force = Force.Lbs(110),
                            distance = Distance.Reps(6)
                        ),
                        Movement(
                            direction = Direction.Dips,
                            force = Force.Lbs(110),
                            distance = Distance.Reps(6)
                        )
                    )
                ),
                Round(
                    epoch = tenMinutesAgo,
                    movements = listOf(
                        Movement(
                            direction = Direction.PullUps,
                            force = Force.Lbs(110),
                            distance = Distance.Reps(6)
                        ),
                        Movement(
                            direction = Direction.Squats,
                            force = Force.Lbs(110),
                            distance = Distance.Reps(6)
                        ),
                        Movement(
                            direction = Direction.Dips,
                            force = Force.Lbs(110),
                            distance = Distance.Reps(6)
                        )
                    )
                )
            )
        )
    )
}