package network.xyo.sdk
import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode

class XyoTcpIpServer(
    relayNode: XyoRelayNode,
    procedureCatalog: XyoProcedureCatalog,
    autoBridge: Boolean,
    acceptBridging: Boolean,
    listen: Boolean
) : XyoServer(relayNode, procedureCatalog) {

    override var autoBridge: Boolean
        get() { return false }
        set(_) { }

    override var acceptBridging: Boolean
        get() { return false }
        set(_) { }

    var listen: Boolean
        get() { return false }
        set(_) { }

    init {
        this.autoBridge = autoBridge
        this.acceptBridging = acceptBridging
        this.listen = listen
    }
}
