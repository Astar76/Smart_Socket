package com.astar.smartsocket.data.connection

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

interface BluetoothConnectionCallback {

    fun onConnected(gatt: BluetoothGatt) {}

    fun onDisconnected(device: BluetoothDevice) {}

    fun onDeviceReady(gatt: BluetoothGatt) {}

    fun onCharacteristicChanged(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {}

    fun onCharacteristicRead(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {}

    fun onCharacteristicWrite(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {}

    fun onNotificationEnabled(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {}

    fun onNotificationDisabled(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {}

}