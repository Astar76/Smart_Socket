package com.astar.smartsocket.data.connection

import android.bluetooth.BluetoothDevice

interface BluetoothConnection {

    fun addCallback(callback: BluetoothConnectionCallback)

    fun removeCallback(callback: BluetoothConnectionCallback)

    fun connected(): Boolean

    fun connect(device: BluetoothDevice)

    fun disconnect()
}