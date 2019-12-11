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

sealed class DialogPartViewHolder(partView: View) : RecyclerView.ViewHolder(partView) {

    abstract fun update(part: Dialog.Part)

    class Timestamp(parent: ViewGroup) :
        DialogPartViewHolder(toView(parent, R.layout.dialog_timestamp)) {

        override fun update(part: Dialog.Part) {
            require(part is Dialog.Part.Timestamp)
            itemView.timestampTextView.text = DateUtils.getRelativeTimeSpanString(part.date.time)
        }
    }

    class Speaker(parent: ViewGroup) :
        DialogPartViewHolder(toView(parent, R.layout.dialog_speaker)) {
        override fun update(part: Dialog.Part) {
            require(part is Dialog.Part.Speaker)
            itemView.speakerTextView.apply {
                text = part.name
                updateSide(part.side)
            }
        }
    }

    class Bubble(parent: ViewGroup) :
        DialogPartViewHolder(toView(parent, R.layout.dialog_bubble)) {
        override fun update(part: Dialog.Part) {
            require(part is Dialog.Part.Bubble)
            itemView.bubbleTextView.apply {
                text = part.text
                setBackgroundResource(
                    when (part.side) {
                        Dialog.Side.LEFT -> R.drawable.left_solo_bubble
                        Dialog.Side.RIGHT -> {
                            when (part.type) {
                                Dialog.BubbleType.SOLO -> R.drawable.right_solo_bubble
                                Dialog.BubbleType.TOP -> R.drawable.right_top_bubble
                                Dialog.BubbleType.MIDDLE -> R.drawable.right_middle_bubble
                                Dialog.BubbleType.BOTTOM -> R.drawable.right_bottom_bubble
                            }
                        }
                    }
                )
                updateSide(part.side)
            }
        }
    }

    class Guard(parent: ViewGroup) :
        DialogPartViewHolder(toView(parent, R.layout.dialog_guard)) {
        override fun update(part: Dialog.Part) = Unit
    }

    companion object {
        fun toView(parent: ViewGroup, resId: Int): View =
            LayoutInflater.from(parent.context).inflate(resId, parent, false)

        private fun TextView.updateSide(side: Dialog.Side) {
            when (side) {
                Dialog.Side.LEFT -> updateLayoutParams<RelativeLayout.LayoutParams> {
                    removeRule(RelativeLayout.ALIGN_PARENT_START)
                    removeRule(RelativeLayout.ALIGN_PARENT_END)
                    addRule(RelativeLayout.ALIGN_PARENT_START)
                }
                Dialog.Side.RIGHT -> updateLayoutParams<RelativeLayout.LayoutParams> {
                    removeRule(RelativeLayout.ALIGN_PARENT_START)
                    removeRule(RelativeLayout.ALIGN_PARENT_END)
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                }
            }
        }
    }
}
