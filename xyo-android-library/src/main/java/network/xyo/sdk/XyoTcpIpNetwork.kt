package network.xyo.sdk
import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode

@kotlin.ExperimentalUnsignedTypes
class XyoTcpIpNetwork(
    relayNode: XyoRelayNode,
    procedureCatalog: XyoProcedureCatalog,
    override val client: XyoTcpIpClient = XyoTcpIpClient(
        relayNode,
        procedureCatalog,
        autoBoundWitness = true,
        autoBridge = true,
        acceptBridging = true
    ),
    override val server: XyoTcpIpServer = XyoTcpIpServer(
        relayNode,
        procedureCatalog,
        autoBridge = false,
        acceptBridging = false,
        listen = false
    )
) : XyoNetwork(Type.TcpIp) {
    init {
        client.knownBridges = client.knownBridges ?: listOf("ws://alpha-peers.xyo.network:11000","ws://54.89.226.87:11000")
    }
}
