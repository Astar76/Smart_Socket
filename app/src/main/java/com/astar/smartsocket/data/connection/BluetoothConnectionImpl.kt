package com.astar.smartsocket.data.connection

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.astar.smartsocket.data.connection.Operation.*
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

abstract class BluetoothConnectionImpl(private val context: Context) : BluetoothConnection {

    private var callbacks: MutableSet<WeakReference<BluetoothConnectionCallback>> = mutableSetOf()

    private var currentDevice: BluetoothDevice? = null

    private var currentDeviceGatt: BluetoothGatt? = null
    private val operationQueue = ConcurrentLinkedQueue<Operation>()
    private var pendingOperation: Operation? = null

    abstract fun isDeviceSupported(gatt: BluetoothGatt): Boolean

    abstract fun onReadyDevice(gatt: BluetoothGatt)

    abstract fun onReadData(characteristic: BluetoothGattCharacteristic, data: ByteArray)

    protected fun enableNotification(characteristic: BluetoothGattCharacteristic) {
        if (connected()) {
            val device = checkNotNull(currentDevice)
            enqueueOperation(EnableNotification(device, characteristic.uuid))
        } else {
            Log.e(TAG, "enableNotification() not connected!")
        }
    }

    protected fun writeToCharacteristic(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
        if (connected()) {
            val device  = checkNotNull(currentDevice)
            val writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            enqueueOperation(WriteCharacteristic(device, characteristic, writeType, data))
        } else {
            Log.e(TAG, "writeToCharacteristic() Not connected!")
        }
    }

    override fun addCallback(callback: BluetoothConnectionCallback) {
        if (callbacks.map { it.get() }.contains(callback)) return
        callbacks.add(WeakReference(callback))
        callbacks = callbacks.filter { it.get() != null }.toMutableSet()
        Log.d(TAG, "Added callback $callback, ${callbacks.size} callbacks total")
    }

    override fun removeCallback(callback: BluetoothConnectionCallback) {
        var toRemove: WeakReference<BluetoothConnectionCallback>? = null
        callbacks.forEach { if (it.get() == callback) toRemove = it }
        toRemove?.let {
            callbacks.remove(it)
            Log.i(TAG, "Removed callback ${it.get()}, ${callbacks.size} callbacks total")
        }
    }

    override fun connected(): Boolean {
        return currentDeviceGatt != null
    }

    override fun connect(device: BluetoothDevice) {
        if (connected()) {
            Log.e(TAG, "Already connected to ${device.address}!")
        } else {
            this.currentDevice = device
            enqueueOperation(Connect(device))
        }
    }

    override fun disconnect() {
        if (connected()) {
            currentDevice?.let { enqueueOperation(Disconnect(it)) }
        } else {
            Log.e(TAG, "Not connected to ${currentDevice?.address}, cannot teardown connection!")
        }
    }

    @Synchronized
    private fun enqueueOperation(operation: Operation) {
        operationQueue.add(operation)
        if (pendingOperation == null) {
            doNextOperation()
        }
    }

    @Synchronized
    private fun signalEndOperation() {
        Log.d(TAG, "End of $pendingOperation")
        pendingOperation = null
        if (operationQueue.isNotEmpty()) {
            doNextOperation()
        }
    }

