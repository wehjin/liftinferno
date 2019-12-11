package com.rubyhuntersky.liftlog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.rubyhuntersky.liftlog.Dialog.*
import com.rubyhuntersky.liftlog.vision.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.concurrent.TimeUnit

class Dialog {
    enum class Side { LEFT, RIGHT }
    enum class BubbleType { SOLO, TOP, MIDDLE, BOTTOM; }

    sealed class Part {
        data class Timestamp(val date: Date) : Part()
        data class Speaker(val name: String, val side: Side) : Part()
        data class Bubble(val text: String, val side: Side, val type: BubbleType) : Part()
        object Guard : Part()
    }
}

class MainActivity : AppCompatActivity() {

    private val days = fetchDays()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView.layoutManager = LinearLayoutManager(this).apply { reverseLayout = true }
    }

    override fun onStart() {
        super.onStart()
        val infernoParts = listOf(
            Part.Speaker("Inferno", Side.LEFT),
            Part.Bubble("Squats: Rest 34s", Side.LEFT, BubbleType.SOLO)
        )
        val dialogParts = listOf(Part.Guard) +
                infernoParts.reversed() +
                days.sortedByDescending { it.startEpoch }.flatMap {
                    it.dialogParts().reversed()
                } +
                Part.Guard
        recyclerView.adapter =
            DialogPartsAdapter(dialogParts)
    }

    private fun fetchDays(): List<LogDay> {
        val now = Date()
        val tenMinutesAgo = now.time - TimeUnit.MINUTES.toMillis(10)
        return listOf(
            LogDay(
                rounds = listOf(
                    Round(
                        epoch = tenMinutesAgo,
                        movements = listOf(
                            Movement(
                                direction = Direction.PullUps,
                                force = Force.Lbs(110),
                                distance = Distance.Reps(6)
                            ),
                            Movement(
                                direction = Direction.Squats,
                                force = Force.Lbs(110),
                                distance = Distance.Reps(6)
                            ),
                            Movement(
                                direction = Direction.Dips,
                                force = Force.Lbs(110),
                                distance = Distance.Reps(6)
                            )
                        )
                    ),
                    Round(
                        epoch = tenMinutesAgo,
                        movements = listOf(
                            Movement(
                                direction = Direction.PullUps,
                                force = Force.Lbs(110),
                                distance = Distance.Reps(6)
                            ),
                            Movement(
                                direction = Direction.Squats,
                                force = Force.Lbs(110),
                                distance = Distance.Reps(6)
                            ),
                            Movement(
                                direction = Direction.Dips,
                                force = Force.Lbs(110),
                                distance = Distance.Reps(6)
                            )
                        )
                    )
                )
            )
        )
    }

    private fun LogDay.dialogParts(): List<Part> {
        val initial = listOf(Part.Timestamp(Date(startEpoch)) as Part)
        return rounds.foldIndexed(initial) { i, sum, round ->
            sum + round.dialogParts(i)
        }
    }

    private fun Round.dialogParts(index: Int): List<Part> {
        val speaker = Part.Speaker("Round ${index + 1}", Side.RIGHT) as Part
        return listOf(speaker) + movements.mapIndexed { i, movement ->
            movement.dialogParts(i, movements.lastIndex)
        }
    }

    private fun Movement.dialogParts(i: Int, last: Int): Part {
        val type = when {
            last <= 0 -> BubbleType.SOLO
            i == 0 -> BubbleType.TOP
            i == last -> BubbleType.BOTTOM
            else -> BubbleType.MIDDLE
        }
        return Part.Bubble(this.toString(), Side.RIGHT, type)
    }

}
