package com.astar.smartsocket.data.scanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.isActive
import kotlinx.coroutines.isActive

interface Scanner {

    suspend fun startScan()

    suspend fun stopScan()

    fun discovered(): Flow<List<BluetoothDevice>>

    fun addCallback(callback: Callback)

    fun removeCallback(callback: Callback)

    fun scanning(): Boolean

    enum class State {
        SCANNING, STOPPED
    }

    interface Callback {

        fun scanningState(state: State)

        fun discoveredDevices(list: List<BluetoothDevice>)
    }

    class Base(adapter: BluetoothAdapter) : Scanner {

        private var scannerCallback: BLEScannerCallback? = null
        private val scannerBle: BluetoothLeScanner = adapter.bluetoothLeScanner
        private val devicesSet = HashSet<BluetoothDevice>()
        private val callbacks = ArrayList<Callback>()
        private val handler = Handler(Looper.myLooper()!!)

        private var send = false

        override suspend fun startScan() {
            if (scannerCallback == null) {
                scannerCallback = BLEScannerCallback()
                devicesSet.clear()
                try {
                    scannerBle.startScan(scannerCallback)
                    handler.postDelayed(stopScanRunnable, SCANNING_DURATION)
                    callbacks.forEach { it.scanningState(State.SCANNING) }
                } catch (e: SecurityException) {
                    scannerCallback = null
                    callbacks.forEach { it.scanningState(State.STOPPED) }
                    Log.e(TAG, "ble scanning error", e)
                }
                return
            }
            Log.w(TAG, "already scan!")
        }

        private val stopScanRunnable = Runnable {
            try {
                scannerBle.stopScan(scannerCallback)
                callbacks.forEach { it.scanningState(State.STOPPED) }
                scannerCallback = null
            } catch (e: SecurityException) {
                Log.e(TAG, "ble stop scanning error", e)
            }
        }

        override suspend fun stopScan() {
            if (scannerCallback != null) {
                handler.removeCallbacks(stopScanRunnable)
                stopScanRunnable.run()
            } else {
                Log.w(TAG, "scanner is not scanning anything")
            }
        }

        override fun addCallback(callback: Callback) {
            if (!callbacks.contains(callback)) {
                callbacks.add(callback)
            }
        }

        override fun removeCallback(callback: Callback) {
            callbacks.remove(callback)
        }

        override fun scanning(): Boolean {
            return scannerCallback != null
        }

        override fun discovered() = flow {
            while (currentCoroutineContext().isActive) {
                if (send) {
                    emit(devicesSet.toList())
                    send = false
                }
                delay(100)
            }
        }

        inner class BLEScannerCallback : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (!devicesSet.contains(result.device)) {
                    devicesSet.add(result.device)
                    callbacks.forEach { it.discoveredDevices(devicesSet.toList()) }
                    send = true
                }
            }
        }

        private companion object {
            const val TAG = "Scanner"

            const val SCANNING_DURATION = 20000L
        }
    }
}