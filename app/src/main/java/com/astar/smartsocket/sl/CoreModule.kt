package com.astar.smartsocket.sl

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.astar.smartsocket.data.connection.TimerConnection
import com.astar.smartsocket.data.connection.TimerConnectionImpl
import com.astar.smartsocket.data.scanner.Scanner

class CoreModule {

    lateinit var scanner: Scanner
    lateinit var connection: TimerConnection
    lateinit var bluetoothAdapter: BluetoothAdapter

    fun init(context: Context) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter
        scanner = Scanner.Base(bluetoothAdapter)
        connection = TimerConnectionImpl(context)
    }
}