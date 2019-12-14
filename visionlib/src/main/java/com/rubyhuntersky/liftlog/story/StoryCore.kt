@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.math.absoluteValue
import kotlin.random.Random

interface Story<out V : Any, in A, out E : Any> {
    val name: String
    fun visions(): ReceiveChannel<V>
    fun offer(action: A)
    fun ending(): ReceiveChannel<E?>
}

interface Edge {
    val well: WishWell

    fun <V : Any, A : Any, E : Any> project(story: Story<V, A, E>)
    fun findStory(id: Pair<String, Int>, receiveChannel: SendChannel<Story<*, *, *>?>)
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

interface RevisionScope<V1 : Any, A1 : Any> {
    val vision: V1
    val action: A1
}

@Suppress("unused")
fun <V : Any, V1 : V, A1 : Any> RevisionScope<V1, A1>.give(nextVision: V): Revision<V> {
    return Revision(nextVision)
}

@Suppress("unused")
fun <V : Any, V1 : V, A1 : Any> RevisionScope<V1, A1>.give(
    vision: V,
    vararg wish: Wish
): Revision<V> = Revision(vision, wish.toList())

class StoryBeat<V : Any, V1 : V, A1 : Any>(
    val visionClass: Class<V1>,
    val actionClass: Class<A1>,
    val revise: RevisionScope<V1, A1>.() -> Revision<V>
) {
    fun <A : Any> produce(vision: V, action: A): Revision<V> {
        val scope = object : RevisionScope<V1, A1> {
            override val vision: V1 by lazy { visionClass.cast(vision) }
            override val action: A1 by lazy { actionClass.cast(action) }
        }
        return revise(scope)
    }
}

interface StoryScope<V : Any, A : Any> {
    val beats: MutableList<StoryBeat<V, *, *>>
    var ending: ((V) -> StoryEnding)?
}

@Suppress("unused")
fun <V : Any, A : Any, V1 : V, A1 : A> StoryScope<V, A>.take(
    visionClass: Class<V1>,
    actionClass: Class<A1>,
    revise: RevisionScope<V1, A1>.() -> Revision<V>
) {
    beats.add(StoryBeat(visionClass, actionClass, revise))
}

inline fun <V : Any, reified A : Any, reified E : Any> storyOf(
    edge: Edge,
    name: String,
    noinline init: StoryScope<V, A>.() -> V
): Story<V, A, E> {
    val scope = (object : StoryScope<V, A> {
        override val beats = mutableListOf<StoryBeat<V, *, *>>()
        override var ending: ((V) -> StoryEnding)? = null
    })
    val start = init(scope)
    val end = scope.ending?.let { it } ?: StoryEnding::None
    val beats = scope.beats.associateBy { it.visionClass to it.actionClass }
    return storyOf(edge, name, start, end, { vision, action, _ ->
        val key = vision::class.java to action::class.java
        beats[key]?.produce(vision, action) ?: fallbackRevision(vision, action)
    })
}

inline fun <V : Any, reified A : Any, reified E : Any> storyOf(
    edge: Edge,
    name: String,
    start: V,
    noinline end: (V) -> StoryEnding,
    noinline revise: (V, A, Edge) -> Revision<V>
): Story<V, A, E> {
    val visions = ConflatedBroadcastChannel(start)
    val endings = ConflatedBroadcastChannel<E?>()
    val actions = Channel<A>(10)
    GlobalScope.launch {
        println("Story/$name launched")
        actions.consumeEach { action ->
            revise(visions.value, action, edge).also { revision ->
                visions.send(revision.vision)
                revision.wishes.forEach { wish ->
                    edge.well.addWill(when (wish) {
                        is Wish.Forget -> Will.Forget(wish)
                        is Wish.Fetch<*, *, *> -> Will.Fetch(wish) {
                            actions.offer(it as A)
                        }
                        is Wish.Tell<*, *, *> -> Will.Tell(wish, edge) {
                            actions.offer(it as A)
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
    return object : Story<V, A, E> {
        override val name = name

        override fun visions(): ReceiveChannel<V> = visions.openSubscription()

        override fun offer(action: A) {
            actions.offer(action)
        }

        override fun ending(): ReceiveChannel<E?> = endings.openSubscription()
    }
}

data class WishId(val name: String, val number: Int) {
    override fun toString() = "$name-${number.toString(16)}"
}

interface Parameterized<P : Any> {
    val paramsClass: Class<P>
    val params: P
}

interface Actionable<R, A> {
    val resultClass: Class<R>
    val actionClass: Class<A>
    val resultToAction: (Result<R>) -> A
}

interface Fetcher<P : Any, R : Any> {
    val paramsClass: Class<P>
    val resultClass: Class<R>
    val paramsToResult: (P) -> R
}

sealed class WishService<P : Any, R : Any> {
    abstract val name: String
    abstract val paramsClass: Class<P>
    abstract val resultClass: Class<R>

    data class Fetch<P : Any, R : Any>(
        override val name: String,
        override val paramsClass: Class<P>,
        override val resultClass: Class<R>,
        override val paramsToResult: (P) -> R
    ) : WishService<P, R>(), Fetcher<P, R>
}

sealed class Wish {

    abstract val id: WishId

    data class Forget(
        override val id: WishId
    ) : Wish()

    data class Fetch<P : Any, R : Any, A : Any>(
        val service: WishService.Fetch<P, R>,
        val number: Int,
        override val params: P,
        override val actionClass: Class<A>,
        override val resultToAction: (Result<R>) -> A
    ) : Wish(), Parameterized<P>, Fetcher<P, R>, Actionable<R, A> {
        override val id by lazy { WishId(service.name, number) }
        override val paramsClass: Class<P> = service.paramsClass
        override val resultClass: Class<R> = service.resultClass
        override val paramsToResult = service.paramsToResult

        fun action(): A {
            val result = try {
                Result.success(paramsToResult.invoke(params))
            } catch (e: Throwable) {
                Result.failure<R>(e)
            }
            return resultToAction(result)
        }
    }

    data class Tell<V : Any, E : Any, A2 : Any>(
        val number: Int,
        override val paramsClass: Class<Story<V, *, E>>,
        override val params: Story<V, *, E>,
        override val resultClass: Class<E>,
        override val actionClass: Class<A2>,
        override val resultToAction: (Result<E>) -> A2
    ) : Wish(), Parameterized<Story<V, *, E>>, Actionable<E, A2> {
        override val id = WishId("story-${params.name}-ending", number)

        suspend fun action(): A2 {
            val result = try {
                val ending = params.ending().receive()
                if (ending == null) throw (Exception("Cancelled"))
                else Result.success(ending)
            } catch (e: Throwable) {
                Result.failure<E>(e)
            }
            return resultToAction(result)
        }
    }
}

inline fun <V : Any, reified E : Any, reified A2 : Any> Story<V, *, E>.toWish(
    noinline resultToAction: (Result<E>) -> A2
) = Wish.Tell(
    number = Random.nextInt().absoluteValue,
    paramsClass = javaClass,
    params = this,
    resultClass = E::class.java,
    actionClass = A2::class.java,
    resultToAction = resultToAction
)

sealed class Will<P : Any, R : Any, A : Any>(val wishId: WishId) {

    val name: String
        get() = wishId.name

    data class Forget(
        val wish: Wish.Forget
    ) : Will<Void, Void, Void>(wish.id)

    data class Fetch<P : Any, R : Any, A : Any>(
        val wish: Wish.Fetch<P, R, A>,
        val sendAction: (A) -> Unit
    ) : Will<P, R, A>(wish.id) {

        fun fulfill(checkSend: (A) -> Boolean) {
            wish.action().let {
                if (checkSend(it)) sendAction(it)
            }
        }
    }

    data class Tell<V : Any, E : Any, A2 : Any>(
        val wish: Wish.Tell<V, E, A2>,
        val edge: Edge,
        val sendAction: (A2) -> Unit
    ) : Will<V, E, A2>(wish.id) {

        suspend fun fulfill(checkSend: (A2) -> Boolean) {
            edge.project(wish.params)
            wish.action().let { if (checkSend(it)) sendAction(it) }
        }
    }
}

class WishWell : CoroutineScope by GlobalScope {
    private sealed class Msg {
        data class AddWill(val will: Will<*, *, *>) : Msg()
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
                        is Will.Fetch<*, *, *> -> {
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
                        is Will.Tell<*, *, *> -> {
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

    fun addWill(will: Will<*, *, *>) {
        msgs.offer(Msg.AddWill(will))
    }

    fun reports(): ReceiveChannel<Rpt> = rpts.openSubscription()

    fun close() = job.cancel()
}
