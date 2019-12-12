package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

interface Story<out V, in A> {
    fun subscribe(): ReceiveChannel<V>
    fun offer(action: A)
    fun close()
}

interface Revisable<out V, in A> {
    fun revise(action: A): V
}

@ExperimentalCoroutinesApi
fun <V, A> storyOf(initial: V): Story<V, A> where V : Revisable<V, A> {
    val visions = ConflatedBroadcastChannel(initial)
    val actions = Channel<A>(10)
    GlobalScope.launch {
        actions.consumeEach { action ->
            visions.value.revise(action).also { visions.offer(it) }
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
