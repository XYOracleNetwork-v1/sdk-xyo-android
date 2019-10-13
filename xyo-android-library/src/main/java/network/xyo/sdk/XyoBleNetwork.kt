package network.xyo.sdk

class XyoBleNetwork(
    override val client:XyoClient = XyoBleClient(autoBoundWitness = false, autoBridge = false, acceptBridging = false),
    override val server:XyoServer = XyoBleServer(autoBridge = false, acceptBridging = false, listen = false)
) : XyoNetwork(Type.BluetoothLE) {
}
