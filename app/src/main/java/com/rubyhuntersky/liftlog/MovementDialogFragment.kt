package com.rubyhuntersky.liftlog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rubyhuntersky.liftlog.story.Direction
import com.rubyhuntersky.liftlog.story.MovementStory.Vision
import com.rubyhuntersky.liftlog.story.Story
import com.rubyhuntersky.liftlog.story.cancel
import com.rubyhuntersky.tomedb.Owner
import kotlinx.android.synthetic.main.fragment_add_movement.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.util.*

class MovementDialogFragment : BottomSheetDialogFragment(), RenderingScope {

    var storyId: Pair<String, Int>
        get() {
            val name = arguments?.getString("story-name") ?: "unknown"
            val number = arguments?.getInt("story-number") ?: 0
            return Pair(name, number)
        }
        set(value) {
            arguments = (arguments ?: Bundle()).apply {
                putString("story-name", value.first)
                putInt("story-number", value.second)
            }
        }

    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        story = renderStory(this, storyId, MainEdge) { vision, post ->
            when (vision) {
                is Vision.Interacting -> {
                    view?.let { view ->
                        view.directionTextView.renderDirection(vision.direction) {
                            post(vision.directionAction(it))
                        }
                        view.weightEditText.render(vision.force?.toString() ?: "") {
                            val lbs = it.toIntOrNull()
                            post(vision.forceAction(lbs))
                        }
                        view.repsEditText.render(vision.distance?.toString() ?: "") {
                            val count = it.toIntOrNull()
                            post(vision.distanceAction(count))
                        }
                        view.addButton.render(
                            onClick = if (vision.isReadyToAdd) {
                                val function = { post(vision.addAction()) }
                                function
                            } else null
                        )
                    }
                }
                is Vision.Dismissed -> dismiss()
            }
        }
    }

    private fun TextView.renderDirection(direction: Direction, onPicked: ((Direction) -> Unit)?) {
        val string = getString(stringRes(direction))
        if (text != string) {
            text = string
        }
        onDirectionPicked = onPicked
    }

    private var onDirectionPicked: ((direction: Direction) -> Unit)? = null

    private lateinit var story: () -> Story<Vision, Owner<Date>>?

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_add_movement, container, false)
        .also { view ->
            view.directionTextView.setOnClickListener { anchor ->
                PopupMenu(context, anchor).also { menu ->
                    menu.menuInflater.inflate(R.menu.menu_pick_motion, menu.menu)
                    menu.setOnMenuItemClickListener {
                        val direction = direction(it.itemId)
                        view.directionTextView.text = getString(stringRes(direction))
                        onDirectionPicked?.invoke(direction)
                        true
                    }
                }.show()
            }
        }

    private fun direction(itemId: Int): Direction = when (itemId) {
        R.id.pick_pullups -> Direction.PullUps
        R.id.pick_squats -> Direction.Squats
        R.id.pick_dips -> Direction.Dips
        R.id.pick_hinges -> Direction.Hinges
        R.id.pick_pushups -> Direction.PushUps
        R.id.pick_rows -> Direction.Rows
        R.id.pick_other -> Direction.Other
        else -> TODO()
    }

    private fun stringRes(direction: Direction): Int = when (direction) {
        Direction.PullUps -> R.string.pullups
        Direction.Squats -> R.string.squats
        Direction.Dips -> R.string.dips
        Direction.Hinges -> R.string.hinges
        Direction.PushUps -> R.string.pushups
        Direction.Rows -> R.string.rows
        Direction.Other -> R.string.other
    }

    override fun onDismiss(dialog: DialogInterface) {
        story()?.cancel()
        super.onDismiss(dialog)
    }

}