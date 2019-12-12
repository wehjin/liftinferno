package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit


interface Story<out V, in A> {
    fun subscribe(): ReceiveChannel<V>
    fun offer(action: A)
    fun close()
}

interface Revisionist<V> {
    fun advance(vision: V): V
}

@ExperimentalCoroutinesApi
fun <V, A : Revisionist<V>> storyOf(initial: V): Story<V, A> {
    val visions = ConflatedBroadcastChannel(initial)
    val actions = Channel<A>(10)
    GlobalScope.launch {
        actions.consumeEach { action ->
            action.advance(visions.value).also { visions.offer(it) }
        }
        visions.close()
    }
    return object : Story<V, A> {
        override fun subscribe(): ReceiveChannel<V> = visions.openSubscription()
        override fun offer(action: A) {
            actions.offer(action)
        }

        override fun close() {
            actions.cancel()
        }
    }
}

data class AddMovementVision(val movement: Movement) {
}

sealed class LoggingVision {
    data class Logging(
        val days: List<LogDay>,
        val options: List<MoveOption>
    ) : LoggingVision() {
        fun addMovement(): LoggingAction = LoggingAction.AddMovement
    }
}

sealed class LoggingAction : Revisionist<LoggingVision> {
    object AddMovement : LoggingAction() {
        override fun advance(vision: LoggingVision): LoggingVision {
            require(vision is LoggingVision.Logging)
            return vision
        }
    }
}


@ExperimentalCoroutinesApi
fun loggingStory(): Story<LoggingVision, LoggingAction> {
    val initial = LoggingVision.Logging(fetchDays(), emptyList()) as LoggingVision
    return storyOf<LoggingVision, Revisionist<LoggingVision>>(initial)
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