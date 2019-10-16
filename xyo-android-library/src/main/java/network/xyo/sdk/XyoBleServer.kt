package network.xyo.sdk

import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.xyo.modbluetoothkotlin.XyoBleSdk
import network.xyo.modbluetoothkotlin.advertiser.XyoBluetoothAdvertiser
import network.xyo.modbluetoothkotlin.server.XyoBluetoothServer
import network.xyo.sdkcorekotlin.network.XyoNetworkHandler
import network.xyo.sdkcorekotlin.network.XyoNetworkPipe
import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode

@kotlin.ExperimentalUnsignedTypes
class XyoBleServer(
    context: Context,
    relayNode: XyoRelayNode,
    procedureCatalog: XyoProcedureCatalog,
    autoBridge: Boolean,
    acceptBridging: Boolean,
    listen: Boolean,
    override var listener: Listener? = null
) : XyoServer(relayNode, procedureCatalog) {

    override var autoBridge: Boolean = false
    override var acceptBridging: Boolean = false

    var advertiser: XyoBluetoothAdvertiser? = null
    lateinit var server: XyoBluetoothServer

    var listen: Boolean
        get() {return advertiser?.started ?: false}
        set(value) {
            GlobalScope.launch {
                var waitForAdvertiserCount = 10
                while (advertiser == null && waitForAdvertiserCount > 0) {
                    delay(1000)
                    waitForAdvertiserCount--
                }
                advertiser?.let { advertiser ->
                    if (value) {
                        log.info("Starting Advertiser")
                        advertiser.startAdvertiser()
                    } else {
                        log.info("Stopping Advertiser")
                        advertiser.stopAdvertiser()
                    }
                    return@launch
                }
                log.error("Advertiser Failed to Initialize", false)
            }
        }

    init {
        this.autoBridge = autoBridge
        this.acceptBridging = acceptBridging
        this.listen = listen
        GlobalScope.launch {
            initServer(context)
        }
    }

    private suspend fun initServer(context: Context): Boolean {
        advertiser = XyoBleSdk.advertiser(context)
        server = XyoBleSdk.server(context)
        server.listener = object: XyoBluetoothServer.Listener {
            override fun onPipe(pipe: XyoNetworkPipe) {
                log.info("onPipe")
                GlobalScope.launch {
                    listener?.boundWitnessStarted()
                    val handler = XyoNetworkHandler(pipe)
                    val bw = relayNode.boundWitness(handler, procedureCatalog).await()
                    listener?.boundWitnessCompleted(bw, null)
                    return@launch
                }
            }
        }
        log.info("Initialized Server")
        return true
    }
}