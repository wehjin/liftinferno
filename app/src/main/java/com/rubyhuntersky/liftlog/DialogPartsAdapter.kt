package com.rubyhuntersky.liftlog

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class DialogPartsAdapter(private val dialogParts: List<Dialog.Part>) :
    RecyclerView.Adapter<DialogPartViewHolder>() {

    override fun getItemCount(): Int = dialogParts.size
    override fun getItemViewType(position: Int): Int = when (dialogParts[position]) {
        is Dialog.Part.Timestamp -> 1
        is Dialog.Part.Speaker -> 2
        is Dialog.Part.Bubble -> 3
        Dialog.Part.Guard -> 4
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DialogPartViewHolder {
        return when (viewType) {
            1 -> DialogPartViewHolder.Timestamp(parent)
            2 -> DialogPartViewHolder.Speaker(parent)
            3 -> DialogPartViewHolder.Bubble(parent)
            4 -> DialogPartViewHolder.Guard(parent)
            else -> TODO()
        }
    }

    override fun onBindViewHolder(holder: DialogPartViewHolder, position: Int) {
        holder.update(dialogParts[position])
    }
}