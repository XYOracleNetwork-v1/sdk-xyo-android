package network.xyo.sdk

import network.xyo.base.XYBase
import network.xyo.sdkcorekotlin.boundWitness.XyoBoundWitness
import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode

abstract class XyoBoundWitnessTarget(
    val relayNode: XyoRelayNode,
    val procedureCatalog: XyoProcedureCatalog
): XYBase() {

    open class Listener: XYBase() {
        open fun getPayloadData(): ByteArray {
            return byteArrayOf()
        }

        open fun boundWitnessStarted() {
            log.info("boundWitnessStarted")
        }

        open fun boundWitnessCompleted(boundWitness: XyoBoundWitness?, error:String?) {
            log.info("boundWitnessCompleted")
        }
    }

    //the interaction listener
    abstract var listener: Listener?

    //accept bound witnesses that have bridges payloads
    abstract var acceptBridging: Boolean

    //when auto bound witnessing, should we bridge our chain
    abstract var autoBridge: Boolean
}
