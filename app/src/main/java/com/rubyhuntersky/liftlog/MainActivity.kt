package com.rubyhuntersky.liftlog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.rubyhuntersky.liftlog.Chatter.*
import com.rubyhuntersky.liftlog.story.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.util.*

class Chatter {
    enum class Side { LEFT, RIGHT }
    enum class BubbleType { SOLO, TOP, MIDDLE, BOTTOM; }

    sealed class Part {
        data class Timestamp(val date: Date) : Part()
        data class Speaker(val name: String, val side: Side) : Part()
        data class Bubble(val text: String, val side: Side, val type: BubbleType) : Part()
        object Guard : Part()
    }
}

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView.layoutManager = LinearLayoutManager(this).apply { reverseLayout = true }
    }

    override fun onStart() {
        super.onStart()
        renderJob = MainScope().launch {
            val visions = loggingStory().openSubscription()
            visions.consumeEach(this@MainActivity::render)
        }
    }

    private fun render(vision: LoggingVision) {
        require(vision is LoggingVision.Logging)
        recyclerView.adapter = ChatterPartsAdapter(dialogParts(vision))
    }

    private fun dialogParts(vision: LoggingVision.Logging): List<Part> {

        val visionParts = vision.days
            .sortedByDescending { it.startEpoch }
            .flatMap { dialogParts(it).reversed() }

        val infernoParts = listOf(
            Part.Speaker("Inferno", Side.LEFT),
            Part.Bubble("Squats: Rest 34s", Side.LEFT, BubbleType.SOLO)
        ).reversed()

        return listOf(Part.Guard) + infernoParts + visionParts + Part.Guard
    }


    private fun dialogParts(logDay: LogDay): List<Part> {
        val initial = listOf(Part.Timestamp(Date(logDay.startEpoch)) as Part)
        return logDay.rounds.foldIndexed(initial) { i, sum, round ->
            sum + dialogParts(round, i)
        }
    }

    private fun dialogParts(round: Round, index: Int): List<Part> {
        val speaker = Part.Speaker("Round ${index + 1}", Side.RIGHT) as Part
        return listOf(speaker) + round.movements.mapIndexed { i, movement ->
            dialogParts(movement, i, round.movements.lastIndex)
        }
    }

    private fun dialogParts(movement: Movement, i: Int, last: Int): Part {
        val type = when {
            last <= 0 -> BubbleType.SOLO
            i == 0 -> BubbleType.TOP
            i == last -> BubbleType.BOTTOM
            else -> BubbleType.MIDDLE
        }
        return Part.Bubble(movement.toString(), Side.RIGHT, type)
    }

    private lateinit var renderJob: Job

    override fun onStop() {
        renderJob.cancel()
        super.onStop()
    }
}
