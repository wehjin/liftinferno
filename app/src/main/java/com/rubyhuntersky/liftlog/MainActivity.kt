package com.rubyhuntersky.liftlog

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.rubyhuntersky.liftlog.Dialog.*
import com.rubyhuntersky.liftlog.Dialog.Part.Bubble
import com.rubyhuntersky.liftlog.vision.*
import java.util.*
import java.util.concurrent.TimeUnit

class Dialog {
    enum class Side { LEFT, RIGHT }
    enum class BubbleType {
        SOLO, TOP, MIDDLE, BOTTOM;

        companion object {
            fun <T> fromListIndex(list: List<T>, i: Int): BubbleType =
                if (list.size <= 1) {
                    SOLO
                } else {
                    when (i) {
                        0 -> TOP
                        list.lastIndex -> BOTTOM
                        else -> MIDDLE
                    }
                }
        }
    }

    sealed class Part {
        data class Timestamp(val date: Date) : Part()
        data class Speaker(val name: String, val side: Side) : Part()
        data class Bubble(val text: String, val side: Side, val type: BubbleType) : Part()
        object Guard : Part()
    }
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val now = Date()
        val tenMinutesAgo = now.time - TimeUnit.MINUTES.toMillis(10)
        val days = listOf(
            LogDay(
                rounds = listOf(
                    Round(
                        epoch = tenMinutesAgo,
                        movements = listOf(
                            Movement(
                                direction = Direction.Squats,
                                force = Force.Lbs(110),
                                distance = Distance.Reps(6)
                            )
                        )
                    )
                )
            )
        )
        val dialogParts =
            days.sortedByDescending { it.startEpoch }.flatMap { it.dialogParts().reversed() }
        Log.d("LOGPARTS", dialogParts.toString())
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
        return Bubble(this.toString(), Side.RIGHT, type)
    }

}
