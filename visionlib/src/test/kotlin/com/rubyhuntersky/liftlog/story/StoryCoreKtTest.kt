@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


internal class StoryCoreKtTest {

    object MyEdge : Edge {
        @ExperimentalCoroutinesApi
        override val well = WishWell()

        override fun <V : Any, A : Any, E : Any> project(story: Story<V, A, E>) = Unit

        override fun findStory(
            id: Pair<String, Int>,
            receiveChannel: SendChannel<Story<*, *, *>?>
        ) = Unit
    }

    sealed class Action {
        data class Incr(val count: Int = 1) : Action()
        data class Decr(val count: Int = 1) : Action()
    }

    data class Count(val count: Int, val number: Int) {
        operator fun plus(add: Int) = copy(count = count + add, number = number + 1)
        operator fun minus(sub: Int) = copy(count = count - sub, number = number + 1)
    }

    @Test
    fun main() {
        val story = storyOf<Count, Action, Void>(MyEdge, "test") {
            take(Count::class.java, Action.Incr::class.java) {
                give(vision + action.count)
            }
            take(Count::class.java, Action.Decr::class.java) {
                give(vision - action.count)
            }
            Count(5, 1)
        }
        with(story) {
            offer(Action.Incr())
            offer(Action.Incr(4))
            offer(Action.Decr())
        }
        val count = runBlocking {
            var count = 0
            for (vision in story.visions()) {
                if (vision.number == 4) {
                    count = vision.count
                    break
                }
            }
            count
        }
        assertEquals(9, count)
    }
}