package com.rubyhuntersky.liftlog

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class ChatterPartsAdapter(private val chatterParts: List<Chatter.Part>) :
    RecyclerView.Adapter<ChatterPartViewHolder>() {

    override fun getItemCount(): Int = chatterParts.size
    override fun getItemViewType(position: Int): Int = when (chatterParts[position]) {
        is Chatter.Part.Timestamp -> 1
        is Chatter.Part.Speaker -> 2
        is Chatter.Part.Bubble -> 3
        Chatter.Part.Guard -> 4
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatterPartViewHolder {
        return when (viewType) {
            1 -> ChatterPartViewHolder.Timestamp(parent)
            2 -> ChatterPartViewHolder.Speaker(parent)
            3 -> ChatterPartViewHolder.Bubble(parent)
            4 -> ChatterPartViewHolder.Guard(parent)
            else -> TODO()
        }
    }

    override fun onBindViewHolder(holder: ChatterPartViewHolder, position: Int) {
        holder.update(chatterParts[position])
    }
}