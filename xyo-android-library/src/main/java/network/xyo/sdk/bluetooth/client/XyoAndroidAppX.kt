package network.xyo.sdk.bluetooth.client

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import network.xyo.ble.generic.devices.XYBluetoothDevice
import network.xyo.ble.generic.devices.XYCreator
import network.xyo.ble.generic.scanner.XYScanResult
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@kotlin.ExperimentalUnsignedTypes
open class XyoAndroidAppX: XyoBluetoothClient{

    constructor(context: Context, scanResult: XYScanResult, hash: String) : super(context, scanResult, hash)

    constructor(context: Context, scanResult: XYScanResult, hash: String, transport: Int) : super(context, scanResult, hash, transport)

    companion object : XYCreator() {

        fun enable(enable: Boolean) {
            if (enable) {
                xyoManufactureIdToCreator[XyoBluetoothClientDeviceType.AndroidAppX.raw] = this
            } else {
                xyoManufactureIdToCreator.remove(XyoBluetoothClientDeviceType.AndroidAppX.raw)
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
                XyoAndroidAppX(context, scanResult, hash, BluetoothDevice.TRANSPORT_LE)
            } else {
                XyoAndroidAppX(context, scanResult, hash)
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