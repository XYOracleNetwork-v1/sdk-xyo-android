package network.xyo.sdk
import android.content.Context
import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode

@kotlin.ExperimentalUnsignedTypes
class XyoBleNetwork(
    context: Context,
    relayNode: XyoRelayNode,
    procedureCatalog: XyoProcedureCatalog,
    override val client: XyoBleClient = XyoBleClient(
        context,
        relayNode,
        procedureCatalog,
        autoBoundWitness = true,
        autoBridge = true,
        acceptBridging = true,
        scan = true
    ),
    override val server: XyoBleServer = XyoBleServer(
        context,
        relayNode,
        procedureCatalog,
        autoBridge = false,
        acceptBridging = false,
        listen = false
    )
) : XyoNetwork(Type.BluetoothLE)
