package com.rubyhuntersky.liftlog

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.dialog_bubble.view.*
import kotlinx.android.synthetic.main.dialog_speaker.view.*
import kotlinx.android.synthetic.main.dialog_timestamp.view.*
import java.util.*

sealed class ChatterPartViewHolder(partView: View) : RecyclerView.ViewHolder(partView) {

    abstract fun update(part: Chatter.Part)

    class Timestamp(parent: ViewGroup) :
        ChatterPartViewHolder(view(parent, R.layout.dialog_timestamp)) {

        override fun update(part: Chatter.Part) {
            require(part is Chatter.Part.Timestamp)
            val date = datePart(part.date.time)
            val today = datePart(part.now.time)
            itemView.timestampTextView.text =
                if (date == today) "Today"
                else DateUtils.getRelativeTimeSpanString(date, today, DateUtils.DAY_IN_MILLIS)
        }

        private fun datePart(minTime: Long): Long {
            return Calendar.getInstance().apply {
                time = Date(minTime)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time.time
        }
    }

    class Speaker(parent: ViewGroup) :
        ChatterPartViewHolder(view(parent, R.layout.dialog_speaker)) {
        override fun update(part: Chatter.Part) {
            require(part is Chatter.Part.Speaker)
            itemView.speakerTextView.apply {
                text = part.name
                updateSide(part.side)
            }
        }
    }

    class Bubble(parent: ViewGroup) :
        ChatterPartViewHolder(view(parent, R.layout.dialog_bubble)) {
        override fun update(part: Chatter.Part) {
            require(part is Chatter.Part.Bubble)
            itemView.bubbleTextView.apply {
                text = part.text
                setBackgroundResource(
                    when (part.side) {
                        Chatter.Side.LEFT -> R.drawable.left_solo_bubble
                        Chatter.Side.RIGHT -> {
                            when (part.type) {
                                Chatter.BubbleType.SOLO -> R.drawable.right_solo_bubble
                                Chatter.BubbleType.TOP -> R.drawable.right_top_bubble
                                Chatter.BubbleType.MIDDLE -> R.drawable.right_middle_bubble
                                Chatter.BubbleType.BOTTOM -> R.drawable.right_bottom_bubble
                            }
                        }
                    }
                )
                updateSide(part.side)
            }
        }
    }

    class Guard(parent: ViewGroup) :
        ChatterPartViewHolder(view(parent, R.layout.dialog_guard)) {
        override fun update(part: Chatter.Part) = Unit
    }

    companion object {
        private fun view(parent: ViewGroup, resId: Int): View =
            LayoutInflater.from(parent.context).inflate(resId, parent, false)

        private fun TextView.updateSide(side: Chatter.Side) {
            when (side) {
                Chatter.Side.LEFT -> updateLayoutParams<RelativeLayout.LayoutParams> {
                    removeRule(RelativeLayout.ALIGN_PARENT_START)
                    removeRule(RelativeLayout.ALIGN_PARENT_END)
                    addRule(RelativeLayout.ALIGN_PARENT_START)
                }
                Chatter.Side.RIGHT -> updateLayoutParams<RelativeLayout.LayoutParams> {
                    removeRule(RelativeLayout.ALIGN_PARENT_START)
                    removeRule(RelativeLayout.ALIGN_PARENT_END)
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                }
            }
        }
    }
}
