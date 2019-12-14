package com.rubyhuntersky.liftlog

import android.util.Log
import androidx.fragment.app.FragmentManager
import com.rubyhuntersky.liftlog.story.Edge
import com.rubyhuntersky.liftlog.story.Story
import com.rubyhuntersky.liftlog.story.WishWell
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlin.random.Random

@FlowPreview
@UseExperimental(InternalCoroutinesApi::class)
@ExperimentalCoroutinesApi
object MainEdge : Edge {

    override val well = WishWell()

    var activeFragmentManager: FragmentManager? = null

    sealed class Msg {

        data class HoldStory<V : Any, A>(
            val id: Pair<String, Int>,
            val story: Story<V, A>,
            val isEnd: (Any) -> Boolean
        ) : Msg()

        data class DropStory(val id: Pair<String, Int>) : Msg()

        data class FindStory(
            val id: Pair<String, Int>,
            val result: SendChannel<Story<*, *>?>
        ) : Msg()
    }

    private val msgs = Channel<Msg>(10)

    init {
        GlobalScope.launch {
            val stories = mutableMapOf<Pair<String, Int>, Story<*, *>>()
            msgs.consumeEach { msg ->
                when (msg) {
                    is Msg.HoldStory<*, *> -> {
                        Log.d("MainEdge", "Holding Story/${msg.id}")
                        stories[msg.id] = msg.story
                        launch {
                            Log.d(
                                "MainEdge",
                                "Observing Story/${msg.id}"
                            )
                            msg.story.subscribe().consumeAsFlow()
                                .filter { msg.isEnd(it) }
                                .collect { msgs.send(Msg.DropStory(msg.id)) }
                        }
                    }
                    is Msg.DropStory -> {
                        stories.remove(msg.id)
                        Log.d("MainEdge", "Dropped Story/${msg.id}")
                    }
                    is Msg.FindStory -> {
                        val story = stories[msg.id]
                        msg.result.send(story)
                    }
                }
            }
        }
    }


    override fun <V : Any, A> project(story: Story<V, A>, isEnd: (Any) -> Boolean) {
        when (story.name) {
            "add-movement" -> {
                val id = Pair(story.name, Random.nextInt())
                msgs.offer(Msg.HoldStory(id, story, isEnd))
                activeFragmentManager?.let { fragmentManager ->
                    MovementDialogFragment()
                        .apply { storyId = id }.show(fragmentManager, "add-movement")
                }
            }
        }
    }

    override fun findStory(id: Pair<String, Int>, receiveChannel: SendChannel<Story<*, *>?>) {
        msgs.offer(Msg.FindStory(id, receiveChannel))
    }
}