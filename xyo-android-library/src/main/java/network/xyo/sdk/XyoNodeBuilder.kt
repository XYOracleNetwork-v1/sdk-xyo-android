package network.xyo.sdk

import android.content.Context
import network.xyo.base.XYBase
import network.xyo.sdkcorekotlin.hashing.XyoHash
import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.node.XyoRelayNode
import network.xyo.sdkcorekotlin.repositories.XyoBridgeQueueRepository
import network.xyo.sdkcorekotlin.repositories.XyoOriginBlockRepository
import network.xyo.sdkcorekotlin.repositories.XyoOriginChainStateRepository
import java.lang.Exception

@kotlin.ExperimentalUnsignedTypes
class XyoNodeBuilder: XYBase() {
    private var networks = mutableListOf<XyoNetwork>()
    private var storage: XyoStorage? = null

    private var relayNode: XyoRelayNode? = null
    private var procedureCatalog: XyoProcedureCatalog? = null
    private var blockRepository: XyoOriginBlockRepository? = null
    private var stateRepository: XyoOriginChainStateRepository? = null
    private var bridgeQueueRepository: XyoBridgeQueueRepository? = null
    private var hashingProvider: XyoHash.XyoHashProvider? = null

    fun addNetwork(network: XyoNetwork) {
        networks.add(network)
    }

    fun setStorage(storage: XyoStorage) {
        this.storage = storage
    }

    fun build(context: Context): XyoNode {
        if (XyoSdk.nodes.isNotEmpty()) {
            throw Exception()
        }

        if (networks.isEmpty()) {
            log.info("No networks specified, using default")
            setDefaultNetworks(context)
        }

        if (blockRepository == null) {
            log.info("No blockRepository specified, using default")
            setDefaultBlockRepository()
        }

        if (stateRepository == null) {
            log.info("No stateRepository specified, using default")
            setDefaultStateRepository()
        }

        if (bridgeQueueRepository == null) {
            log.info("No bridgeQueueRepository specified, using default")
            setDefaultBridgeQueueRepository()
        }

        if (hashingProvider == null) {
            log.info("No hashingProvider specified, using default")
            setDefaultHashingProvider()
        }

        if (procedureCatalog == null) {
            log.info("No procedureCatalog specified, using default")
            setDefaultProcedureCatalog()
        }

        if (relayNode == null) {
            log.info("No relayNode specified, using default")
            setDefaultRelayNode()
        }

        if (storage == null) {
            log.info("No storage specified, using default")
            setDefaultStorage()
        }

        val node = XyoNode(storage!!, networks.toTypedArray())
        XyoSdk.nodes.add(node)
        return node
    }

    private fun setDefaultProcedureCatalog() {

    }

    private fun setDefaultBlockRepository() {

    }

    private fun setDefaultStateRepository() {

    }

    private fun setDefaultBridgeQueueRepository() {

    }

    private fun setDefaultHashingProvider() {

    }

    private fun setDefaultRelayNode() {
        relayNode = XyoRelayNode(
            blockRepository!!,
            stateRepository!!,
            bridgeQueueRepository!!,
            hashingProvider!!
        )
    }

    private fun setDefaultNetworks(context: Context) {
        addNetwork(XyoBleNetwork(context, relayNode!!, procedureCatalog!!))
        addNetwork(XyoTcpIpNetwork(relayNode!!, procedureCatalog!!))
    }

    private fun setDefaultStorage() {
        setStorage(XyoStorage())
    }
}