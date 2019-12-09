package network.xyo.sdk
import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode

abstract class XyoClient(
    relayNode: XyoRelayNode,
    procedureCatalog: XyoProcedureCatalog,
    open var autoBoundWitness: Boolean,
    open var knownBridges: List<String>? = null
): XyoBoundWitnessTarget(relayNode, procedureCatalog) {
    //this is not a parameter since scanning has to start off of false
    open var scan: Boolean = false
}