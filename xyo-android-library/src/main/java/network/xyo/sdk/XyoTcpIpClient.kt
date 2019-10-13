package network.xyo.sdk

import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode

class XyoTcpIpClient(
    relayNode: XyoRelayNode,
    procedureCatalog: XyoProcedureCatalog,
    autoBridge: Boolean,
    acceptBridging: Boolean,
    val autoBoundWitness: Boolean,
    override val payloadCallback: (() -> ByteArray)? = null
)
    : XyoClient(relayNode, procedureCatalog) {

    override var autoBridge: Boolean
        get() {return false}
        set(value) {}

    override var acceptBridging: Boolean
        get() {return false}
        set(value) {}

    init {
        this.autoBridge = autoBridge
        this.acceptBridging = acceptBridging
    }

    override var scan: Boolean
        get() {return false}
        set(value) {}
}