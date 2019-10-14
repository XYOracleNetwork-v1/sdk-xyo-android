package network.xyo.sdk

import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode

abstract class XyoClient(relayNode: XyoRelayNode, procedureCatalog: XyoProcedureCatalog, open var autoBoundWitness: Boolean): XyoBoundWitnessTarget(relayNode, procedureCatalog) {
    abstract var scan: Boolean
}