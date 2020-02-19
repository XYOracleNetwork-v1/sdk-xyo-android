package network.xyo.sdk.bluetooth.advertiser

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.ParcelUuid
import network.xyo.ble.generic.gatt.peripheral.XYBluetoothResult
import network.xyo.ble.generic.gatt.server.XYBluetoothAdvertiser
import network.xyo.ble.generic.gatt.server.XYIBeaconAdvertiseDataCreator
import network.xyo.sdk.bluetooth.XyoUuids
import java.nio.ByteBuffer

/**
 * A class for managing XYO advertising.
 *
 * @property major The device major to advertise
 * @property minor The device minor to advertise
 * @param advertiser The XY advertiser to advertise with.
 */
@kotlin.ExperimentalUnsignedTypes
class XyoBluetoothAdvertiser(
        private val major: UShort,
        private val minor: UShort,
        private val advertiser: XYBluetoothAdvertiser
) {

    private var includeName = false

    /**
     * Start a advertisement cycle
     */
    fun configureAdvertiser() {
        includeName = BluetoothAdapter.getDefaultAdapter().setName("Xyo")
        if (advertiser.isMultiAdvertisementSupported) {
            configureAdverserMulti()
            return
        }
        configureAdvertiserSingle()
    }

//    private fun getAdvertiseUuid (uuid: UUID, major: ByteArray, minor: ByteArray): UUID {
//        val uuidString = uuid.toString().dropLast(8)
//        val majorString = major.toHexString().drop(2)
//        val minorString = minor.toHexString().drop(2)
//
//        return UUID.fromString(  minorString + majorString + uuidString)
//    }

    private fun configureAdverserMulti() {
        val encodeMajor = ByteBuffer.allocate(2).putShort(major.toShort()).array()
        val encodedMinor = ByteBuffer.allocate(2).putShort(minor.toShort()).array()
        val advertiseData = XYIBeaconAdvertiseDataCreator.create(
                encodeMajor,
                encodedMinor,
                XyoUuids.XYO_SERVICE,
                APPLE_MANUFACTURER_ID,
                false
        ).build()

        val responseData = AdvertiseData.Builder()
                .setIncludeDeviceName(includeName)
                .addServiceUuid(ParcelUuid(
                        XyoUuids.XYO_SERVICE
                ))
                .build()

        advertiser.advertisingData = advertiseData
        advertiser.advertisingResponse = responseData
        advertiser.changeContactable(true)
        advertiser.changeAdvertisingMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        advertiser.changeAdvertisingTxLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
    }

    private fun configureAdvertiserSingle() {
        val advertiseData = AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid(XyoUuids.XYO_SERVICE))
                .setIncludeDeviceName(includeName)
                .build()

        advertiser.advertisingData = advertiseData
        advertiser.changeContactable(true)
        advertiser.changeAdvertisingMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        advertiser.changeAdvertisingTxLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
    }

    /**
     * Stop the current advertisement cycle
     */
    fun stopAdvertiser() {
        advertiser.stopAdvertising()
        _started = false
    }

    suspend fun startAdvertiser(): XYBluetoothResult<out Int>? {
        val result = advertiser.startAdvertising()
        _started = result?.error == XYBluetoothResult.ErrorCode.None
        return result
    }

    var _started: Boolean = false
    val started: Boolean
        get() { return _started }

    companion object {
        const val APPLE_MANUFACTURER_ID = 76
    }
}