package com.astar.smartsocket.ui.control

import androidx.lifecycle.viewModelScope
import com.astar.smartsocket.data.connection.BluetoothConnectionCallback
import com.astar.smartsocket.data.connection.TimerConnection
import com.astar.smartsocket.ui.core.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ControlViewModel(
    controlCommunication: ControlCommunication,
    private val connection: TimerConnection,
    private val dispatcherIO: CoroutineDispatcher = Dispatchers.IO,
    private val dispatcherMain: CoroutineDispatcher = Dispatchers.Main,
) : BaseViewModel<ControlCommunication, ControlUi>(controlCommunication),
    BluetoothConnectionCallback, TimerConnection.Callback, Timer {

    // todo refactor
    private var control: ControlUi = ControlUi.Base()

    init {
        connection.addCallback(this)
        connection.registerCallback(this)
        if (connection.connected()) {
            communication.map(ControlUi.Connected())
            viewModelScope.launch(dispatcherIO) {
                connection.requestTimerState()
            }
        }
    }

    fun setTimerValue(hours: Int, minutes: Int)  {
        val seconds = hours * 3600 + minutes * 60
        communication.map(ControlUi.SetTimer(seconds))

        viewModelScope.launch(dispatcherIO) {
            connection.resetTimer()
            connection.setTimer(seconds)
        }
    }

    fun runOnStop() {
        control.runOrStop(this)
    }

    override fun start() {
        viewModelScope.launch(dispatcherIO) {
            connection.startTimer()
        }
    }

    override fun stop() {
        viewModelScope.launch(dispatcherIO) {
            connection.stopTimer()
        }
    }

    override fun onTimerResponse(
        state: TimerConnection.State,
        response: TimerConnection.Response,
        seconds: Int,
    ) {
        viewModelScope.launch(dispatcherMain) {
            control = when (state) {
                TimerConnection.State.STARTED -> ControlUi.State(true)
                TimerConnection.State.STOPPED -> ControlUi.State(false)
            }
            communication.map(control)
        }
    }

    override fun onTimerTick(seconds: Int) {
        viewModelScope.launch(dispatcherMain) {
            communication.map(ControlUi.Tick(seconds))
        }
    }

    override fun onCleared() {
        connection.disconnect()
        connection.removeCallback(this)
        connection.unregisterCallback(this)
    }

    companion object {
        const val TAG = "ControlViewModel"
    }
}
