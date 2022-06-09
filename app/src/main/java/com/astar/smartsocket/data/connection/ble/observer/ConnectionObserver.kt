package com.astar.smartsocket.data.connection.ble.observer

interface ConnectionObserver {

    fun onConnected()

    fun onDisconnected()

    fun onFailedToConnect()

    fun onReady()
}