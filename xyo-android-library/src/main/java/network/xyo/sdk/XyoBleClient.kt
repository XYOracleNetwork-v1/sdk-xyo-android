package network.xyo.sdk

import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import network.xyo.ble.generic.devices.XYBluetoothDevice
import network.xyo.ble.generic.gatt.peripheral.XYBluetoothResult
import network.xyo.ble.generic.scanner.XYSmartScan
import network.xyo.ble.generic.scanner.XYSmartScanModern
import network.xyo.modbluetoothkotlin.client.XyoBluetoothClient
import network.xyo.sdkcorekotlin.crypto.signing.ecdsa.secp256k.XyoSha256WithSecp256K
import network.xyo.sdkcorekotlin.network.XyoNetworkHandler
import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode
import java.util.concurrent.locks.ReentrantLock

@kotlin.ExperimentalUnsignedTypes
class XyoBleClient(
    context: Context,
    relayNode: XyoRelayNode,
    procedureCatalog: XyoProcedureCatalog,
    autoBridge: Boolean,
    acceptBridging: Boolean,
    val autoBoundWitness: Boolean,
    override val payloadCallback: (() -> ByteArray)? = null
)
    : XyoClient(relayNode, procedureCatalog) {

    override var autoBridge: Boolean
        get() {return false}
        set(value) {}

    override var acceptBridging: Boolean
        get() {return false}
        set(value) {}

    private var scanner: XYSmartScanModern
    private val boundWitnessLock = ReentrantLock()

    override var scan: Boolean
        get() {return scanner.started()}
        set(value) {
            GlobalScope.launch {
                if (value && !scanner.started()) {
                    scanner.start()
                } else if (!value && scanner.started()) {
                    scanner.stop()
                }
            }
        }

    private val scannerListener = object: XYSmartScan.Listener() {
        override fun entered(device: XYBluetoothDevice) {
            super.entered(device)
            if (autoBoundWitness) {
                if (device is XyoBluetoothClient) {
                    GlobalScope.launch {
                        tryBoundWitnessWithDevice(device)
                    }
                }
            }
        }
    }

    suspend fun tryBoundWitnessWithDevice(device: XyoBluetoothClient) {
        if (boundWitnessLock.tryLock()) {
            device.connection {
                val pipe = device.createPipe()

                if (pipe != null) {
                    val handler = XyoNetworkHandler(pipe)

                    val bw = relayNode.boundWitness(handler, procedureCatalog).await()
                    return@connection XYBluetoothResult(bw != null)
                }

                return@connection XYBluetoothResult(false)
            }
            boundWitnessLock.unlock()
        }
    }

    init {
        this.autoBridge = autoBridge
        this.acceptBridging = acceptBridging
        XyoBluetoothClient.enable(true)
        XyoSha256WithSecp256K.enable()
        this.scanner = XYSmartScanModern(context)
        this.scanner.addListener("xyo_client", this.scannerListener)
    }
}