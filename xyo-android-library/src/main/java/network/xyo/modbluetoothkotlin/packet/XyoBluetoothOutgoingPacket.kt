package network.xyo.modbluetoothkotlin.packet

import java.nio.ByteBuffer

/**
 * A class to chunk bluetooth data when writing to a GATT.
 *
 * @param chunkSize The number of bytes per chunk.
 * @param bytes The bytes to chunk.
 * @param sizeOfSize The number of bytes to prepend the size with
 */
class XyoBluetoothOutgoingPacket(private val chunkSize: Int, bytes: ByteArray, private val sizeOfSize: Int) {
    private var currentIndex = 0
    private val sizeWithBytes = getSizeWithBytes(bytes)

    private fun getSizeWithBytes(bytes: ByteArray): ByteArray {
        val buff = ByteBuffer.allocate(bytes.size + sizeOfSize)

        when (sizeOfSize) {
            1 -> buff.put((bytes.size + 1).toByte())
            2 -> buff.putShort((bytes.size + 2).toShort())
            4 -> buff.putInt(bytes.size + 4)
        }

        buff.put(bytes)
        return buff.array()
    }

    /**
     * If there are more packets to send.
     */
    val canSendNext: Boolean
        get() {
            return sizeWithBytes.size != currentIndex
        }


    /**
     * Gets the next packet to send.
     */
    fun getNext(): ByteArray {
        var size = Math.min(chunkSize, (sizeWithBytes.size - currentIndex))
        //possible to get a negative size -- will cause NegativeArraySizeException
        if (size < 0 ) {
            size = 0
        }

        val packet = ByteArray(size)

        for (i in packet.indices) {
            packet[i] = sizeWithBytes[currentIndex]
            currentIndex++
        }

        return packet
    }
}