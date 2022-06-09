package com.astar.smartsocket.ui.scanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.astar.smartsocket.R
import com.astar.smartsocket.core.Abstract
import com.astar.smartsocket.ui.core.ClickListener
import java.lang.IllegalStateException

class DevicesAdapter(
    private val clickListener: ClickListener<DeviceUi>,
) : RecyclerView.Adapter<DevicesViewHolder>(),
    Abstract.Mapper.Data<List<DeviceUi>, Unit> {

    private val items = ArrayList<DeviceUi>()

    override fun map(data: List<DeviceUi>) {
        val diff = DevicesDiffUtil(items, data)
        val result = DiffUtil.calculateDiff(diff)
        items.clear()
        items.addAll(data)
        result.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DeviceUi.Base -> 0
            is DeviceUi.Search -> 1
            is DeviceUi.Empty -> 2
            else -> -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DevicesViewHolder {
        return when (viewType) {
            0 -> DevicesViewHolder.Base(R.layout.item_device.view(parent), clickListener)
            1 -> DevicesViewHolder.Search(R.layout.scanning.view(parent))
            2 -> DevicesViewHolder.Empty(R.layout.empty_item.view(parent))
            else -> throw IllegalStateException("unknown view type: $viewType")
        }

    }

    override fun onBindViewHolder(holder: DevicesViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    private fun Int.view(parent: ViewGroup) =
        LayoutInflater.from(parent.context).inflate(this, parent, false)
}

abstract class DevicesViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    open fun bind(item: DeviceUi) {}

    class Base(
        view: View, private val clickListener: ClickListener<DeviceUi>,
    ) : DevicesViewHolder(view) {

        private val nameTextView: TextView = view.findViewById(R.id.device_name)
        private val addressTextView: TextView = view.findViewById(R.id.device_address)

        override fun bind(item: DeviceUi) {
            itemView.setOnClickListener { clickListener.onClick(item) }
            item.map(nameTextView, addressTextView)
        }
    }

    class Empty(view: View) : DevicesViewHolder(view)
    class Search(view: View) : DevicesViewHolder(view)

}