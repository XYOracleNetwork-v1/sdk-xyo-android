package network.xyo.sdk

import network.xyo.base.XYBase
import network.xyo.sdkcorekotlin.persist.XyoKeyValueStore

class XyoNode(val storage: XyoKeyValueStore, val networks: Array<XyoNetwork>): XYBase() {

}