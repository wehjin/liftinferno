package com.rubyhuntersky.liftlog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rubyhuntersky.liftlog.story.MovementAction
import com.rubyhuntersky.liftlog.story.MovementVision
import kotlinx.android.synthetic.main.fragment_add_movement.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview


class MovementDialogFragment : BottomSheetDialogFragment() {

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
        rendering = renderStory(this, storyId, MainEdge) { vision, post ->
            when (vision) {
                is MovementVision.Adding -> {
                    view?.let { view ->
                        view.weightEditText.setText(vision.movement.force.value.toString())
                        view.repsEditText.setText(vision.movement.distance.count.toString())
                        view.addButton.setOnClickListener {
                            post(MovementAction.Cancel)
                        }
                    }
                }
                is MovementVision.Dismissed -> dismiss()
            }
        }
    }

    private lateinit var rendering: Rendering<MovementVision, MovementAction>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_add_movement, container, false)
        .also { view ->
            view.movementTextView.setOnClickListener { anchor ->
                PopupMenu(context, anchor).also {
                    it.menuInflater.inflate(R.menu.menu_pick_motion, it.menu)
                    it.setOnMenuItemClickListener {
                        view.movementTextView.text = it.title
                        true
                    }
                }.show()
            }
        }

    override fun onDismiss(dialog: DialogInterface) {
        rendering.story?.offer(MovementAction.Cancel)
        super.onDismiss(dialog)
    }
}