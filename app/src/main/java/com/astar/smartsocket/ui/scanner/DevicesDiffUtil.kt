package com.astar.smartsocket.ui.scanner

import androidx.recyclerview.widget.DiffUtil

class DevicesDiffUtil(
    private val oldList: List<DeviceUi>,
    private val newList: List<DeviceUi>,
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition].same(newList[newItemPosition])

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]
}