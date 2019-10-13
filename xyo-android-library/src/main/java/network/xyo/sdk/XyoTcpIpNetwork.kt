package network.xyo.sdk

class XyoTcpIpNetwork(
    override val client:XyoClient = XyoTcpIpClient(autoBoundWitness = false, autoBridge = false, acceptBridging = false),
    override val server:XyoServer = XyoTcpIpServer(autoBridge = false, acceptBridging = false, listen = false)
) : XyoNetwork(Type.TcpIp) {
}
