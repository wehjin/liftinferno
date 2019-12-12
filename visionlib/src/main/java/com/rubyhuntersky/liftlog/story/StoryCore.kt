package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

interface Story<out V : Any, in A> {
    val name: String
    fun subscribe(): ReceiveChannel<V>
    fun offer(action: A)
}

interface Revisable<out V, in A> {
    fun revise(action: A, edge: Edge): V
}

interface Edge {
    fun <V : Any, A> project(story: Story<V, A>, isEnd: (Any) -> Boolean)
}

@ExperimentalCoroutinesApi
fun <V, A> storyOf(name: String, initial: V, edge: Edge): Story<V, A> where V : Revisable<V, A> {
    val visions = ConflatedBroadcastChannel(initial)
    val actions = Channel<A>(10)
    GlobalScope.launch {
        try {
            println("Story/$name launched")
            actions.consumeEach { action ->
                visions.value.revise(action, edge).also {
                    visions.offer(it)
                    println("Story/$name vision: $it")
                }
            }
            println("Story/$name ended1")
            visions.close()
            println("Story/$name ended2")
        } catch (e: Throwable) {
            println("Story/$name threw $e")
        }
    }
    return object : Story<V, A> {
        override val name = name

        override fun subscribe(): ReceiveChannel<V> = visions.openSubscription()

        override fun offer(action: A) {
            actions.offer(action)
        }
    }
}
