package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

sealed class LoggingVision {
    data class Logging(
        val days: List<LogDay>,
        val options: List<MoveOption>
    ) : LoggingVision()
}

sealed class LoggingAction {
    abstract fun advance(vision: LoggingVision): LoggingVision

    data class AddMovement(val movement: Movement) : LoggingAction() {
        override fun advance(vision: LoggingVision): LoggingVision {
            require(vision is LoggingVision.Logging)
            val day = vision.days.first().addMovement(movement, Date().time)
            return vision.copy(days = listOf(day))
        }
    }
}

@ExperimentalCoroutinesApi
fun loggingStory(): Triple<() -> ReceiveChannel<LoggingVision>, (LoggingAction) -> Unit, Job> {
    val initial = LoggingVision.Logging(fetchDays(), emptyList()) as LoggingVision
    val visions = ConflatedBroadcastChannel(initial)
    val actions = Channel<LoggingAction>(10)
    val job = GlobalScope.launch {
        actions.consumeEach { action ->
            action.advance(visions.value).also { visions.offer(it) }
        }
        visions.close()
    }
    return Triple({ visions.openSubscription() }, { action -> actions.offer(action) }, job)
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