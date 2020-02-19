package network.xyo.sdk.bluetooth.client

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import network.xyo.ble.devices.apple.XYAppleBluetoothDevice
import network.xyo.ble.devices.apple.XYIBeaconBluetoothDevice
import network.xyo.ble.generic.devices.XYBluetoothDevice
import network.xyo.ble.generic.devices.XYCreator
import network.xyo.ble.generic.gatt.peripheral.XYBluetoothGattCallback
import network.xyo.ble.generic.gatt.peripheral.XYBluetoothResult
import network.xyo.ble.generic.scanner.XYScanResult
import network.xyo.sdk.bluetooth.XyoUuids
import network.xyo.sdk.bluetooth.packet.XyoBluetoothIncomingPacket
import network.xyo.sdk.bluetooth.packet.XyoBluetoothOutgoingPacket
import network.xyo.sdkcorekotlin.network.XyoNetworkPipe
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.experimental.and

enum class XyoBluetoothClientDeviceType(val raw: Byte){
    SentinelX(0x01),
    IosAppX(0x02),
    BridgeX(0x03),
    AndroidAppX(0x04),
}

/**
 * A Bluetooth client that can create a XyoNetworkPipe. This pipe can be used with the sdk-core-kotlin to talk to
 * other XYO enabled devices.
 *
 * @property context Context of the device
 * @property device The android bluetooth device
 * @property hash The unique hash of the device
 */
@kotlin.ExperimentalUnsignedTypes
open class XyoBluetoothClient : XYIBeaconBluetoothDevice {

    constructor(context: Context, scanResult: XYScanResult, hash: String) : super(context, scanResult, hash)

    constructor(context: Context, scanResult: XYScanResult, hash: String, transport: Int) : super(context, scanResult, hash, transport)

    /**
     * The standard size of the MTU of the connection. This value is used when chunking large amounts of data.
     */
    private var mtu = DEFAULT_MTU

    /**
     * creates a XyoNetworkPipe with THIS bluetooth device.
     * @return A Deferred XyoNetworkPipe if successful, null if not.
     */
    suspend fun createPipe(): XyoNetworkPipe? {
        findAndWriteCharacteristicNotify(XyoUuids.XYO_SERVICE, XyoUuids.XYO_PIPE, true)

        val requestMtu = requestMtu(MAX_MTU)

        mtu = (requestMtu.value ?: mtu) - 3

        return XyoBluetoothClientPipe(this)
    }

    /**
     * Get the public Key
     */
    suspend fun getPublicKey(): XYBluetoothResult<ByteArray> {
        return findAndReadCharacteristicBytes(XyoUuids.XYO_SERVICE, XyoUuids.XYO_PUBLIC_KEY)
    }

    /**
     * Preforms a chunk send
     *
     * @param outgoingPacket The packet to send to the server. This value will be chunked accordingly, if larger than
     * the MTU of the connection.
     * @param characteristic The characteristic UUID to write to.
     * @param service The service UUID to write to.
     * @param sizeOfSize size of the packet header size to send
     * @return An XYBluetoothError if there was an issue writing the packet.
     */
    suspend fun chunkSend(outgoingPacket: ByteArray, characteristic: UUID, service: UUID, sizeOfSize: Int): XYBluetoothResult.ErrorCode {
        Log.i(TAG, "chunkSend: started")
        val chunkedOutgoingPacket = XyoBluetoothOutgoingPacket(mtu, outgoingPacket, sizeOfSize)

        var errorCode = XYBluetoothResult.ErrorCode.None

        connection {
            while (chunkedOutgoingPacket.canSendNext && errorCode == XYBluetoothResult.ErrorCode.None) {
                val result = findAndWriteCharacteristic(
                        service,
                        characteristic,
                        chunkedOutgoingPacket.getNext(),
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                delay(500)
                if (result.error != XYBluetoothResult.ErrorCode.None) {
                    errorCode = result.error
                }
            }
            return@connection XYBluetoothResult(true)
        }
        return errorCode
    }


    /**
     * Reads an incoming packet by listening for notifications. This function must be invoked before any notifications
     * are sent or else will return null. Timeout of the first notification is defined with FIRST_NOTIFY_TIMEOUT, in
     * milliseconds and notification delta timeout is defined as NOTIFY_TIMEOUT in milliseconds.
     *
     * @return A deferred ByteArray of the value read. If there was an error or timeout, will return null.
     */
    suspend fun readIncoming(): ByteArray? {
        Log.i(TAG, "readIncoming: started")
        return suspendCoroutine { cont ->
            val key = this.toString() + Math.random().toString()

            centralCallback.addListener(key, object : XYBluetoothGattCallback() {
                var numberOfPackets = 0
                var hasResumed = false

                var timeoutJob: Job = GlobalScope.launch {
                    delay(FIRST_NOTIFY_TIMEOUT.toLong())
                    if (isActive) {
                        Log.e(TAG, "readIncoming: timeout")
                        hasResumed = true
                        centralCallback.removeListener(key)
                        cont.resume(null)
                    }
                }

                var incomingPacket: XyoBluetoothIncomingPacket? = null

                override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                    Log.i(TAG, "readIncoming: onCharacteristicChanged")
                    super.onCharacteristicChanged(gatt, characteristic)
                    val value = characteristic?.value

                    if (characteristic?.uuid == XyoUuids.XYO_PIPE && !hasResumed) {

                        if (numberOfPackets == 0 && value != null) {
                            incomingPacket = XyoBluetoothIncomingPacket(value)
                        } else if (value != null) {
                            incomingPacket?.addPacket(value)
                        }

                        if (incomingPacket?.done == true) {
                            hasResumed = true
                            centralCallback.removeListener(key)
                            timeoutJob.cancel()
                            cont.resume(incomingPacket?.getCurrentBuffer())
                        } else {
                            timeoutJob.cancel()
                            timeoutJob = GlobalScope.launch {
                                delay(NOTIFY_TIMEOUT.toLong())
                                hasResumed = true
                                centralCallback.removeListener(key)
                                cont.resume(null)
                            }
                        }

                        numberOfPackets++
                    }
                }
            })
        }
    }


