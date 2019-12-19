@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.rubyhuntersky.liftlog

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.rubyhuntersky.liftlog.Chatter.*
import com.rubyhuntersky.liftlog.story.LogDay
import com.rubyhuntersky.liftlog.story.LoggingStory
import com.rubyhuntersky.liftlog.story.Movement
import com.rubyhuntersky.liftlog.story.Round
import com.rubyhuntersky.tomedb.Owner
import com.rubyhuntersky.tomedb.Tomic
import com.rubyhuntersky.tomedb.tomicOf
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
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

object Main {
    private lateinit var tomic: Tomic

    fun startTome(context: Context): Tomic {
        if (!::tomic.isInitialized) {
            val tomeDir = File(context.filesDir, "tome1")
            tomic = tomicOf(tomeDir) { emptyList() }
        }
        return tomic
    }
}

@ExperimentalTime
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainEdge.activeFragmentManager = this.supportFragmentManager
        setContentView(R.layout.activity_main)
        recyclerView.layoutManager = LinearLayoutManager(this).apply { reverseLayout = true }

        val tomic = Main.startTome(this)
        val loggingStory = LoggingStory(MainEdge, tomic)
        renderStory(lifecycle, loggingStory, this::renderVision)
    }

    private fun renderVision(vision: LoggingStory.Vision, post: (Any) -> Unit) {
        require(vision is LoggingStory.Vision.Loaded)
        recyclerView.adapter = ChatterPartsAdapter(vision.asParts())
        movementButton.setOnClickListener {
            post(vision.addAction())
        }
    }
}


@ExperimentalTime
private fun LoggingStory.Vision.Loaded.asParts(): List<Part> {
    val now = Date()

    val visionParts = this.history.logDays
        .sortedByDescending { it.startTime }
        .flatMap { it.toParts(now).reversed() }

    val infernoParts = listOf(
        Part.Speaker("Inferno", Side.LEFT),
        Part.Bubble("Squats: Rest 34s", Side.LEFT, BubbleType.SOLO)
    ).reversed()
    val infernoTitle =
        if (visionParts.isEmpty()) listOf(Part.Timestamp(now, now)) else emptyList<Part>()

    return listOf(Part.Guard) + infernoParts + infernoTitle + visionParts + Part.Guard
}

private fun LogDay.toParts(now: Date): List<Part> {
    val startTime = startTime
    val initial = listOf(Part.Timestamp(Date(startTime), now) as Part)
    return rounds.foldIndexed(initial) { i, sum, round ->
        sum + round.toParts(i)
    }
}


private fun Round.toParts(index: Int): List<Part> {
    val speaker = Part.Speaker("Round ${index + 1}", Side.RIGHT) as Part
    return listOf(speaker) + movements.mapIndexed { i, movement ->
        movement.toParts(i, movements.lastIndex)
    }
}

private fun Owner<Date>.toParts(i: Int, last: Int): Part {
    val type = when {
        last <= 0 -> BubbleType.SOLO
        i == 0 -> BubbleType.TOP
        i == last -> BubbleType.BOTTOM
        else -> BubbleType.MIDDLE
    }
    val text = this.movementText()
    return Part.Bubble(text, Side.RIGHT, type)
}

private fun Owner<Date>.movementText(): String {
    val direction = this[Movement.DIRECTION]!!
    val force = this[Movement.FORCE]
    val distance = this[Movement.DISTANCE]
    return "${direction.name} $force × $distance"
}
