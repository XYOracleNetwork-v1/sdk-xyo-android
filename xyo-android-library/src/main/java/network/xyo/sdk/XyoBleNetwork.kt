package network.xyo.sdk

import android.content.Context
import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode

@kotlin.ExperimentalUnsignedTypes
class XyoBleNetwork(
    context: Context,
    relayNode: XyoRelayNode,
    procedureCatalog: XyoProcedureCatalog,
    override val client:XyoClient = XyoBleClient(context, relayNode, procedureCatalog, autoBoundWitness = false, autoBridge = false, acceptBridging = false),
    override val server:XyoServer = XyoBleServer(relayNode, procedureCatalog, autoBridge = false, acceptBridging = false, listen = false)
) : XyoNetwork(Type.BluetoothLE) {
}
