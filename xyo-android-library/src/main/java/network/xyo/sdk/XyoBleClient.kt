package network.xyo.sdk
import android.content.Context
import java.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import network.xyo.ble.generic.devices.XYBluetoothDevice
import network.xyo.ble.generic.gatt.peripheral.XYBluetoothResult
import network.xyo.ble.generic.scanner.XYSmartScan
import network.xyo.ble.generic.scanner.XYSmartScanModern
import network.xyo.sdk.bluetooth.client.XyoBluetoothClient
import network.xyo.sdk.bluetooth.client.XyoBridgeX
import network.xyo.sdk.bluetooth.client.XyoSentinelX
import network.xyo.sdkcorekotlin.boundWitness.XyoBoundWitness
import network.xyo.sdkcorekotlin.crypto.signing.ecdsa.secp256k.XyoSha256WithSecp256K
import network.xyo.sdkcorekotlin.network.XyoNetworkHandler
import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoNodeListener
import network.xyo.sdkcorekotlin.node.XyoRelayNode

@kotlin.ExperimentalUnsignedTypes
class XyoBleClient(
    context: Context,
    relayNode: XyoRelayNode,
    procedureCatalog: XyoProcedureCatalog,
    autoBridge: Boolean,
    acceptBridging: Boolean,
    autoBoundWitness: Boolean,
    scan: Boolean
) : XyoClient(relayNode, procedureCatalog, autoBoundWitness) {

    override var autoBridge: Boolean = false
    override var acceptBridging: Boolean = false
    var supportBridgeX = false
    var supportSentinelX = false
    var minimumRssi = -70

    val minBWTimeGap = 10 * 1000

    var lastBoundWitnessTime = Date().time - minBWTimeGap // ten seconds ago

    var scanner: XYSmartScanModern

    private val boundWitnessMutex = Mutex()

    override var scan: Boolean
        get() { return scanner.started() }
        set(value) {
            GlobalScope.launch {
                if (value && !scanner.started()) {
                    scanner.start()
                } else if (!value && scanner.started()) {
                    scanner.stop()
                }
            }
        }

    private val scannerListener = object : XYSmartScan.Listener() {
        override fun detected(device: XYBluetoothDevice) {
            super.detected(device)
            if (this@XyoBleClient.autoBoundWitness) {
                if (Date().time - lastBoundWitnessTime > minBWTimeGap) {
                    (device as? XyoBluetoothClient)?.let { client ->
                        device.rssi?.let { rssi ->
                            if (rssi < minimumRssi) {
                                log.info("Rssi too low: $rssi")
                                return
                            }
                            (client as? XyoBridgeX)?.let {
                                if (!supportBridgeX) {
                                    log.info("BridgeX not Supported: $rssi")
                                    return
                                }
                            }
                            (client as? XyoSentinelX)?.let {
                                if (!supportSentinelX) {
                                    log.info("SentinelX not Supported: $rssi")
                                    return
                                }
                            }
                            GlobalScope.launch {
                                tryBoundWitnessWithDevice(client)
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun tryBoundWitnessWithDevice(device: XyoBluetoothClient) {
        if (boundWitnessMutex.tryLock()) {
            boundWitnessStarted(device)

            var errorMessage: String? = null

            val result = device.connection {
                val pipe = device.createPipe()

                if (pipe != null) {
                    val handler = XyoNetworkHandler(pipe)

                    relayNode.addListener("tryBoundWitnessWithDevice", object : XyoNodeListener() {
                        override fun onBoundWitnessEndFailure(error: Exception?) {
                            errorMessage = error?.message ?: error?.toString() ?: "Unknown Error"
                        }
                    })

                    val bw = relayNode.boundWitness(handler, procedureCatalog).await()

                    relayNode.removeListener("tryBoundWitnessWithDevice")

                    return@connection XYBluetoothResult(bw)
                }

                return@connection XYBluetoothResult<XyoBoundWitness>(null)
            }
            val errorCode: String? =
                if (result.error != XYBluetoothResult.ErrorCode.None) {
                    result.error.toString()
                } else {
                    errorMessage
                }
            boundWitnessCompleted(device, result.value, errorCode)
            lastBoundWitnessTime = Date().time
            boundWitnessMutex.unlock()
        }
    }

    init {
        this.autoBridge = autoBridge
        this.acceptBridging = acceptBridging
        XyoBluetoothClient.enable(true)
        XyoBridgeX.enable(true)
        XyoSentinelX.enable(true)
        XyoSha256WithSecp256K.enable()
        this.scanner = XYSmartScanModern(context)
        this.scanner.addListener("xyo_client", this.scannerListener)
        this.scan = scan
    }
}
