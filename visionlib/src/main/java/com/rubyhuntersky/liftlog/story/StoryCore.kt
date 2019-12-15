@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.math.absoluteValue
import kotlin.random.Random

interface Story<out V : Any, out E : Any> {
    val name: String
    fun visions(): ReceiveChannel<V>
    fun ending(): ReceiveChannel<E?>
    fun offer(action: Any)
    fun cancelAction(): Any?
}

@Suppress("unused")
fun <V : Any, E : Any> Story<V, E>.cancel() {
    cancelAction()?.let { offer(it) }
}

interface Edge {
    val well: WishWell

    fun <V : Any, E : Any> project(story: Story<V, E>)
    fun findStory(id: Pair<String, Int>, receiveChannel: SendChannel<Story<*, *>?>)
}

data class Revision<out V : Any>(
    val vision: V,
    val wishes: List<Wish> = emptyList()
)

fun <V : Any, A : Any> fallbackRevision(vision: V, action: A): Revision<V> {
    println("No revision case for $action x $vision")
    return Revision(vision)
}

sealed class StoryEnding {
    data class Ended<E : Any>(val value: E) : StoryEnding()
    data class Cancelled(private val ignore: Any = Unit) : StoryEnding()
    data class None(private val ignore: Any = Unit) : StoryEnding()
}

fun <T : Any> storyEnding(value: T?): StoryEnding {
    return value?.let { StoryEnding.Ended(it) } ?: StoryEnding.Cancelled()
}

fun storyEndingNone() = StoryEnding.None(Unit)

interface RevisionScope<out V : Any, A : Any> {
    val vision: V
    val action: A
}

@Suppress("unused")
fun <V : Any, V1 : V, A : Any> RevisionScope<V1, A>.give(nextVision: V) = Revision(nextVision)

@Suppress("unused")
fun <V : Any, V1 : V, A : Any> RevisionScope<V1, A>.give(
    vision: V, vararg wish: Wish
) = Revision(vision, wish.toList())

class StoryBeat<V : Any, V1 : V, A : Any>(
    val visionClass: Class<V1>,
    val actionClass: Class<A>,
    val revise: RevisionScope<V1, A>.() -> Revision<V>
) {
    fun produce(vision: V, action: Any): Revision<V> {
        val scope = object : RevisionScope<V1, A> {
            override val vision: V1 by lazy { visionClass.cast(vision) }
            override val action: A by lazy { actionClass.cast(action) }
        }
        return revise(scope)
    }
}

interface StoryScope<V : Any> {
    val beats: MutableList<StoryBeat<V, *, *>>
    var ending: (V) -> StoryEnding
    var cancel: () -> Any?
}

@Suppress("unused")
fun <V : Any, V1 : V, A : Any> StoryScope<V>.on(
    visionClass: Class<V1>,
    actionClass: Class<A>,
    revise: RevisionScope<V1, A>.() -> Revision<V>
) {
    beats.add(StoryBeat(visionClass, actionClass, revise))
}

inline fun <V : Any, reified E : Any> storyOf(
    edge: Edge,
    name: String,
    noinline init: StoryScope<V>.() -> V
): Story<V, E> {
    val scope = (object : StoryScope<V> {
        override val beats = mutableListOf<StoryBeat<V, *, *>>()
        override var ending: (V) -> StoryEnding = { StoryEnding.None() }
        override var cancel: () -> Any? = { null }
    })
    val start = init(scope)
    val end = scope.ending
    val beats = scope.beats.associateBy { it.visionClass to it.actionClass }
    return storyOf(edge, name, start, end,
        cancel = { scope.cancel() },
        revise = { vision, action, _ ->
            val key = vision::class.java to action::class.java
            val beat = beats[key]
            beat?.produce(vision, action) ?: fallbackRevision(vision, action)
        })
}

inline fun <V : Any, reified E : Any> storyOf(
    edge: Edge,
    name: String,
    start: V,
    noinline end: (V) -> StoryEnding,
    noinline cancel: () -> Any?,
    noinline revise: (V, Any, Edge) -> Revision<V>
): Story<V, E> {
    val visions = ConflatedBroadcastChannel(start)
    val endings = ConflatedBroadcastChannel<E?>()
    val actions = Channel<Any>(10)
    GlobalScope.launch {
        println("Story/$name launched")
        actions.consumeEach { action ->
            revise(visions.value, action, edge).also { revision ->
                visions.send(revision.vision)
                revision.wishes.forEach { wish ->
                    edge.well.addWill(when (wish) {
                        is Wish.Forget -> Will.Forget(wish)
                        is Wish.Fetch<*, *> -> Will.Fetch(wish) {
                            actions.offer(it)
                        }
                        is Wish.Tell<*, *> -> Will.Tell(wish, edge) {
                            actions.offer(it)
                        }
                    })
                }
                when (val ending = end(revision.vision)) {
                    is StoryEnding.None -> Unit
                    is StoryEnding.Cancelled -> endings.send(null)
                    is StoryEnding.Ended<*> -> endings.send(ending.value as E)
                }
            }
            println("Story/$name vision: ${visions.value}")
        }
    }
    return object : Story<V, E> {
        override val name = name

        override fun visions(): ReceiveChannel<V> = visions.openSubscription()

        override fun ending(): ReceiveChannel<E?> = endings.openSubscription()

        override fun offer(action: Any) {
            actions.offer(action)
        }

        override fun cancelAction(): Any? {
            return cancel()
        }
    }
}

data class WishId(val name: String, val number: Int) {
    override fun toString() = "$name-${number.toString(16)}"
}

