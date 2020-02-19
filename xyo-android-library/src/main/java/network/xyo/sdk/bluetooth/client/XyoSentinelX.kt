package network.xyo.sdk.bluetooth.client

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Build
import kotlinx.coroutines.*
import network.xyo.ble.devices.xy.XY4BluetoothDevice
import network.xyo.ble.devices.xy.XYFinderBluetoothDevice
import network.xyo.ble.firmware.XYBluetoothDeviceUpdate
import network.xyo.ble.firmware.XYOtaFile
import network.xyo.ble.firmware.XYOtaUpdate
import network.xyo.ble.generic.devices.XYBluetoothDevice
import network.xyo.ble.generic.devices.XYCreator
import network.xyo.ble.generic.gatt.peripheral.XYBluetoothResult
import network.xyo.ble.generic.scanner.XYScanResult
import network.xyo.ble.generic.services.standard.BatteryService
import network.xyo.ble.generic.services.standard.DeviceInformationService
import network.xyo.ble.services.dialog.SpotaService
import network.xyo.ble.services.xy.PrimaryService
import network.xyo.sdk.bluetooth.XyoUuids
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.experimental.and

@kotlin.ExperimentalUnsignedTypes
open class XyoSentinelX : XyoBluetoothClient {

    constructor(context: Context, scanResult: XYScanResult, hash: String) : super(context, scanResult, hash)

    constructor(context: Context, scanResult: XYScanResult, hash: String, transport: Int) : super(context, scanResult, hash, transport)

    private val sentinelListeners = HashMap<String, Listener>()
    private var lastButtonPressTime: Long = 0

    //Keep as public
    val batteryService = BatteryService(this)
    val primary = PrimaryService(this)
    val deviceInformationService = DeviceInformationService(this)
    val spotaService = SpotaService(this)

    fun addButtonListener(key: String, listener: Listener) {
        sentinelListeners[key] = listener
    }

    fun removeButtonListener(key: String) {
        sentinelListeners.remove(key)
    }

    fun isClaimed(): Boolean {
        val iBeaconData = scanResult?.scanRecord?.getManufacturerSpecificData(0x4c) ?: return true

        if (iBeaconData.size == 23) {
            val flags = iBeaconData[21]
            return flags and 1.toByte() != 0.toByte()
        }

        return true
    }

    private fun isButtonPressed(scanResult: XYScanResult): Boolean {
        val iBeaconData = scanResult.scanRecord?.getManufacturerSpecificData(0x4c) ?: return true

        if (iBeaconData.size == 23) {
            val flags = iBeaconData[21]
            return flags and 2.toByte() != 0.toByte()
        }

        return false
    }

    override fun onDetect(scanResult: XYScanResult?) {
        if (scanResult != null && isButtonPressed(scanResult) && lastButtonPressTime < System.currentTimeMillis() - 11_000) {
            // button of sentinel x is pressed
            lastButtonPressTime = System.currentTimeMillis()
            // TODO - added delay to allow listener attachment before calling it. onButtonPressed needs to be separate.
            CoroutineScope(Dispatchers.IO).launch {
                delay(1000)
                for ((_, l) in sentinelListeners) {
                    l.onButtonPressed()
                }
            }

            return
        }

        return
    }

    /**
     * Changes the password on the remote device if the current password is correct.
     * @param password The password of the device now.
     * @param newPassword The password to change on the remote device.
     * @return An XYBluetoothError if there was an issue writing the packet.
     */
    suspend fun changePassword(password: ByteArray, newPassword: ByteArray): XYBluetoothResult.ErrorCode {
        val encoded = ByteBuffer.allocate(2 + password.size + newPassword.size)
                .put((password.size + 1).toByte())
                .put(password)
                .put((newPassword.size + 1).toByte())
                .put(newPassword)
                .array()

        return chunkSend(encoded, XyoUuids.XYO_PASSWORD, XyoUuids.XYO_SERVICE, 1)
    }

