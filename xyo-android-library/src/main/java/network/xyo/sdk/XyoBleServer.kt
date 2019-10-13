package network.xyo.sdk

class XyoBleServer(
    autoBridge: Boolean,
    acceptBridging: Boolean,
    listen: Boolean,
    override val payloadCallback: (() -> ByteArray)? = null
) : XyoServer() {

    override var autoBridge: Boolean
        get() {return false}
        set(value) {}

    override var acceptBridging: Boolean
        get() {return false}
        set(value) {}

    var listen: Boolean
        get() {return false}
        set(value) {}

    init {
        this.autoBridge = autoBridge
        this.acceptBridging = acceptBridging
        this.listen = listen
    }
}