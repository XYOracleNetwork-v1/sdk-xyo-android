package network.xyo.modbluetoothkotlin.packet

class XyoInputStream {
    private val donePackets = ArrayList<ByteArray>()
    private var currentBuffer: XyoBluetoothIncomingPacket? = null
    var onComplete : ((done: ByteArray) -> Any?)? = null

    fun addChunk (data: ByteArray) {
        if (currentBuffer == null) {
            currentBuffer = XyoBluetoothIncomingPacket(data)

            if (currentBuffer?.done == true) {
                val finished = currentBuffer?.getCurrentBuffer() ?: return
                donePackets.add(finished)
                onDone()
                currentBuffer = null
            }

            return
        }

        val finished = currentBuffer?.addPacket(data) ?: return
        donePackets.add(finished)
        onDone()
        currentBuffer = null
    }

    private fun onDone () {
        val cb = onComplete

        if (cb != null && donePackets.size > 0) {
            cb(donePackets[0])
            donePackets.removeAt(0)
        }
    }

    fun getOldestPacket () : ByteArray? {
        if (donePackets.isEmpty()) {
            return null
        }

        val done = donePackets.first()

        donePackets.removeAt(0)

        return done
    }

}