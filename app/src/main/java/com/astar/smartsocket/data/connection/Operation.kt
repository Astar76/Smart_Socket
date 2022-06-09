package com.astar.smartsocket.data.connection

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import java.util.*

sealed class Operation {

    abstract val device: BluetoothDevice

    data class Connect(override val device: BluetoothDevice) : Operation()

    data class Disconnect(override val device: BluetoothDevice) : Operation()

    data class WriteCharacteristic(
        override val device: BluetoothDevice,
        val characteristic: BluetoothGattCharacteristic,
        val writeType: Int,
        val data: ByteArray,
    ) : Operation() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return true

            other as WriteCharacteristic

            if (device != other.device) return false
            if (characteristic != other.characteristic) return false
            if (writeType != other.writeType) return false
            if (!data.contentEquals(other.data)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = device.hashCode()
            result = 31 * result + characteristic.hashCode()
            result = 31 * result + writeType
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    data class ReadCharacteristic(
        override val device: BluetoothDevice,
        val uuid: UUID,
    ) : Operation()

    data class EnableNotification(
        override val device: BluetoothDevice,
        val uuid: UUID,
    ) : Operation()

    data class DisableNotification(
        override val device: BluetoothDevice,
        val uuid: UUID
    ): Operation()
}