package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.lang.Thread.sleep
import java.net.URL
import kotlin.random.Random

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

interface WishParams

data class WishId(val name: String, val number: Int)

interface Parameterized<P> {
    val params: P
    val paramsClass: Class<P>
}

interface Actionable<T, A> {
    val resultClass: Class<T>
    val toAction: (Result<T>) -> A
    val actionClass: Class<A>
}

sealed class Wish<T> {
    abstract val id: WishId

    data class Forget(override val id: WishId) : Wish<Void>()

    data class Fetch<P : Any, T, A>(
        override val id: WishId,
        override val params: P,
        override val paramsClass: Class<P>,
        override val resultClass: Class<T>,
        override val toAction: (Result<T>) -> A,
        override val actionClass: Class<A>
    ) : Wish<T>(), Parameterized<P>, Actionable<T, A> {

        lateinit var sendAction: (A) -> Unit

        fun sendSuccess(value: Any) = sendResult(Result.success(resultClass.cast(value)))
        fun sendFailure(e: Throwable) = sendResult(Result.failure(e))

        private fun sendResult(result: Result<T>) = sendAction(toAction(result))
    }
}

sealed class WishService<P, T> {
    abstract val name: String
    abstract val paramsClass: Class<P>
    abstract val resultClass: Class<T>
    abstract val perform: (P) -> T

    data class Fetch<P : Any, T : Any>(
        override val name: String,
        override val paramsClass: Class<P>,
        override val resultClass: Class<T>,
        override val perform: (P) -> T
    ) : WishService<P, T>() {

        fun performOnAny(params: Any): T = perform(paramsClass.cast(params))
    }
}

class WishWell {

    private sealed class Msg {
        data class AddService(val service: WishService<*, *>) : Msg()
        data class AddWish(val wish: Wish<*>) : Msg()
        data class DropJob(val wishId: WishId) : Msg()
    }

    private val msgChannel = Channel<Msg>(10)

    @ExperimentalCoroutinesApi
    private val msgJob = GlobalScope.launch {
        val services = mutableMapOf<String, WishService<*, *>>()
        val jobs = mutableMapOf<WishId, Job>()
        msgChannel.consumeEach { msg ->
            when (msg) {
                is Msg.AddService -> {
                    services[msg.service.name] = msg.service
                }
                is Msg.AddWish -> {
                    val (wish) = msg
                    jobs.remove(wish.id)?.apply { cancel() }
                    when (wish) {
                        is Wish.Forget -> Unit
                        is Wish.Fetch<*, *, *> -> {
                            services[wish.id.name]?.let { service ->
                                require(service is WishService.Fetch<*, *>)
                                jobs[wish.id] = startJob(service, wish)
                            }
                        }
                    }
                }
                is Msg.DropJob -> {
                    jobs.remove(msg.wishId)
                }
            }
        }
    }

    private fun startJob(
        service: WishService.Fetch<*, *>,
        wish: Wish.Fetch<*, *, *>
    ): Job = GlobalScope.launch {

        // TODO apply service to wish.
        val result = try {
            Result.success(service.performOnAny(wish.params))
        } catch (e: Throwable) {
            Result.failure<Any>(e)
        }
        if (isActive) {
            msgChannel.sendBlocking(Msg.DropJob(wish.id))
            result.fold(wish::sendSuccess, wish::sendFailure)
        }
    }

    fun add(service: WishService<*, *>) {
        msgChannel.offer(Msg.AddService(service))
    }

    fun <T> add(wish: Wish<T>) {
        msgChannel.offer(Msg.AddWish(wish))
    }

}

val httpTextService = WishService.Fetch(
    name = "http-text",
    paramsClass = URL::class.java,
    resultClass = String::class.java,
    perform = {
        sleep(3)
        it.toString()
    }
)

inline fun <reified A> wishForText(
    url: URL,
    number: Int = Random.nextInt(),
    noinline action: (Result<String>) -> A
): Wish.Fetch<URL, String, A> = Wish.Fetch(
    id = WishId(httpTextService.name, number),
    params = url,
    paramsClass = URL::class.java,
    resultClass = String::class.java,
    toAction = action,
    actionClass = A::class.java
)


fun testma() {

    val well = WishWell().apply { add(httpTextService) }
    val wish = wishForText(URL("https://example.com/")) { it.map { 1 }.getOrElse { 0 } }
        .apply { sendAction = { println("testma: $it") } }
    well.add(wish)
}

