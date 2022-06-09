package com.astar.smartsocket.data.connection

interface TimerConnection : BluetoothConnection {

    fun registerCallback(callback: Callback)

    fun unregisterCallback(callback: Callback)

    fun setTimer(seconds: Int)

    fun startTimer()

    fun stopTimer()

    fun requestTimerState()

    fun resetTimer()

    enum class State {
        STARTED, STOPPED
    }

    enum class Response {
        SUCCESS, ERROR
    }

    interface Callback {

        fun onTimerResponse(state: State, response: Response, seconds: Int)

        fun onTimerTick(seconds: Int)
    }
}