package network.xyo.sdk

import network.xyo.base.XYBase
import network.xyo.sdkcorekotlin.persist.XyoKeyValueStore

class XyoNode(val storage: XyoKeyValueStore, val networks: Map<String, XyoNetwork>): XYBase() {
    fun setAllListeners(name: String, listener: XyoBoundWitnessTarget.Listener) {
        networks.forEach {
            it.value.client.listeners[name] = listener
            it.value.server.listeners[name] = listener
        }
    }
}