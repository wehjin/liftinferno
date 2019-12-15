@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.rubyhuntersky.liftlog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.rubyhuntersky.liftlog.Chatter.*
import com.rubyhuntersky.liftlog.story.LogDay
import com.rubyhuntersky.liftlog.story.LoggingStory
import com.rubyhuntersky.liftlog.story.Movement
import com.rubyhuntersky.liftlog.story.Round
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.time.ExperimentalTime

class Chatter {
    enum class Side { LEFT, RIGHT }
    enum class BubbleType { SOLO, TOP, MIDDLE, BOTTOM; }

    sealed class Part {
        data class Timestamp(val date: Date, val now: Date) : Part()
        data class Speaker(val name: String, val side: Side) : Part()
        data class Bubble(val text: String, val side: Side, val type: BubbleType) : Part()
        object Guard : Part()
    }
}

@ExperimentalTime
private val loggingStory = LoggingStory(MainEdge)

@ExperimentalTime
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainEdge.activeFragmentManager = this.supportFragmentManager
        setContentView(R.layout.activity_main)
        recyclerView.layoutManager = LinearLayoutManager(this).apply { reverseLayout = true }
        renderStory(lifecycle, loggingStory, this::renderVision)
    }

    private fun renderVision(vision: LoggingStory.Vision, post: (Any) -> Unit) {
        require(vision is LoggingStory.Vision.Loaded)
        recyclerView.adapter = ChatterPartsAdapter(dialogParts(vision))
        movementButton.setOnClickListener {
            post(vision.addAction())
        }
    }

    private fun dialogParts(vision: LoggingStory.Vision.Loaded): List<Part> {
        val now = Date()
        val visionParts = vision.history.logDays
            .sortedByDescending { it.startTime }
            .flatMap { dialogParts(it, now).reversed() }


        val infernoParts = listOf(
            Part.Speaker("Inferno", Side.LEFT),
            Part.Bubble("Squats: Rest 34s", Side.LEFT, BubbleType.SOLO)
        ).reversed()
        val infernoTitle =
            if (visionParts.isEmpty()) listOf(Part.Timestamp(now, now)) else emptyList<Part>()

        return listOf(Part.Guard) + infernoParts + infernoTitle + visionParts + Part.Guard
    }


    private fun dialogParts(logDay: LogDay, now: Date): List<Part> {
        val startTime = logDay.startTime
        val initial = listOf(Part.Timestamp(Date(startTime), now) as Part)
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
}
