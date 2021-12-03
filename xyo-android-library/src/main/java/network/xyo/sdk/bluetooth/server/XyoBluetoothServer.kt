package network.xyo.sdk.bluetooth.server

import android.bluetooth.*
import android.util.Log
import android.util.SparseIntArray
import kotlinx.coroutines.*
import network.xyo.ble.generic.gatt.peripheral.XYBluetoothResult
import network.xyo.ble.generic.gatt.server.XYBluetoothCharacteristic
import network.xyo.ble.generic.gatt.server.XYBluetoothDescriptor
import network.xyo.ble.generic.gatt.server.XYBluetoothGattServer
import network.xyo.ble.generic.gatt.server.XYBluetoothService
import network.xyo.ble.generic.gatt.server.responders.XYBluetoothWriteResponder
import network.xyo.sdk.bluetooth.XyoUuids
import network.xyo.sdk.bluetooth.XyoUuids.NOTIFY_DESCRIPTOR
import network.xyo.sdk.bluetooth.packet.XyoBluetoothOutgoingPacket
import network.xyo.sdk.bluetooth.packet.XyoInputStream
import network.xyo.sdkcorekotlin.network.XyoAdvertisePacket
import network.xyo.sdkcorekotlin.network.XyoNetworkPipe
import network.xyo.sdkobjectmodelkotlin.structure.XyoObjectStructure

/**
 * A BLE GATT Server than can create XyoNetworkPipes. This pipe can be used with the sdk-core-kotlin to talk to
 * other XYO enabled devices.
 *
 * @property bluetoothServer The Bluetooth GATT server to create the pipe with.
 */
@InternalCoroutinesApi
class XyoBluetoothServer(private val bluetoothServer: XYBluetoothGattServer) {

    var listener: Listener? = null

    interface Listener {
        fun onPipe(pipe: XyoNetworkPipe)
    }

    /**
     * The key of the main gatt listener to listen for new new XYO Devices.
     */
    private val responderKey = this.toString()


    /**
     * A list of MTU values to device hash codes.
     *
     * mtuS[DEVICE HASH CODE] = MTU of DEVICE
     */
    private val mtuS = SparseIntArray()


