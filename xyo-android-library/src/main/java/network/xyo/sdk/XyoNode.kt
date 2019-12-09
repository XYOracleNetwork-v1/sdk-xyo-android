package network.xyo.sdk
import network.xyo.base.XYBase

class XyoNode(val networks: Map<String, XyoNetwork>): XYBase() {
    fun setAllListeners(name: String, listener: XyoBoundWitnessTarget.Listener) {
        networks.forEach {
            it.value.client.listeners[name] = listener
            it.value.server.listeners[name] = listener
        }
    }
}