    companion object : XYCreator() {
        const val TAG = "XyoBluetoothClient"
        const val FIRST_NOTIFY_TIMEOUT = 12_000
        const val NOTIFY_TIMEOUT = 10_000
        const val MAX_MTU = 512
        const val DEFAULT_MTU = 22

        const val DEVICE_TYPE_MASK = 0x3f.toByte()

        @SuppressLint("UseSparseArrays") //SparseArrays cannot use Byte as key
        val xyoManufactureIdToCreator = HashMap<Byte, XYCreator>()

        /**
         * Enable this device to be created on scan.
         *
         * @param enable Weather or not to enable the device.
         */
        fun enable(enable: Boolean) {
            if (enable) {
                serviceToCreator[XyoUuids.XYO_SERVICE] = this
                uuidToCreator[XyoUuids.XYO_SERVICE] = this
                XYBluetoothGattCallback.blockNotificationCallback = true
                XYIBeaconBluetoothDevice.enable(true)
            } else {
                serviceToCreator.remove(XyoUuids.XYO_SERVICE)
                uuidToCreator.remove(XyoUuids.XYO_SERVICE)
                XYIBeaconBluetoothDevice.enable(false)
                XYBluetoothGattCallback.blockNotificationCallback = false
            }
        }

        private fun majorFromScanResult(scanResult: XYScanResult): UShort? {
            val bytes = scanResult.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)
            return if (bytes != null) {
                val buffer = ByteBuffer.wrap(bytes)
                buffer.getShort(18).toUShort()
            } else {
                null
            }
        }

        private fun minorFromScanResult(scanResult: XYScanResult): UShort? {
            val bytes = scanResult.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)
            return if (bytes != null) {
                val buffer = ByteBuffer.wrap(bytes)
                buffer.getShort(20).toUShort()
            } else {
                null
            }
        }

        internal fun hashFromScanResult(scanResult: XYScanResult): String {
            val uuid = iBeaconUuidFromScanResult(scanResult)
            val major = majorFromScanResult(scanResult)
            val minor = minorFromScanResult(scanResult)

            return "$uuid:$major:$minor"
        }

        override fun getDevicesFromScanResult(
                context: Context,
                scanResult: XYScanResult,
                globalDevices: ConcurrentHashMap<String, XYBluetoothDevice>,
                foundDevices: HashMap<String,
                        XYBluetoothDevice>
        ) {
            val hash = hashFromScanResult(scanResult)


            val ad = scanResult.scanRecord?.getManufacturerSpecificData(0x4c)

            if (ad?.size == 23) {
                val id = ad[19]

                // masks the byte with 00111111
                if (xyoManufactureIdToCreator.containsKey(id and DEVICE_TYPE_MASK)) {
                    xyoManufactureIdToCreator[id and DEVICE_TYPE_MASK]?.getDevicesFromScanResult(context, scanResult, globalDevices, foundDevices)
                    return
                }
            }

            val createdDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                XyoBluetoothClient(context, scanResult, hash, BluetoothDevice.TRANSPORT_LE)
            } else {
                XyoBluetoothClient(context, scanResult, hash)
            }

            val foundDevice = foundDevices[hash]
            if (foundDevice != null) {
                foundDevice.rssi = scanResult.rssi
                foundDevice.updateBluetoothDevice(scanResult.device)
            } else {
                foundDevices[hash] = createdDevice
                globalDevices[hash] = createdDevice
            }
        }
    }
}
