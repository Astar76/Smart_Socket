package com.astar.smartsocket.ui.scanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.util.Log
import androidx.lifecycle.*
import com.astar.smartsocket.data.connection.BluetoothConnectionCallback
import com.astar.smartsocket.data.connection.TimerConnection
import com.astar.smartsocket.data.scanner.Scanner
import com.astar.smartsocket.ui.Screen
import com.astar.smartsocket.ui.core.BaseViewModel
import com.astar.smartsocket.ui.core.Event
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DevicesViewModel(
    devicesCommunication: DeviceListCommunication,
    private val scannerStateCommunication: ScannerStateCommunication,
    private val bluetoothAdapter: BluetoothAdapter,
    private val devicesUiMapper: DeviceListUiMapper,
    private val connection: TimerConnection,
    private val scanner: Scanner,
    private val dispatcherIO: CoroutineDispatcher = Dispatchers.IO,
    private val dispatcherMain: CoroutineDispatcher = Dispatchers.Main,
) : BaseViewModel<DeviceListCommunication, DeviceListUi>(devicesCommunication),
    Scanner.Callback, BluetoothConnectionCallback, Control {

    // TODO: fix navigation
    private val _navigation = MutableLiveData<Event<Screen>>()
    val navigation: LiveData<Event<Screen>> get() = _navigation

    private val initial = DeviceListUi.Base(listOf(DeviceUi.Search()))

    init {
        scanner.addCallback(this)
        connection.addCallback(this)
        communication.map(initial)
        scannerStateCommunication.map(ScannerStateUi.Base())

        viewModelScope.launch(dispatcherMain) {
            scanner.discovered().collect { devices ->
                devicesCommunication.map(devicesUiMapper.map(devices))
            }
        }
    }

    fun observeState(owner: LifecycleOwner, observe: Observer<ScannerStateUi>) {
        scannerStateCommunication.observe(owner, observe)
    }

    fun startOrStopScan() = viewModelScope.launch(dispatcherIO) {
        if (scanner.scanning()) {
            scanner.stopScan()
        } else {
            scanner.startScan()
        }
    }

    fun stopScan() = viewModelScope.launch(dispatcherIO) {
        scanner.stopScan()
    }

    override fun scanningState(state: Scanner.State) {
        viewModelScope.launch(dispatcherMain) {
            when (state) {
                Scanner.State.SCANNING -> {
                    communication.map(DeviceListUi.Base(listOf(DeviceUi.Empty())))
                    scannerStateCommunication.map(ScannerStateUi.Running())
                }
                Scanner.State.STOPPED -> scannerStateCommunication.map(ScannerStateUi.Stopped())
            }
        }
    }

    override fun discoveredDevices(list: List<BluetoothDevice>) {
        Log.e(TAG, "discoveredDevices: $list")
    }

    override fun onDeviceReady(gatt: BluetoothGatt) {
        Log.e(TAG, "onDeviceReady(): called ")
        viewModelScope.launch(dispatcherMain) {
            val deviceName = try {
                gatt.device.name
            } catch (e: SecurityException) {
                "Unknown"
            }
            scannerStateCommunication.map(ScannerStateUi.Ready(deviceName))
            _navigation.value = Event(Screen.Control(gatt.device.address))
        }
    }

    /**
     * После успешного соединения вызовется onDeviceReady
     */
    override fun openControlWith(address: String) {
        connect(address)
    }

    private fun connect(address: String) = viewModelScope.launch(dispatcherIO) {
        Log.e(TAG, "connect() called")
        if (!connection.connected()) {
            connection.connect(bluetoothAdapter.getRemoteDevice(address))
            withContext(dispatcherMain) {
                scannerStateCommunication.map(ScannerStateUi.Connection())
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanner.removeCallback(this)
        connection.removeCallback(this)
        scannerStateCommunication.map(ScannerStateUi.Empty)
    }

    private companion object {
        const val TAG = "DevicesViewModel"
    }
}
