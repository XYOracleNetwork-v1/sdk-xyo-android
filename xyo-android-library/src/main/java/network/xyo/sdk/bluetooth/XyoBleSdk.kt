package network.xyo.sdk.bluetooth

import android.content.Context
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import network.xyo.ble.generic.gatt.server.XYBluetoothAdvertiser
import network.xyo.ble.generic.gatt.server.XYBluetoothGattServer
import network.xyo.sdk.bluetooth.advertiser.XyoBluetoothAdvertiser
import network.xyo.sdk.bluetooth.server.XyoBluetoothServer
import java.util.*

@kotlin.ExperimentalUnsignedTypes
class XyoBleSdk {
    @InternalCoroutinesApi
    companion object {
        private var server: XyoBluetoothServer? = null
        private var advertiser: XyoBluetoothAdvertiser? = null
        private val initServerMutex = Mutex(false)
        private val initAdvertiserMutex = Mutex(false)

        private fun createNewAdvertiser(context: Context, major: UShort?, minor: UShort?): XyoBluetoothAdvertiser {
            val newAdvertiser = XyoBluetoothAdvertiser(
                    major ?: Random().nextInt(Short.MAX_VALUE * 2 + 1).toUShort(),
                    minor ?: Random().nextInt(Short.MAX_VALUE * 2 + 1).toUShort(),
                    XYBluetoothAdvertiser(context))
            newAdvertiser.configureAdvertiser()
            advertiser = newAdvertiser
            return newAdvertiser
        }

        private suspend fun initServer(context: Context): XyoBluetoothServer {
            val newServer =  XyoBluetoothServer(XYBluetoothGattServer(context))
            newServer.initServer()
            server = newServer
            return newServer
        }

        suspend fun server(context: Context): XyoBluetoothServer {
            initServerMutex.lock(this)
            val result = server ?: initServer(context)
            initServerMutex.unlock(this)
            return result
        }

        suspend fun advertiser(context: Context, major: UShort? = null, minor: UShort? = null): XyoBluetoothAdvertiser {
            initAdvertiserMutex.lock(this)
            val result = advertiser ?: createNewAdvertiser(context, major, minor)
            initAdvertiserMutex.unlock(this)
            return result
        }
    }
}