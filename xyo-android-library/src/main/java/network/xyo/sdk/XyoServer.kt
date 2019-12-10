package network.xyo.sdk
import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode

abstract class XyoServer(relayNode: XyoRelayNode, procedureCatalog: XyoProcedureCatalog) : XyoBoundWitnessTarget(relayNode, procedureCatalog)
