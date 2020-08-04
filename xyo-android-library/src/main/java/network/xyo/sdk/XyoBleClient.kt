@file:Suppress("SpellCheckingInspection")

package network.xyo.sdk
import android.content.Context
import android.util.Log
import java.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import network.xyo.ble.generic.devices.XYBluetoothDevice
import network.xyo.ble.generic.gatt.peripheral.XYBluetoothResult
import network.xyo.ble.generic.scanner.XYSmartScan
import network.xyo.ble.generic.scanner.XYSmartScanModern
import network.xyo.sdk.bluetooth.client.*
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

    override var autoBridge: Boolean = true
    override var acceptBridging: Boolean = true

    override var deviceCount = 0
    override var xyoDeviceCount = 0
    override var nearbyXyoDeviceCount = 0

    var supportBridgeX = true
    var supportSentinelX = true
    var minimumRssi = -95

    val minBWTimeGap = 10 * 900

    var lastBoundWitnessTime = Date().time - minBWTimeGap // five seconds ago

    private var scanner: XYSmartScanModern

    private val boundWitnessMutex = Mutex()

    override var scan: Boolean
        get() {
//            val tag = "TAG "
//            Log.i(tag, "Does this scan??")
            return scanner.started()
        }
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
        //override fun onStart
        override fun entered(device: XYBluetoothDevice) {
            val tag = "TAG "
            super.entered(device)
            deviceCount++
            Log.i(tag,"Xyo Device Entered: ${device.id}")
            xyoDeviceCount++
        }

        override fun exited(device: XYBluetoothDevice) {
            val tag = "TAG "
            super.exited(device)
            deviceCount--
            Log.i(tag, "Xyo Device Exited: ${device.id}")
            xyoDeviceCount--
        }
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
                                log.info("BridgeX rssi: $rssi");
                                if (!supportBridgeX) {
                                    log.info("BridgeX not Supported: $rssi")
                                    return
                                }
                            }
                            (client as? XyoSentinelX)?.let {
                                log.info("SenX rssi: $rssi");
                                if (!supportSentinelX) {
                                    log.info("SentinelX not Supported: $rssi")
                                    return
                                }
                            }
                            nearbyXyoDeviceCount++
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
        XyoIosAppX.enable(true)
        XyoAndroidAppX.enable(true)
        XyoSha256WithSecp256K.enable()
        this.scanner = XYSmartScanModern(context)
        this.scanner.addListener("xyo_client", this.scannerListener)
        this.scan = scan
    }
}
