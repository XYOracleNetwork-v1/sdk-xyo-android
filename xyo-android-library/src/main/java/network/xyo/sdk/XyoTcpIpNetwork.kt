package network.xyo.sdk

import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode

class XyoTcpIpNetwork(
    relayNode: XyoRelayNode,
    procedureCatalog: XyoProcedureCatalog,
    override val client:XyoClient = XyoTcpIpClient(relayNode, procedureCatalog, autoBoundWitness = false, autoBridge = false, acceptBridging = false),
    override val server:XyoServer = XyoTcpIpServer(relayNode, procedureCatalog, autoBridge = false, acceptBridging = false, listen = false)
) : XyoNetwork(Type.TcpIp) {
}
