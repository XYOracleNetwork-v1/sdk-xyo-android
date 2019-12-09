package network.xyo.sdk
import network.xyo.base.XYBase
import network.xyo.sdkcorekotlin.boundWitness.XyoBoundWitness
import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode

abstract class XyoBoundWitnessTarget(
    val relayNode: XyoRelayNode,
    val procedureCatalog: XyoProcedureCatalog
): XYBase() {

    val publicKey: String?
        get() {
            if (relayNode.originState.signers.isEmpty()) {
                return null
            }

            return relayNode.originState.signers.first().publicKey.bytesCopy.toBase58String()
        }

    open class Listener: XYBase() {
        open fun boundWitnessStarted(source: Any?, target: XyoBoundWitnessTarget) {
            log.info("boundWitnessStarted")
        }

        open fun boundWitnessCompleted(source: Any?, target: XyoBoundWitnessTarget, boundWitness: XyoBoundWitness?, error:String?) {
            log.info("boundWitnessCompleted")
        }
    }

    //the interaction listener
    val listeners = mutableMapOf<String, Listener>()

    fun boundWitnessStarted(source: Any?) {
        listeners.forEach {
            it.value.boundWitnessStarted(source, this)
        }
    }

    fun boundWitnessCompleted(source: Any?, boundWitness: XyoBoundWitness?, error:String?) {
        listeners.forEach {
            it.value.boundWitnessCompleted(source, this, boundWitness, error)
        }
    }

    //accept bound witnesses that have bridges payloads
    abstract var acceptBridging: Boolean

    //when auto bound witnessing, should we bridge our chain
    abstract var autoBridge: Boolean
}
