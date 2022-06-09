package com.astar.smartsocket.ui.scanner

import android.widget.TextView
import com.astar.smartsocket.core.Abstract

interface DeviceListUi : Abstract.Object<Unit, Abstract.Mapper.Data<List<DeviceUi>, Unit>> {

    class Base(private val list: List<DeviceUi>) : DeviceListUi {
        override fun map(mapper: Abstract.Mapper.Data<List<DeviceUi>, Unit>) {
            mapper.map(list)
        }
    }
}

interface DeviceUi {

    fun map(nameTextView: TextView, addressTextView: TextView) = Unit

    fun same(device: DeviceUi): Boolean = false

    fun control(control: Control) = Unit

    class Base(
        private val name: String,
        private val address: String,
    ) : DeviceUi {
        override fun map(nameTextView: TextView, addressTextView: TextView) {
            nameTextView.text = name
            addressTextView.text = address
        }

        override fun same(device: DeviceUi): Boolean {
            return device is Base && device.address == address
        }

        override fun control(control: Control) {
            control.openControlWith(address)
        }
    }

    class Empty: DeviceUi
    class Search: DeviceUi
}

interface Control {

    fun openControlWith(address: String)
}