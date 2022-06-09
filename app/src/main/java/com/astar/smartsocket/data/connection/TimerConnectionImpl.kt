package com.astar.smartsocket.data.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.astar.smartsocket.data.connection.TimerConnection.*
import java.nio.ByteBuffer
import java.util.*

class TimerConnectionImpl(context: Context) : BluetoothConnectionImpl(context), TimerConnection {

    private var controlCharacteristic: BluetoothGattCharacteristic? = null
    private var responseCharacteristic: BluetoothGattCharacteristic? = null

    private var callbacks = ArrayList<Callback>()

    override fun registerCallback(callback: Callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback)
        }
    }

    override fun unregisterCallback(callback: Callback) {
        callbacks.remove(callback)
    }

    override fun isDeviceSupported(gatt: BluetoothGatt): Boolean {
        val service = gatt.getService(UUID.fromString(MAIN_SERVICE_UUID))
        if (service != null) {
            controlCharacteristic =
                service.getCharacteristic(UUID.fromString(CONTROL_CHARACTERISTIC_UUID))
            responseCharacteristic =
                service.getCharacteristic(UUID.fromString(RESPONSE_CHARACTERISTIC_UUID))
        }
        return controlCharacteristic != null && responseCharacteristic != null
    }

    override fun onReadyDevice(gatt: BluetoothGatt) {
        val characteristic = checkNotNull(responseCharacteristic)
        enableNotification(characteristic)
    }

    override fun onReadData(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
        if (characteristic.uuid.toString() == RESPONSE_CHARACTERISTIC_UUID) {
            Log.e("TimeConnection", "onReadData: ${data.toHexString()}")
            parse(data)
        }
    }

    override fun setTimer(seconds: Int) {
        val characteristic = checkNotNull(controlCharacteristic)
        val secondsBytes = ByteBuffer.allocate(4).putInt(seconds).array()
        val commandArray = ByteArray(5)
        commandArray[0] = COMMAND_SET_TIMER
        System.arraycopy(secondsBytes, 0, commandArray, 1, secondsBytes.size)
        writeToCharacteristic(characteristic, commandArray)
    }

    override fun startTimer() {
        val characteristic = checkNotNull(controlCharacteristic)
        writeToCharacteristic(characteristic, byteArrayOf(COMMAND_START_TIMER))
    }

    override fun stopTimer() {
        val characteristic = checkNotNull(controlCharacteristic)
        writeToCharacteristic(characteristic, byteArrayOf(COMMAND_STOP_TIMER))
    }

    override fun requestTimerState() {
        val characteristic = checkNotNull(controlCharacteristic)
        writeToCharacteristic(characteristic, byteArrayOf(COMMAND_REQUEST_INFO))
    }

    override fun resetTimer() {
        val characteristic = checkNotNull(controlCharacteristic)
        writeToCharacteristic(characteristic, byteArrayOf(COMMAND_RESET_TIMER))
    }

    private fun parse(data: ByteArray) {
        val d = Data(data)
        val response = if (data[1] == RESPONSE_SUCCESS) Response.SUCCESS else Response.ERROR
        when (data[0]) {
            // COMMAND_SET_TIMER -> callbacks.map { it.onTimerResponse(State.SET_TIMER, response, -1) }
            COMMAND_START_TIMER -> callbacks.map { it.onTimerResponse(State.STARTED, response, -1) }
            COMMAND_RESET_TIMER -> {}
            COMMAND_STOP_TIMER -> callbacks.map { it.onTimerResponse(State.STOPPED, response, -1) }
            COMMAND_REQUEST_INFO -> {
                val state = if (data[2] == TIMER_STATE_RUNNING) State.STARTED else State.STOPPED
                if (state == State.STARTED && data.isNotEmpty()) {
                    val seconds: Int = d.getIntValue(Data.FORMAT_UINT32_BE, 3) ?: 0
                    callbacks.map { it.onTimerResponse(state, response, seconds) }
                    return
                }
                callbacks.map { it.onTimerResponse(state, response, -1) }
            }
            COMMAND_TICK_TIMER -> {
                if (data.isNotEmpty()) {
                    val seconds = d.getIntValue(Data.FORMAT_UINT32_BE, 1) ?: 0
                    callbacks.forEach { it.onTimerTick(seconds) }
                }
            }
        }
    }

    companion object {

        const val MAIN_SERVICE_UUID = "396300c2-438f-4ed6-8983-27ca480d7f40"
        const val CONTROL_CHARACTERISTIC_UUID = "e51ca6f2-c931-402d-a7a1-8a9df67e0385"
        const val RESPONSE_CHARACTERISTIC_UUID = "323f3f87-411a-421b-ac13-3871c3b58cba"

        const val COMMAND_SET_TIMER: Byte = 0x1
        const val COMMAND_START_TIMER: Byte = 0x2
        const val COMMAND_STOP_TIMER: Byte = 0x3
        const val COMMAND_REQUEST_INFO: Byte = 0x4
        const val COMMAND_RESET_TIMER: Byte = 0x5
        const val COMMAND_TICK_TIMER: Byte = 0x6

        const val RESPONSE_ERROR: Byte = 0x1
        const val RESPONSE_SUCCESS: Byte = 0x2
        const val TIMER_STATE_STOPPED: Byte = 0x1
        const val TIMER_STATE_RUNNING: Byte = 0x2
    }
}