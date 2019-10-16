package network.xyo.sdk

import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode

class XyoTcpIpClient(
    relayNode: XyoRelayNode,
    procedureCatalog: XyoProcedureCatalog,
    autoBridge: Boolean,
    acceptBridging: Boolean,
    autoBoundWitness: Boolean
)
    : XyoClient(relayNode, procedureCatalog, autoBoundWitness) {

    override var autoBridge: Boolean
        get() {return false}
        set(_) {}

    override var acceptBridging: Boolean
        get() {return false}
        set(_) {}

    init {
        this.autoBridge = autoBridge
        this.acceptBridging = acceptBridging
    }

    override var scan: Boolean
        get() {return false}
        set(_) {}
}