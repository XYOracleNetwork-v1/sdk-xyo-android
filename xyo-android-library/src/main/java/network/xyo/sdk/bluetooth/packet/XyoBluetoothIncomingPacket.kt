package network.xyo.sdk.bluetooth.packet


import java.nio.ByteBuffer

/**
 * A class to help receive chucked data that came from XyoBluetoothOutgoingPacket.
 *
 * @param firstPacket The first chunk sent by the other party.
 */
class XyoBluetoothIncomingPacket(firstPacket: ByteArray) {
    private var packets = ArrayList<ByteArray>()
    private var currentSize = 0
    private var totalSize = 0

    /**
     * If there is no more packets to add.
     */
    val done: Boolean
        get() = (currentSize >= totalSize && totalSize != 0)

    /**
     * Adds a chunk to incoming packet.
     *
     * @param toAdd The chunk to add.
     * @return If the packet if finished, it will return the completed packet.
     */
    fun addPacket(toAdd: ByteArray): ByteArray? {
        if (totalSize == 0 && currentSize == 0) {
            totalSize = ByteBuffer.wrap(toAdd.copyOfRange(0, 4)).int
            packets.add(toAdd.copyOfRange(4, toAdd.size))
            currentSize += toAdd.size
            return null
        }

        packets.add(toAdd)
        currentSize += toAdd.size

        if (totalSize == currentSize) {
            return getCurrentBuffer()
        }

        return null
    }

    /**
     * Get the current packet buffer.
     */
    fun getCurrentBuffer(): ByteArray {
        val buff = ByteBuffer.allocate(currentSize - 4)

        for (i in 0 until packets.size) {
            buff.put(packets[i])
        }

        return buff.array()
    }

    init {
        addPacket(firstPacket)
    }
}