    /**
     * Changes the bound witness data on the remote device
     * @param boundWitnessData The data to include in tche remote devices bound witness.
     * @param password The password of the device to so it can write the boundWitnessData
     * @return An XYBluetoothError if there was an issue writing the packet.
     */
    suspend fun changeBoundWitnessData(password: ByteArray, boundWitnessData: ByteArray): XYBluetoothResult.ErrorCode {
        val encoded = ByteBuffer.allocate(3 + password.size + boundWitnessData.size)
                .put((password.size + 1).toByte())
                .put(password)
                .putShort((boundWitnessData.size + 2).toShort())
                .put(boundWitnessData)
                .array()

        return chunkSend(encoded, XyoUuids.XYO_CHANGE_BW_DATA, XyoUuids.XYO_SERVICE, 4)
    }

    suspend fun getBoundWitnessData(): XYBluetoothResult<ByteArray> {
        return findAndReadCharacteristicBytes(XyoUuids.XYO_SERVICE, XyoUuids.XYO_CHANGE_BW_DATA)
    }

    /**
     * Reset the device.
     * @param password The password of the device to so it can write the boundWitnessData
     * @return An XYBluetoothError if there was an issue writing the packet.
     */
    suspend fun resetDevice(password: ByteArray): XYBluetoothResult<ByteArray> {
        val msg = ByteBuffer.allocate(password.size + 2)
                .put((password.size + 2).toByte())
                .put((password.size + 1).toByte())
                .put(password)
                .array()

        return findAndWriteCharacteristic(
                XyoUuids.XYO_SERVICE,
                XyoUuids.XYO_RESET_DEVICE,
                msg,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
    }

    /**
     * Lock the device.
     */
    suspend fun lock() = connection {
        return@connection primary.lock.set(XY4BluetoothDevice.DefaultLockCode)
    }

    /**
     * Unlock the device
     */
    suspend fun unlock() = connection {
        return@connection primary.unlock.set(XY4BluetoothDevice.DefaultLockCode)
    }

    suspend fun stayAwake() = connection {
        return@connection primary.stayAwake.set(XYFinderBluetoothDevice.StayAwake.On.state)
    }

    suspend fun fallAsleep() = connection {
        return@connection primary.stayAwake.set(XYFinderBluetoothDevice.StayAwake.Off.state)
    }

    suspend fun batteryLevel() = connection {
        return@connection batteryService.level.get()
    }

    /**
     * Firmware Update
     * @param fileByteArray the firmware as a ByteArray
     * @param listener listener for progress, failed, completed
     */
    fun updateFirmware(fileByteArray: ByteArrayInputStream, listener: XYOtaUpdate.Listener) {
        val otaFile = fileByteArray.let { XYOtaFile.getByStream(it) }
        val updater = XYBluetoothDeviceUpdate(spotaService, this, otaFile)
        updater.addListener("SentinelXDevice", listener)
        updater.start()
    }

    companion object : XYCreator() {

        open class Listener {
            open fun onButtonPressed() {}
        }

        fun enable(enable: Boolean) {
            if (enable) {
                xyoManufactureIdToCreator[XyoBluetoothClientDeviceType.SentinelX.raw] = this
            } else {
                xyoManufactureIdToCreator.remove(XyoBluetoothClientDeviceType.SentinelX.raw)
            }
        }

        override fun getDevicesFromScanResult(
                context: Context,
                scanResult: XYScanResult,
                globalDevices: ConcurrentHashMap<String, XYBluetoothDevice>,
                foundDevices: HashMap<String,
                        XYBluetoothDevice>
        ) {
            val hash = hashFromScanResult(scanResult)
            val createdDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                XyoSentinelX(context, scanResult, hash, BluetoothDevice.TRANSPORT_LE)
            } else {
                XyoSentinelX(context, scanResult, hash)
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