interface Parameterized<P : Any> {
    val paramsClass: Class<P>
    val params: P
}

interface Fetcher<P : Any, R : Any> {
    val paramsClass: Class<P>
    val resultClass: Class<R>
    val resultOnParams: (P) -> R
}

sealed class WishService<P : Any, R : Any> {
    abstract val name: String
    abstract val paramsClass: Class<P>
    abstract val resultClass: Class<R>

    data class Fetch<P : Any, R : Any>(
        override val name: String,
        override val paramsClass: Class<P>,
        override val resultClass: Class<R>,
        override val resultOnParams: (P) -> R
    ) : WishService<P, R>(), Fetcher<P, R>
}

sealed class Wish {

    abstract val id: WishId

    data class Forget(
        override val id: WishId
    ) : Wish()

    data class Fetch<P : Any, R : Any>(
        val service: WishService.Fetch<P, R>,
        val number: Int,
        override val params: P,
        val actionOnResult: (Result<R>) -> Any
    ) : Wish(), Parameterized<P>, Fetcher<P, R> {
        override val id by lazy { WishId(service.name, number) }
        override val paramsClass: Class<P> = service.paramsClass
        override val resultClass: Class<R> = service.resultClass
        override val resultOnParams = service.resultOnParams

        fun action(): Any {
            val result = try {
                Result.success(resultOnParams.invoke(params))
            } catch (e: Throwable) {
                Result.failure<R>(e)
            }
            return actionOnResult(result)
        }
    }

    data class Tell<V : Any, E : Any>(
        val number: Int,
        override val paramsClass: Class<Story<V, E>>,
        override val params: Story<V, E>,
        val resultClass: Class<E>,
        val actionOnSuccess: (E) -> Any,
        val actionOnFailure: (Throwable) -> Any
    ) : Wish(), Parameterized<Story<V, E>> {
        override val id = WishId("story-${params.name}-ending", number)

        suspend fun action(): Any =
            try {
                val ending = params.ending().receive()
                if (ending == null) throw (Exception("Cancelled"))
                else actionOnSuccess(ending)
            } catch (e: Throwable) {
                actionOnFailure(e)
            }
    }
}


inline fun <V : Any, reified E : Any> Story<V, E>.toWish(
    noinline onSuccess: (E) -> Any,
    noinline onFailure: (Throwable) -> Any
): Wish.Tell<V, E> {
    return Wish.Tell(
        number = Random.nextInt().absoluteValue,
        paramsClass = javaClass,
        params = this,
        resultClass = E::class.java,
        actionOnSuccess = onSuccess,
        actionOnFailure = onFailure
    )
}

sealed class Will<P : Any, R : Any>(val wishId: WishId) {

    val name: String
        get() = wishId.name

    data class Forget(
        val wish: Wish.Forget
    ) : Will<Void, Void>(wish.id)

    data class Fetch<P : Any, R : Any>(
        val wish: Wish.Fetch<P, R>,
        val sendAction: (Any) -> Unit
    ) : Will<P, R>(wish.id) {

        fun fulfill(checkSend: (Any) -> Boolean) {
            wish.action().let {
                if (checkSend(it)) sendAction(it)
            }
        }
    }

    data class Tell<V : Any, E : Any>(
        val wish: Wish.Tell<V, E>,
        val edge: Edge,
        val sendAction: (Any) -> Unit
    ) : Will<V, E>(wish.id) {

        suspend fun fulfill(checkSend: (Any) -> Boolean) {
            edge.project(wish.params)
            wish.action().let { if (checkSend(it)) sendAction(it) }
        }
    }
}

class WishWell : CoroutineScope by GlobalScope {
    private sealed class Msg {
        data class AddWill(val will: Will<*, *>) : Msg()
        data class DropWill(val wishId: WishId) : Msg()
    }

    sealed class Rpt {
        data class DroppedWish(val wishId: WishId) : Rpt()
    }

    private val msgs = Channel<Msg>(10)
    private val rpts = BroadcastChannel<Rpt>(10)

    private val job = launch {
        val jobs = mutableMapOf<WishId, Job>()
        for (msg in msgs) {
            when (msg) {
                is Msg.AddWill -> {
                    val will = msg.will
                    jobs.remove(will.wishId)?.apply { cancel() }
                    when (will) {
                        is Will.Forget -> Unit
                        is Will.Fetch<*, *> -> {
                            val ready = Channel<Unit>(1)
                            jobs[will.wishId] = launch {
                                ready.receive()
                                will.fulfill {
                                    if (this.isActive) {
                                        msgs.sendBlocking(Msg.DropWill(will.wishId))
                                        true
                                    } else false
                                }
                            }
                            ready.send(Unit)
                        }
                        is Will.Tell<*, *> -> {
                            val ready = Channel<Unit>(1)
                            jobs[will.wishId] = launch {
                                ready.receive()
                                will.fulfill {
                                    if (this.isActive) {
                                        msgs.sendBlocking(Msg.DropWill(will.wishId))
                                        true
                                    } else false
                                }
                            }
                            ready.send(Unit)
                        }
                    }
                }
                is Msg.DropWill -> {
                    jobs.remove(msg.wishId)
                    rpts.send(Rpt.DroppedWish(msg.wishId))
                }
            }
        }
    }

    fun addWill(will: Will<*, *>) {
        msgs.offer(Msg.AddWill(will))
    }

    fun reports(): ReceiveChannel<Rpt> = rpts.openSubscription()

    fun close() = job.cancel()
}
