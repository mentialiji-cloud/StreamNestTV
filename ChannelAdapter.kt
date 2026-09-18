package com.shqiptv.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class ChannelAdapter(
    private val onFocus: (Channel) -> Unit,
    private val onClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.VH>() {

    private val items = ArrayList<Channel>()
    var focusedChannel: Channel? = null
        private set

    fun submit(newItems: List<Channel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val logo: ImageView = view.findViewById(R.id.channelLogo)
        private val name: TextView = view.findViewById(R.id.channelName)
        private val group: TextView = view.findViewById(R.id.channelGroup)

        fun bind(channel: Channel) {
            name.text = channel.name
            group.text = channel.group
            if (channel.logoUrl.isNotBlank()) {
                logo.visibility = View.VISIBLE
                logo.load(channel.logoUrl) { crossfade(false) }
            } else {
                logo.setImageResource(R.drawable.app_icon)
            }
            itemView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    focusedChannel = channel
                    onFocus(channel)
                    itemView.animate().scaleX(1.025f).scaleY(1.025f).setDuration(80).start()
                } else {
                    itemView.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                }
            }
            itemView.setOnClickListener { onClick(channel) }
        }
    }
}
