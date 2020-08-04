package network.xyo.sdk
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import network.xyo.base.XYBase
import network.xyo.sdkcorekotlin.crypto.signing.XyoSigner
import network.xyo.sdkcorekotlin.crypto.signing.ecdsa.secp256k.XyoSha256WithSecp256K
import network.xyo.sdkcorekotlin.hashing.XyoBasicHashBase
import network.xyo.sdkcorekotlin.hashing.XyoHash
import network.xyo.sdkcorekotlin.heuristics.XyoHeuristicGetter
import network.xyo.sdkcorekotlin.heuristics.XyoUnixTime
import network.xyo.sdkcorekotlin.network.XyoProcedureCatalog
import network.xyo.sdkcorekotlin.network.XyoProcedureCatalogFlags
import network.xyo.sdkcorekotlin.node.XyoRelayNode
import network.xyo.sdkcorekotlin.persist.XyoKeyValueStore
import network.xyo.sdkcorekotlin.persist.repositories.XyoStorageBridgeQueueRepository
import network.xyo.sdkcorekotlin.persist.repositories.XyoStorageOriginBlockRepository
import network.xyo.sdkcorekotlin.persist.repositories.XyoStorageOriginStateRepository
import network.xyo.sdkcorekotlin.schemas.XyoSchemas
import network.xyo.sdkobjectmodelkotlin.structure.XyoIterableStructure
import network.xyo.sdkobjectmodelkotlin.structure.XyoObjectStructure
import java.nio.ByteBuffer


@kotlin.ExperimentalUnsignedTypes
class XyoNodeBuilder : XYBase() {
    private var networks = mutableMapOf<String, XyoNetwork>()
    private var storage: XyoKeyValueStore? = null
    private var listener: XyoBoundWitnessTarget.Listener? = null

    private var relayNode: XyoRelayNode? = null
    private var procedureCatalog: XyoProcedureCatalog? = null
    private var blockRepository: XyoStorageOriginBlockRepository? = null
    private var stateRepository: XyoStorageOriginStateRepository? = null
    private var bridgeQueueRepository: XyoStorageBridgeQueueRepository? = null
    private var hashingProvider: XyoHash.XyoHashProvider? = null
    lateinit var node: XyoNode
    private var knownBridges = mutableListOf<String>()

    fun addNetwork(name: String, network: XyoNetwork) {
        networks[name] = network
    }

    fun setStorage(storage: XyoKeyValueStore) {
        this.storage = storage
    }

    fun setListener(listener: XyoBoundWitnessTarget.Listener) {
        this.listener = listener
    }

