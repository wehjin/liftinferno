package com.rubyhuntersky.liftlog

import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.rubyhuntersky.liftlog.story.Edge
import com.rubyhuntersky.liftlog.story.Story
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
fun <V : Any, A> renderStory(
    lifecycle: Lifecycle,
    story: Story<V, A>,
    renderVision: (vision: V, post: (A) -> Unit) -> Unit
) {
    lifecycle.addObserver(object : LifecycleObserver {
        private lateinit var rendering: Job

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun startProjection() {
            rendering = MainScope().launch {
                story.subscribe().consumeEach { renderVision(it, story::offer) }
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun stopProjection() = rendering.cancel()
    })
}

interface Rendering<V : Any, A> {
    val story: Story<V, A>?
}

@ExperimentalCoroutinesApi
fun <V : Any, A> renderStory(
    fragment: DialogFragment,
    storyId: Pair<String, Int>,
    edge: Edge,
    renderVision: (vision: V, post: (A) -> Unit) -> Unit
): Rendering<V, A> {
    var story: Story<V, A>? = null
    val channel = Channel<Boolean>(5)
    MainScope().launch {
        story = Channel<Story<*, *>?>()
            .also { edge.findStory(storyId, it) }
            .receive() as? Story<V, A>
        story?.let { story ->
            var renderJob: Job? = null
            channel.consumeEach { start ->
                if (start) {
                    renderJob?.cancel()
                    renderJob = launch {
                        story.subscribe().consumeEach {
                            renderVision(it, story::offer)
                        }
                    }
                } else {
                    renderJob?.cancel()
                }
            }
        } ?: fragment.dismiss()
    }
    fragment.lifecycle.addObserver(object : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun startProjection() {
            check(channel.offer(true))
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun stopProjection() {
            channel.offer(false)
        }
    })
    return object : Rendering<V, A> {
        override val story: Story<V, A>? get() = story
    }
}
