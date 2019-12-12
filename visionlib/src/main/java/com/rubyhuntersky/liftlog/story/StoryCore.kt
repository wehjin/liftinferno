package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.launch

interface Story<out V : Any, in A> {
    val name: String
    fun subscribe(): ReceiveChannel<V>
    fun offer(action: A)
}

interface Edge {
    fun <V : Any, A> project(story: Story<V, A>, isEnd: (Any) -> Boolean)
    fun findStory(id: Pair<String, Int>, receiveChannel: SendChannel<Story<*, *>?>)
}

fun <V : Any, A> fallbackRevision(vision: V, action: A): V {
    println("No revision case for $action x $vision")
    return vision
}

@ExperimentalCoroutinesApi
fun <V : Any, A> storyOf(
    name: String,
    initial: V,
    revise: (V, A, Edge) -> V,
    edge: Edge
): Story<V, A> {
    val visions = ConflatedBroadcastChannel(initial)
    val actions = Channel<A>(10)
    GlobalScope.launch {
        println("Story/$name launched")
        actions.consumeEach { action ->
            revise(visions.value, action, edge).also { visions.send(it) }
            println("Story/$name vision: ${visions.value}")
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