    suspend fun build(context: Context): XyoNode {
        if (XyoSdk.nodes.isNotEmpty()) {
            throw Exception()
        }

        if (storage == null) {
            log.info("No storage specified, using default")
            setDefaultStorage(context)
        }

        if (hashingProvider == null) {
            log.info("No hashingProvider specified, using default")
            setDefaultHashingProvider()
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

        if (procedureCatalog == null) {
            log.info("No procedureCatalog specified, using default")
            setDefaultProcedureCatalog()
        }

        if (relayNode == null) {
            log.info("No relayNode specified, using default")
            setDefaultRelayNode()
        }

        if (networks.isEmpty()) {
            log.info("No networks specified, using default")
            setDefaultNetworks(context)
        }

        val node = XyoNode(networks)
        XyoSdk.nodes.add(node)

        restoreAndInitBlockStorage()

        listener?.let {
            node.setAllListeners("default", it)
        }

        return node
    }

    private fun setDefaultProcedureCatalog() {
        /*procedureCatalog = object : XyoProcedureCatalog {
            override fun canDo(byteArray: ByteArray): Boolean {
                if (true) {
                    return true
                }

                return ByteBuffer.wrap(byteArray).int and 1 != 0
            }

            override fun choose(byteArray: ByteArray): ByteArray {
                return byteArrayOf(0x00, 0x00, 0x00, 0x01)
            }

            override fun getEncodedCanDo(): ByteArray {
                if (true) {
                    return byteArrayOf(0x00, 0x00, 0x00, 0xff.toByte())
                }

                return byteArrayOf(0x00, 0x00, 0x00, 0x01)
            }
        } */
        procedureCatalog = object : XyoProcedureCatalog {
            val canDoByte = XyoProcedureCatalogFlags.BOUND_WITNESS or XyoProcedureCatalogFlags.GIVE_ORIGIN_CHAIN or XyoProcedureCatalogFlags.TAKE_ORIGIN_CHAIN

            override fun canDo(byteArray: ByteArray): Boolean {
                if (byteArray.isEmpty()) {
                    return false
                }

                return byteArray.last().toInt() and canDoByte != 0
            }

            override fun choose(byteArray: ByteArray): ByteArray {
                if (byteArray.isEmpty()) {
                    return byteArrayOf(XyoProcedureCatalogFlags.BOUND_WITNESS.toByte())
                }

                val interestedIn = byteArray.last().toInt()

                if (interestedIn and XyoProcedureCatalogFlags.GIVE_ORIGIN_CHAIN != 0) {
                    return byteArrayOf(XyoProcedureCatalogFlags.TAKE_ORIGIN_CHAIN.toByte())
                }

                if (interestedIn and XyoProcedureCatalogFlags.TAKE_ORIGIN_CHAIN != 0) {
                    return byteArrayOf(XyoProcedureCatalogFlags.GIVE_ORIGIN_CHAIN.toByte())
                }

                return byteArrayOf(XyoProcedureCatalogFlags.BOUND_WITNESS.toByte())
            }

            override fun getEncodedCanDo(): ByteArray {

                return byteArrayOf(0x00, 0x00, 0x00, canDoByte.toByte())
            }
        }
    }

    private fun setDefaultBlockRepository() {
        storage?.let { storage ->
            hashingProvider?.let { hashingProvider ->
                blockRepository = XyoStorageOriginBlockRepository(storage, hashingProvider)
                return
            }
            log.error("Missing hashingProvider", true)
            return
        }
        log.error("Missing storage", true)
    }

    private fun setDefaultStateRepository() {
        storage?.let { storage ->
            stateRepository = XyoStorageOriginStateRepository(storage)
            return
        }
        log.error("Missing storage", true)
    }

    private fun setDefaultBridgeQueueRepository() {
        storage?.let { storage ->
            bridgeQueueRepository = XyoStorageBridgeQueueRepository(storage)
            return
        }
        log.error("Missing storage", true)
    }

    private fun setDefaultHashingProvider() {
        hashingProvider = XyoBasicHashBase.createHashType(XyoSchemas.SHA_256, "SHA-256")
    }

    private fun setDefaultRelayNode() {
        blockRepository?.let { blockRepository ->
            stateRepository?.let { stateRepository ->
                bridgeQueueRepository?.let { bridgeQueueRepository ->
                    hashingProvider?.let { hashingProvider ->
                        relayNode = XyoRelayNode(
                            blockRepository,
                            stateRepository,
                            bridgeQueueRepository,
                            hashingProvider
                        )
                        return
                    }
                    log.error("Missing hashingProvider", true)
                    return
                }
                log.error("Missing bridgeQueueRepository", true)
                return
            }
            log.error("Missing stateRepository", true)
            return
        }
        log.error("Missing blockRepository", true)
    }

    private suspend fun getSigner(): XyoSigner {
        storage!!.let { storage ->
            val currentSigner = storage.read(SIGNER_KEY).await()

            if (currentSigner == null) {
                val newSigner = XyoSha256WithSecp256K.newInstance()
                storage.write(SIGNER_KEY, newSigner.privateKey.bytesCopy).await()
                return newSigner
            }

            return XyoSha256WithSecp256K.newInstance(currentSigner)
        }
    }



    private suspend fun restoreAndInitBlockStorage() {
        relayNode!!.let { relayNode ->
            relayNode.originBlocksToBridge.removeWeight = 2
            relayNode.originBlocksToBridge.sendLimit = 38

            relayNode.addHeuristic("TIME", XyoUnixTime.getter)

            val currentSigner = getSigner()

            stateRepository!!.restore(arrayListOf(currentSigner)).await()
            bridgeQueueRepository!!.restore().await()

            if (ByteBuffer.wrap((relayNode.originState.index.valueCopy)).int == 0) {
                relayNode.selfSignOriginChain().await()
            }
        }
    }

    private fun setDefaultNetworks(context: Context) {
        relayNode?.let { relayNode ->
            procedureCatalog?.let { procedureCatalog ->
                addNetwork("ble", XyoBleNetwork(context, relayNode, procedureCatalog))
                addNetwork("tcpip", XyoTcpIpNetwork(relayNode, procedureCatalog))
                return
            }
            log.error("Missing procedureCatalog", true)
            return
        }
        log.error("Missing relayNode", true)
    }

    private fun setDefaultStorage(context: Context) {
        setStorage(XyoSnappyDBStorageProvider(context))
    }

    companion object {
        private val SIGNER_KEY = "SIGNER_KEY".toByteArray()
    }
}
