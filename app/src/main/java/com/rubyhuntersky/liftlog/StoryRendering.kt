package com.rubyhuntersky.liftlog

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.rubyhuntersky.liftlog.story.Story
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
fun <V : Any, A> renderStory(
    story: Story<V, A>,
    lifecycle: Lifecycle,
    renderVision: (vision: V, post: (A) -> Unit) -> Unit
) {
    lifecycle.addObserver(object : LifecycleObserver {
        private lateinit var renderJob: Job

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun startProjection() {
            renderJob = MainScope().launch {
                story.subscribe().consumeEach { renderVision(it, story::offer) }
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun stopProjection() = renderJob.cancel()
    })
}
