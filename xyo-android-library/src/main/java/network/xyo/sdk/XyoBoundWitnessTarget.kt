package network.xyo.sdk

import network.xyo.base.XYBase
import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode

abstract class XyoBoundWitnessTarget(
    val relayNode: XyoRelayNode,
    val procedureCatalog: XyoProcedureCatalog
): XYBase() {
    //this is where people provide additional payload data if they want.
    //we will need helpers to help people build the byte arrays
    abstract val payloadCallback: (() -> ByteArray)?

    //accept bound witnesses that have bridges payloads
    abstract var acceptBridging: Boolean

    //when auto bound witnessing, should we bridge our chain
    abstract var autoBridge: Boolean
}
