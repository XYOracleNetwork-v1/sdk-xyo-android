package network.xyo.sdk

import network.xyo.base.XYBase

abstract class XyoNetwork(val type: Type): XYBase() {

    enum class Type {
        BluetoothLE,
        TcpIp,
        Other
    }

    abstract val client: XyoClient
    abstract val server: XyoServer
}