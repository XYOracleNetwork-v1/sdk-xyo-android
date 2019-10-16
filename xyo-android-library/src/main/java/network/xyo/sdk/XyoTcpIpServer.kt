package network.xyo.sdk

import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode

class XyoTcpIpServer(
    relayNode: XyoRelayNode,
    procedureCatalog: XyoProcedureCatalog,
    autoBridge: Boolean,
    acceptBridging: Boolean,
    listen: Boolean,
    override var listener: Listener? = null
) : XyoServer(relayNode, procedureCatalog) {

    override var autoBridge: Boolean
        get() {return false}
        set(value) {}

    override var acceptBridging: Boolean
        get() {return false}
        set(value) {}

    var listen: Boolean
        get() {return false}
        set(value) {}

    init {
        this.autoBridge = autoBridge
        this.acceptBridging = acceptBridging
        this.listen = listen
    }
}