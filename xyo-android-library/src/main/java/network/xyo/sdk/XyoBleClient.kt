package network.xyo.sdk

import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import network.xyo.ble.generic.devices.XYBluetoothDevice
import network.xyo.ble.generic.gatt.peripheral.XYBluetoothResult
import network.xyo.ble.generic.scanner.XYSmartScan
import network.xyo.ble.generic.scanner.XYSmartScanModern
import network.xyo.modbluetoothkotlin.client.XyoBluetoothClient
import network.xyo.sdkcorekotlin.crypto.signing.ecdsa.secp256k.XyoSha256WithSecp256K
import network.xyo.sdkcorekotlin.network.XyoNetworkHandler
import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

@kotlin.ExperimentalUnsignedTypes
class XyoBleClient(
    context: Context,
    relayNode: XyoRelayNode,
    procedureCatalog: XyoProcedureCatalog,
    autoBridge: Boolean,
    acceptBridging: Boolean,
    autoBoundWitness: Boolean,
    override var listener: Listener? = null
)
    : XyoClient(relayNode, procedureCatalog, autoBoundWitness) {

    override var autoBridge: Boolean = false
    override var acceptBridging: Boolean = false

    val minBWTimeGap = 10 * 1000

    var lastBoundWitnessTime = Date().time - minBWTimeGap //ten seconds ago

    var scanner: XYSmartScanModern

    private val boundWitnessMutex = Mutex()

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
            if (this@XyoBleClient.autoBoundWitness) {
                if (Date().time - lastBoundWitnessTime > minBWTimeGap) {
                    (device as? XyoBluetoothClient)?.let { device ->
                        GlobalScope.launch {
                            tryBoundWitnessWithDevice(device)
                        }
                    }
                }
            }
        }
    }

    suspend fun tryBoundWitnessWithDevice(device: XyoBluetoothClient) {
        if (boundWitnessMutex.tryLock()) {
            listener?.boundWitnessStarted()
            device.connection {
                val pipe = device.createPipe()

                if (pipe != null) {
                    val handler = XyoNetworkHandler(pipe)

                    val bw = relayNode.boundWitness(handler, procedureCatalog).await()
                    return@connection XYBluetoothResult(bw != null)
                }

                return@connection XYBluetoothResult(false)
            }
            listener?.boundWitnessCompleted()
            lastBoundWitnessTime = Date().time
            boundWitnessMutex.unlock()
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