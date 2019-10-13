package network.xyo.sdk

class XyoTcpIpClient(
    autoBridge: Boolean,
    acceptBridging: Boolean,
    val autoBoundWitness: Boolean,
    override val payloadCallback: (() -> ByteArray)? = null
)
    : XyoClient() {

    override var autoBridge: Boolean
        get() {return false}
        set(value) {}

    override var acceptBridging: Boolean
        get() {return false}
        set(value) {}

    init {
        this.autoBridge = autoBridge
        this.acceptBridging = acceptBridging
    }
}