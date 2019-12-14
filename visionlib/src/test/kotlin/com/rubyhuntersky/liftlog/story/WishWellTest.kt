package com.rubyhuntersky.liftlog.story

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URL
import kotlin.math.absoluteValue
import kotlin.random.Random

internal class WishWellTest {

    private val urlToTextService = WishService.Fetch(
        name = "url-text",
        paramsClass = URL::class.java,
        resultClass = String::class.java,
        paramsToResult = { it.toString() }
    )

    private inline fun <reified A : Any> urlTextWish(
        url: URL,
        number: Int = Random.nextInt().absoluteValue,
        noinline textToAction: (Result<String>) -> A
    ) = Wish.Fetch(
        service = urlToTextService,
        number = number,
        params = url,
        actionClass = A::class.java,
        resultToAction = textToAction
    )

    @ExperimentalCoroutinesApi
    @Test
    fun main() {
        val urlString = "https://example.com/"
        val well = WishWell()
        runBlocking {
            val reporting = launch(start = CoroutineStart.UNDISPATCHED) {
                val report = well.reports().receive()
                assertTrue(report is WishWell.Rpt.DroppedWish)
            }
            val wish = urlTextWish(URL(urlString)) { result ->
                result.map { it.length }.getOrElse { 0 }
            }
            val will = Will.Fetch(wish) {
                assertEquals(urlString.length, it)
            }
            well.addWill(will)
            reporting.join()
        }
        well.close()
    }
}