    @OptIn(InternalCoroutinesApi::class)
    private val serverPrimaryEndpoint = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    //check to make sure we are being connected to.  A central should not have types
                    device?.let {
                        Log.i("Arie", "Type: ${it.type}")
                        //this causes a problem when the other device also is broadcasting.  Need to find a way to prevent listening when connecting out
                        //if (it.bluetoothClass?.majorDeviceClass == 0 && it.bluetoothClass.deviceClass == 0) {
                            Log.i(TAG, "onConnectionStateChange: ${device.bluetoothClass?.majorDeviceClass}:${device.bluetoothClass?.deviceClass} ")
                            GlobalScope.launch {
                                val inputStream = XyoInputStream()
                                val writeKey = "writing ${Math.random()}"

                                bluetoothWriteCharacteristic.addWriteResponder(writeKey, object : XYBluetoothWriteResponder {
                                    override fun onWriteRequest(writeRequestValue: ByteArray?, device: BluetoothDevice?): Boolean? {
                                        if (device?.address == device?.address && writeRequestValue != null) {
                                            inputStream.addChunk(writeRequestValue)
                                            return true
                                        }

                                        return null
                                    }
                                })

                                val incoming = waitForInputStreamPacket(inputStream)

                                bluetoothWriteCharacteristic.removeResponder(writeKey)

                                if (incoming != null) {
                                    val pipe = XyoBluetoothServerPipe(device, bluetoothWriteCharacteristic, incoming)
                                    listener?.onPipe(pipe)
                                }
                            }
                        //}
                    }
                }
            }
        }
    }


    /**
     * This pipe will be creating after a negotiation has occurred. This pipe abstracts a BLE Gatt Server than can
     * talk to a another BLE gatt client enabled device.
     *
     * @property bluetoothDevice The device that the server is connected to. This is used to filter request from
     * other devices.
     * @property writeCharacteristic The characteristic to write look for writes from the server.
     * @property catalog The catalog that the client has sent to the server on connection.
     */
    @InternalCoroutinesApi
    inner class XyoBluetoothServerPipe(private val bluetoothDevice: BluetoothDevice,
                                       private val writeCharacteristic: XYBluetoothCharacteristic,
                                       startingData: ByteArray) : XyoNetworkPipe {

        /**
         * The data that the connection was tarted with. This value is set to null since there is no ignition data
         * on the first write from the client.
         */
        override val initiationData: XyoAdvertisePacket? = XyoAdvertisePacket(startingData)


        /**
         * Closes the pipe. In this case, tells the BLE GATT server to shut disconnect from the device.
         */
        override suspend fun close(): Boolean {
            bluetoothServer.disconnect(bluetoothDevice)
            return true
        }

        override fun getNetworkHeuristics(): Array<XyoObjectStructure> {
            return arrayOf()
        }



        /**
         * Sends data to a BLE central/client through a pipe. This function wraps
         * with device connection functionality (e.g. listening for disconnects) asynchronously.
         *
         * @param data Data to send at the other end of the pipe.
         * @param waitForResponse If set to true, will wait for a response after sending the data.
         * @return The deferred response from the party at the other end of the pipe. If waitForResponse is set to
         * true. The method will return null. Will also return null if there is an error.
         */
        override suspend fun send(data: ByteArray, waitForResponse: Boolean): ByteArray? {
            val inputStream = XyoInputStream()

            return GlobalScope.async {
                if (!bluetoothServer.isDeviceConnected(bluetoothDevice)) {
                    return@async null
                }

                val disconnectKey = "$this disconnect"
                val writeKey = "$this disconnect"


                return@async suspendCancellableCoroutine<ByteArray?> { cont ->
                    GlobalScope.launch {
                        val listener = object : BluetoothGattServerCallback() {
                            override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
                                if (cont.isActive && newState == BluetoothGatt.STATE_DISCONNECTED && device?.address == bluetoothDevice.address) {
                                    bluetoothServer.removeListener(disconnectKey)
                                    val idempotent = cont.tryResume(null)
                                    idempotent?.let {
                                        cont.completeResume(it)
                                    }
                                    coroutineContext.cancel()
                                    return
                                }
                            }
                        }

                        bluetoothServer.addListener(disconnectKey, listener)

                        writeCharacteristic.addWriteResponder(writeKey, object : XYBluetoothWriteResponder {
                            override fun onWriteRequest(writeRequestValue: ByteArray?, device: BluetoothDevice?): Boolean? {
                                if (bluetoothDevice.address == device?.address && writeRequestValue != null) {
                                    inputStream.addChunk(writeRequestValue)
                                    return true
                                }

                                return null
                            }
                        })


                        sendAwaitAsync(data)

                        var response: ByteArray? = null

                        if (waitForResponse) {
                            response = waitForInputStreamPacket(inputStream)
                        }

                        bluetoothServer.removeListener(disconnectKey)
                        writeCharacteristic.removeResponder(writeKey)

                        val idempotent = cont.tryResume(response)
                        idempotent?.let {
                            cont.completeResume(it)
                        }
                    }
                }
            }.await()
        }


        /**
         * Sends data to the other end of the pipe, in this case a BLE central/client. NOTE: This function does not
         * check to see if the device is connected or listen for disconnects. That is handled in the send() function.
         *
         * @param outgoingPacket The data to send at the other end of the pipe.
         * @param waitForResponse If set to true, will wait for a response after sending the data.
         * @return The differed response from the party at the other end of the pipe. If waitForResponse is set to
         * true. The method will return null. Will also return null if there is an error.
         */
        private suspend fun sendAwaitAsync(outgoingPacket: ByteArray): ByteArray? {
            Log.i(TAG, "sendAwaitAsync: started")
            return sendPacket(outgoingPacket, writeCharacteristic, bluetoothDevice)?.value
        }
    }


    /**
     * Sends a packet to central by sending notifications one at a time. Notifications will chunk the
     * data accordingly at the size of the MTU.
     *
     * @param outgoingPacket The packet to send to the other end of the pipe (BLE Central)
     * @param characteristic The characteristic to notify has changed.
     * @param bluetoothDevice The bluetooth device to send data to.
     */
    private suspend fun sendPacket(outgoingPacket: ByteArray, characteristic: XYBluetoothCharacteristic, bluetoothDevice: BluetoothDevice) = suspendCancellableCoroutine<XYBluetoothResult<ByteArray>?> { cont ->
        val key = "sendPacket $this ${Math.random()}"
        Log.i(TAG, "sendPacket: started")
        characteristic.value = outgoingPacket

        val timeoutResume = GlobalScope.launch {
            delay(READ_TIMEOUT.toLong())
            if (this.isActive) {
                characteristic.removeReadResponder(key)

                val idempotent = cont.tryResume(null)
                idempotent?.let {
                    cont.completeResume(it)
                }
            }
        }

        val outgoingChuckedPacket = XyoBluetoothOutgoingPacket((mtuS[bluetoothDevice.hashCode()]) - 4, outgoingPacket, 4)


        GlobalScope.launch {
            while (outgoingChuckedPacket.canSendNext) {
                Log.i(TAG, "sendPacket: can send")
                val next = outgoingChuckedPacket.getNext()
                characteristic.value = next
                delay(ADVERTISEMENT_DELTA_TIMEOUT.toLong())

                Log.i(TAG, "sendPacket: sending notification")
                if (bluetoothServer.sendNotification(bluetoothWriteCharacteristic, true, bluetoothDevice)?.value != 0) {
                    Log.i(TAG, "sendPacket: send notification complete")
                    timeoutResume.cancel()
                    val idempotent = cont.tryResume(null)
                    idempotent?.let {
                        cont.completeResume(it)
                    }
                }

                if (!outgoingChuckedPacket.canSendNext) {
                    Log.i(TAG, "sendPacket: done")
                    timeoutResume.cancel()
                    val idempotent = cont.tryResume(null)
                    idempotent?.let {
                        cont.completeResume(it)
                    }
                }
            }
        }
    }

    private suspend fun waitForInputStreamPacket (inputStream: XyoInputStream) : ByteArray? {
        val currentWaitingPacket = inputStream.getOldestPacket()

        if (currentWaitingPacket != null) {
            return currentWaitingPacket
        }

        return suspendCancellableCoroutine { cont ->
            val timeoutJob = GlobalScope.launch {
                delay(READ_TIMEOUT.toLong())
                if (this.isActive) {
                    Log.e(TAG, "readPacket: Timed Out!")
                    val idempotent = cont.tryResume(null)
                    inputStream.onComplete = null
                    idempotent?.let {
                        cont.completeResume(it)
                    }
                }
            }

            inputStream.onComplete = { value ->
                val idempotent = cont.tryResume(value)
                idempotent?.let {
                    cont.completeResume(it)
                }

                timeoutJob.cancel()

                null
            }
        }
    }


    /**
     * Starts up the server so it can be connected to. NOTE: this does not start advertising. Advertising is
     * handled outside of the scope of this function.
     *
     * @return A deferred XYGattStatus with the status of the service being added.
     */
    suspend fun initServer(): XYBluetoothResult<Int>? {
        bluetoothWriteCharacteristic.addDescriptor(notifyDescriptor)
        bluetoothService.addCharacteristic(bluetoothWriteCharacteristic)
        bluetoothServer.startServer()

        return bluetoothServer.addService(bluetoothService)
    }

    /**
     * Listens for MTU changes and updates the device to MTU map accordingly.
     */
    private val mtuListener = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)

            when (newState) {
                BluetoothGatt.STATE_DISCONNECTED -> {
                    mtuS.delete(device.hashCode())
                }
            }
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            super.onMtuChanged(device, mtu)

            mtuS.put(device.hashCode(), mtu)
        }
    }


    init {
        bluetoothServer.addListener("main$this", mtuListener)
        bluetoothServer.addListener(responderKey, serverPrimaryEndpoint)
    }

    companion object {
        const val TAG = "XyoBluetoothServer"
        const val READ_TIMEOUT = 12_000
        const val ADVERTISEMENT_DELTA_TIMEOUT = 100

        private val notifyDescriptor = object : XYBluetoothDescriptor(NOTIFY_DESCRIPTOR, PERMISSION_WRITE or PERMISSION_READ) {
            override fun onWriteRequest(writeRequestValue: ByteArray?, device: BluetoothDevice?): Boolean? {
                return true
            }

            override fun onReadRequest(device: BluetoothDevice?, offset: Int): XYBluetoothGattServer.XYReadRequest? {
                return XYBluetoothGattServer.XYReadRequest(byteArrayOf(0x00, 0x00), 0)
            }

        }

        private val bluetoothWriteCharacteristic = XYBluetoothCharacteristic(
                XyoUuids.XYO_PIPE,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        private val bluetoothService = XYBluetoothService(
                XyoUuids.XYO_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
    }
}