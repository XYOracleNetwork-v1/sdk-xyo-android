package network.xyo.sdk

import network.xyo.base.XYBase
import java.lang.Exception

class XyoNodeBuilder: XYBase() {
    private var networks = mutableListOf<XyoNetwork>()
    private var storage: XyoStorage? = null

    fun addNetwork(network: XyoNetwork) {
        networks.add(network)
    }

    fun setStorage(storage: XyoStorage) {
        this.storage = storage
    }

    fun build(): XyoNode {
        if (XyoSdk.nodes.isNotEmpty()) {
            throw Exception()
        }

        if (networks.isEmpty()) {
            log.info("No networks specified, using default")
            setDefaultNetworks()
        }

        if (storage == null) {
            log.info("No storage specified, using default")
            setDefaultStorage()
        }

        val node = XyoNode(storage!!, networks.toTypedArray())
        XyoSdk.nodes.add(node)
        return node
    }

    private fun setDefaultNetworks() {
        addNetwork(XyoBleNetwork())
        addNetwork(XyoTcpIpNetwork())
    }

    private fun setDefaultStorage() {
        setStorage(XyoStorage())
    }
}