    private fun doNextOperation() {
        if (pendingOperation != null) {
            Log.e(TAG, "doNextOperation() called when an operation is pending! Aborting.")
            return
        }

        val operation = operationQueue.poll() ?: run {
            Log.i(TAG, "Operation queue empty, returning")
            return
        }

        pendingOperation = operation

        if (operation is Connect) {
            with(operation) {
                try {
                    Log.w(TAG, "Connecting to ${device.address}")
                    device.connectGatt(context, false, connectionCallback)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Error connection: No permission!", e)
                }
            }
            return
        }

        val gatt = currentDeviceGatt ?: run {
            Log.e(TAG,
                "Not connected to ${operation.device.address}! Aborting $operation operation.")
            signalEndOperation()
            return
        }

        when (operation) {
            is Disconnect -> with(operation) {
                try {
                    Log.d(TAG, "Disconnecting from ${device.address}")
                    gatt.close()
                    currentDeviceGatt = null
                    callbacks.forEach { it.get()?.onDisconnected(device) }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Error disconnection: No permission.", e)
                }
                signalEndOperation()
            }

            is WriteCharacteristic -> with(operation) {
                try {
                    characteristic.writeType = writeType
                    characteristic.value = data
                    gatt.writeCharacteristic(characteristic)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Write Characteristic error: No permission.", e)
                }
                signalEndOperation()
            }

            is ReadCharacteristic -> with(operation) {
                gatt.findCharacteristic(uuid)?.let { characteristic ->
                    try {
                        gatt.readCharacteristic(characteristic)
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Read Characteristic error: No permission.", e)
                    }
                    signalEndOperation()
                }
            }

            is EnableNotification -> with(operation) {
                gatt.findCharacteristic(uuid)?.let { characteristic ->
                    val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
                    val payload = when {
                        characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                        characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        else -> error("${characteristic.uuid} doesn't support notifications/indications")
                    }

                    characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
                        try {
                            if (!gatt.setCharacteristicNotification(characteristic, true)) {
                                Log.e(TAG,
                                    "setCharacteristicNotification failed for ${characteristic.uuid}")
                                signalEndOperation()
                                return
                            }

                            cccDescriptor.value = payload
                            gatt.writeDescriptor(cccDescriptor)
                        } catch (e: SecurityException) {
                            Log.e(TAG,
                                "setCharacteristicNotification failed for ${characteristic.uuid}! No permission!",
                                e)
                            signalEndOperation()
                            return
                        }
                    } ?: this@BluetoothConnectionImpl.run {
                        Log.e(TAG, "${characteristic.uuid} doesn't contain the CCC descriptor!")
                        signalEndOperation()
                    }
                } ?: this@BluetoothConnectionImpl.run {
                    Log.e(TAG, "Cannot find $uuid! Failed to enable notifications.")
                    signalEndOperation()
                }
            }

            is DisableNotification -> with(operation) {
                gatt.findCharacteristic(uuid)?.let { characteristic ->
                    val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
                    characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
                        try {
                            if (!gatt.setCharacteristicNotification(characteristic, false)) {
                                Log.e(TAG, "setCharacteristicNotification failed for ${characteristic.uuid}")
                                signalEndOperation()
                                return
                            }

                            cccDescriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(cccDescriptor)
                        } catch (e: SecurityException) {
                            Log.e(TAG, "setCharacteristicNotification failed for ${characteristic.uuid}! No permission!", e)
                            signalEndOperation()
                            return
                        }
                    } ?: this@BluetoothConnectionImpl.run {
                        Log.e(TAG, "${characteristic.uuid} doesn't contain the CCC descriptor!")
                        signalEndOperation()
                    }
                } ?: this@BluetoothConnectionImpl.run {
                    Log.e(TAG, "Cannot find $uuid! Failed to disable notifications.")
                    signalEndOperation()
                }
            }
        }
    }

    private val connectionCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w(TAG, "onConnectionStateChange: connected to $deviceAddress")
                    currentDeviceGatt = gatt
                    callbacks.forEach { it.get()?.onConnected(gatt) }
                    try {
                        Handler(Looper.getMainLooper()).post {
                            gatt.discoverServices()
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "onConnectionStateChange: No permission!")
                        disconnect()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.e(TAG, "onConnectionStateChange: disconnected from $deviceAddress")
                    disconnect()
                }
            } else {
                Log.e(TAG, "onConnectionStateChange: status $status encountered for $deviceAddress")
                if (pendingOperation is Connect) {
                    signalEndOperation()
                }
                disconnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.w(TAG, "Discovered ${services.size} services for ${device.address}")
                    printGattTable()

                    if (!isDeviceSupported(this)) {
                        Log.e(TAG, "onServicesDiscovered() Device not supported!")
                        if (pendingOperation is Connect) {
                            signalEndOperation()
                            enqueueOperation(Disconnect(device))
                        }
                        return
                    }

                    onReadyDevice(this)
                    callbacks.forEach { it.get()?.onDeviceReady(this) }
                } else {
                    Log.e(TAG, "Service discovery failed due to status $status")
                    this@BluetoothConnectionImpl.disconnect()
                }
            }

            if (pendingOperation is Connect) {
                signalEndOperation()
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i(TAG, "Read characteristic $uuid | value: ${value.toHexString()}")
                        onReadData(characteristic, characteristic.value)
                        callbacks.forEach { it.get()?.onCharacteristicRead(gatt.device, this) }
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e(TAG, "Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e(TAG, "Characteristic read failed for $uuid, error: $status")
                    }
                }

                if (pendingOperation is ReadCharacteristic) {
                    signalEndOperation()
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i(TAG, "Wrote to characteristic $uuid | value: ${value.toHexString()}")
                        callbacks.forEach { it.get()?.onCharacteristicWrite(gatt.device, this) }
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.e(TAG, "Write not permitted for $uuid")
                    }
                    else -> {
                        Log.e(TAG, "Characteristic write failed for $uuid, error: $status")
                    }
                }

                if (pendingOperation is WriteCharacteristic) {
                    signalEndOperation()
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            with(characteristic) {
                Log.i(TAG, "Characteristic $uuid changed | value: ${value.toHexString()}")
                onReadData(characteristic, characteristic.value)
                callbacks.forEach { it.get()?.onCharacteristicChanged(gatt.device, this) }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            with(descriptor) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i(TAG, "Wrote to descriptor $uuid | value: ${value.toHexString()}")
                        if (isCccd()) {
                            onCccdWrite(gatt, value, characteristic)
                        } else {
                            // callbacks write descriptor
                        }
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.e(TAG, "Write not permitted for $uuid")
                    }
                    else -> {
                        Log.e(TAG, "Descriptor write failed for $uuid, error: $status}")
                    }
                }
            }
            if (descriptor.isCccd() && (pendingOperation is EnableNotification || pendingOperation is DisableNotification)) {
                signalEndOperation()
            }
        }

        private fun onCccdWrite(
            gatt: BluetoothGatt,
            value: ByteArray,
            characteristic: BluetoothGattCharacteristic,
        ) {
            val charUuid = characteristic.uuid
            val notificationEnabled =
                value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
                        value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
            val notificationDisabled =
                value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)

            when {
                notificationEnabled -> {
                    Log.w(TAG, "Notifications or indications ENABLED on $charUuid")
                    callbacks.forEach {
                        it.get()?.onNotificationEnabled(gatt.device, characteristic)
                    }
                }
                notificationDisabled -> {
                    Log.w(TAG, "Notifications or indications DISABLED on $charUuid")
                    callbacks.forEach {
                        it.get()?.onNotificationDisabled(gatt.device, characteristic)
                    }
                }
                else -> {
                    Log.e(TAG, "Unexpected value ${value.toHexString()} on CCCD of $charUuid")
                }
            }
        }
    }

    private fun BluetoothGatt.findCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        services?.forEach { service ->
            service.characteristics?.firstOrNull { characteristic ->
                characteristic.uuid == uuid
            }?.let { matchingCharacteristic ->
                return matchingCharacteristic
            }
        }
        return null
    }

    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.i(TAG, "No service and characteristic available")
            return
        }
        services.forEach { service ->
            val characteristicsTable =
                service.characteristics.joinToString(separator = "\n|--", prefix = "|--") { char ->
                    var description = "${char.uuid}: ${char.printProperties()}"
                    if (char.descriptors.isNotEmpty()) {
                        description += "\n" + char.descriptors.joinToString(
                            separator = "\n-----",
                            prefix = "\n-----"
                        ) { descriptor ->
                            "${descriptor.uuid}: ${descriptor.printProperties()}"
                        }
                    }
                    description
                }
            Log.i(TAG, "Service ${service.uuid}\nCharacteristics:\n$characteristicsTable")
        }
    }

    private fun BluetoothGattCharacteristic.printProperties(): String =
        mutableListOf<String>().apply {
            if (isReadable()) add("READABLE")
            if (isWritable()) add("WRITABLE")
            if (isWritableWithoutResponse()) add("WRITABLE WITHOUT RESPONSE")
            if (isIndicatable()) add("INDICATABLE")
            if (isNotifiable()) add("NOTIFIABLE")
            if (isEmpty()) add("EMPTY")
        }.joinToString()

    private fun BluetoothGattCharacteristic.isReadable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    private fun BluetoothGattCharacteristic.isWritable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

    private fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

    private fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

    private fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

    private fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean =
        properties and property != 0

    private fun BluetoothGattDescriptor.printProperties(): String = mutableListOf<String>().apply {
        if (isReadable()) add("READABLE")
        if (isWritable()) add("WRITABLE")
        if (isEmpty()) add("EMPTY")
    }.joinToString()

    private fun BluetoothGattDescriptor.isReadable(): Boolean =
        containsPermission(BluetoothGattDescriptor.PERMISSION_READ)

    private fun BluetoothGattDescriptor.isWritable(): Boolean =
        containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE)

    private fun BluetoothGattDescriptor.containsPermission(permission: Int): Boolean =
        permissions and permission != 0

    private fun BluetoothGattDescriptor.isCccd() =
        uuid.toString().uppercase(Locale.US) == CCC_DESCRIPTOR_UUID.uppercase(Locale.US)

    protected fun ByteArray.toHexString(): String =
        joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }

    private companion object {

        const val TAG = "Connection"

        const val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805F9B34FB"

        const val CONTROL_UUID = "e51ca6f2-c931-402d-a7a1-8a9df67e0385"

        const val RESPONSE_UUID = "323f3f87-411a-421b-ac13-3871c3b58cba"
    }
}
