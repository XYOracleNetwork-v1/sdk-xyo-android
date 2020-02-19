package network.xyo.sdk.bluetooth

import java.util.*

/**
 * All the XYO Bluetooth UUIDs.
 */
object XyoUuids {
    // The descriptor to use when to manage subscribing to notifications.
    val NOTIFY_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")!!

    // The primary GATT service that will be advertised.
    var XYO_SERVICE = UUID.fromString("d684352e-df36-484e-bc98-2d5398c5593e")!!

    // The GATT characteristic to be written to when creating pipes. This will be in the XYO_SERVICE.
    val XYO_PIPE = UUID.fromString("727a3639-0eb4-4525-b1bc-7fa456490b2d")!!

    // The password GATT characteristic to write to when changing the password.
    val XYO_PASSWORD = UUID.fromString("727a3639-0eb4-4525-b1bc-7fa4564A0b2d")!!

    // The password bound witness data characteristic characteristic to be written to.
    val XYO_CHANGE_BW_DATA = UUID.fromString("727a3639-0eb4-4525-b1bc-7fa4564B0b2d")!!

    val XYO_RESET_DEVICE = UUID.fromString("727a3639-0eb4-4525-b1bc-7fa4564C0b2d")!!
    val XYO_PUBLIC_KEY = UUID.fromString("727a3639-0eb4-4525-b1bc-7fa4564D0b2d")!!
}

