package com.astar.smartsocket.ui.scanner

import android.bluetooth.BluetoothDevice

interface DeviceListUiMapper {

    fun map(list: List<BluetoothDevice>): DeviceListUi

    class Base : DeviceListUiMapper {
        override fun map(list: List<BluetoothDevice>): DeviceListUi {
            return DeviceListUi.Base(list.map { bluetoothDevice ->
                val deviceName: String = try {
                    bluetoothDevice.name
                } catch (e: SecurityException) {
                    "Unknown"
                }
                DeviceUi.Base(deviceName, bluetoothDevice.address)
            })
        }
    }
}