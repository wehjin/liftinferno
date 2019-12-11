package com.rubyhuntersky.liftlog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_add_movement.view.*


class AddMovementDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_add_movement, container, false).also { view ->
        view.movementTextView.setOnClickListener { anchor ->
            PopupMenu(this.context, anchor)
                .also {
                    it.menuInflater.inflate(R.menu.menu_pick_motion, it.menu)
                    it.setOnMenuItemClickListener {
                        view.movementTextView.text = it.title
                        true
                    }
                }
                .show()
        }
    }
}