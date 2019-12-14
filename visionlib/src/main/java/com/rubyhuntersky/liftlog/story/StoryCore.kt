package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

interface Story<out V : Any, in A> {
    val name: String
    fun subscribe(): ReceiveChannel<V>
    fun offer(action: A)
}

interface Edge {
    @ExperimentalCoroutinesApi
    val well: WishWell

    fun <V : Any, A> project(story: Story<V, A>, isEnd: (Any) -> Boolean)
    fun findStory(id: Pair<String, Int>, receiveChannel: SendChannel<Story<*, *>?>)
}

@ExperimentalCoroutinesApi
data class Revision<out V : Any>(
    val vision: V,
    val wishes: List<Wish> = emptyList()
)

@ExperimentalCoroutinesApi
fun <V : Any> revision(vision: V) = Revision(vision)

@ExperimentalCoroutinesApi
fun <V : Any, A : Any> fallbackRevision(vision: V, action: A): Revision<V> {
    println("No revision case for $action x $vision")
    return Revision(vision)
}


@ExperimentalCoroutinesApi
inline fun <V : Any, reified A : Any> storyOf(
    name: String,
    initial: V,
    noinline revise: (V, A, Edge) -> Revision<V>,
    edge: Edge
): Story<V, A> {
    val visions = ConflatedBroadcastChannel(initial)
    val actions = Channel<A>(10)
    GlobalScope.launch {
        println("Story/$name launched")
        actions.consumeEach { action ->
            revise(visions.value, action, edge).also { revision ->
                visions.send(revision.vision)
                revision.wishes.forEach { wish ->
                    edge.well.addWill(when (wish) {
                        is Wish.Forget -> Will.Forget(wish)
                        is Wish.Fetch<*, *, *> -> Will.Fetch(wish) { actions.offer(it as A) }
                    })
                }
            }
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
}

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

        fun fulfill(checkSend: ((A) -> Boolean)) {
            wish.action().let {
                if (checkSend(it)) {
                    sendAction(it)
                }
            }
        }
    }
}


@ExperimentalCoroutinesApi
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
