package network.xyo.sdk.bluetooth.client

import android.bluetooth.BluetoothGatt
import android.util.Log
import kotlinx.coroutines.*
import network.xyo.ble.generic.devices.XYBluetoothDevice
import network.xyo.ble.generic.gatt.peripheral.XYBluetoothResult
import network.xyo.ble.generic.gatt.peripheral.XYBluetoothResultErrorCode
import network.xyo.ble.generic.listeners.XYBluetoothDeviceListener
import network.xyo.sdk.bluetooth.XyoUuids
import network.xyo.sdkcorekotlin.network.XyoAdvertisePacket
import network.xyo.sdkcorekotlin.network.XyoNetworkPipe
import network.xyo.sdkcorekotlin.schemas.XyoSchemas
import network.xyo.sdkobjectmodelkotlin.structure.XyoObjectStructure
import kotlin.coroutines.suspendCoroutine
import network.xyo.sdkobjectmodelkotlin.toHexString
import kotlin.coroutines.resume

@kotlin.ExperimentalUnsignedTypes
class XyoBluetoothClientPipe(val client: XyoBluetoothClient) : XyoNetworkPipe {

    override val initiationData: XyoAdvertisePacket? = null

    /**
     * Closes the pipe between parties. In this case, disconnects from the device and closes the GATT. This should
     * be called after the pipe is finished being used.
     */
    override suspend fun close(): Boolean {
        Log.i(TAG, "close: started")
        client.disconnect()
        return true
    }

    override fun getNetworkHeuristics(): Array<XyoObjectStructure> {
        Log.i(TAG, "getNetworkHeuristics: started")
        val toReturn = ArrayList<XyoObjectStructure>()

        val rssi = client.rssi

        if (rssi != null) {
            val encodedRssi = XyoObjectStructure.newInstance(XyoSchemas.RSSI, byteArrayOf(rssi.toByte()))
            toReturn.add(encodedRssi)
        }

        val pwr = XyoObjectStructure.newInstance(XyoSchemas.BLE_POWER_LVL, byteArrayOf(client.power))
        toReturn.add(pwr)

        return toReturn.toTypedArray()
    }

    fun wrapAsync() = GlobalScope.async {
        return@async client.readIncoming()
    }

    /**
     * Sends data and waits for a response if the waitForResponse flag is set to true. 
     * NOTE: The send and recieve are abstracted away from the caller, thus the exact bytes transferred is unclear.
     *
     * @param data Data to send to the other end of the pipe.
     * @param waitForResponse If this flag is set, this function will wait for a response. If not, will return
     * null.
     * @return A deferred ByteArray of the response of the server. If waitForResponse is null, will return null.
     */
    override suspend fun send(data: ByteArray, waitForResponse: Boolean): ByteArray? {
        Log.i(TAG, "send: started")
        return suspendCoroutine { cont ->
            val disconnectKey = this.toString() + Math.random().toString()

            val sendAndReceive = GlobalScope.async {
                Log.i(TAG, "send: sendAndReceive: started")
                val job = wrapAsync()
                val packetError = client.chunkSend(data, XyoUuids.XYO_PIPE, XyoUuids.XYO_SERVICE, 4)

                Log.i(TAG, "Sent entire packet to the server.")
                if (packetError == XYBluetoothResultErrorCode.None) {
                    Log.i(TAG, "Sent entire packet to the server (good).")
                    var valueIn: ByteArray? = null

                    if (waitForResponse) {
                        valueIn = job.await()
                    }

                    Log.i(TAG, "Have read entire server response packet. ${valueIn?.toHexString()}")
                    client.removeListener(disconnectKey)
                    cont.resume(valueIn)
                } else {
                    client.log.info("Error sending entire packet to the server. ${packetError.name}")
                    client.removeListener(disconnectKey)
                    cont.resume(null)
                }
            }

            // add the disconnect listener
            client.log.info("Adding disconnect listener.")
            client.addListener(disconnectKey, object : XYBluetoothDeviceListener() {
                override fun connectionStateChanged(device: XYBluetoothDevice, newState: Int) {
                    Log.i(TAG, "send: connectionStateChanged: $newState")
                    when (newState) {

                        BluetoothGatt.STATE_DISCONNECTED -> {
                            client.log.info("Someone disconnected.")

                            if (cont.context.isActive) {
                                client.log.info("Context is still active.")
                                client.removeListener(disconnectKey)

                                client.log.info("Canceling send and receive.")

                                cont.resume(null)
                                cont.context.cancel()
                                sendAndReceive.cancel()
                            }
                        }
                    }
                }
            })
        }
    }

    companion object {
        const val TAG = "XyoBluetoothClientPipe"
    }
}