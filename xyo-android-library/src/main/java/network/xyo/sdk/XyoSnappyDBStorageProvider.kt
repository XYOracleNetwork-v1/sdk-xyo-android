package network.xyo.sdk
import android.content.Context
import android.util.Base64
import com.snappydb.DB
import com.snappydb.DBFactory
import com.snappydb.SnappydbException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import network.xyo.sdkcorekotlin.persist.XyoKeyValueStore
import network.xyo.sdkcorekotlin.persist.XyoStorageException

/**
 * A key value store implementation of the XyoStorageProviderInterface, in android using SnappyDB.
 *
 * For more information about SnappyDB: http://snappydb.com/
 *
 * @param context The android context to open the SnappyDB with.
 */
open class XyoSnappyDBStorageProvider(private var context: Context) : XyoKeyValueStore {

    private val db = DBFactory.open(context)
    // TODO -DB is not being closed
    // TODO - use sync blocks - snappyDB is not thread safe
    // TODO - move save functions here instead of using getDB ?

    // get an instance of the opened DB
    fun getDB(): DB? {
        return db
    }

    override suspend fun containsKey(key: ByteArray): Boolean {
        try {
            return db.exists(makeKey(key))
        } catch (dbException: SnappydbException) {
            throw XyoStorageException("Failed to read: $dbException")
        }
    }

    override suspend fun delete(key: ByteArray) {
        try {
            db.del(makeKey(key))
        } catch (dbException: SnappydbException) {
            throw XyoStorageException("Failed to delete: $dbException")
        }
    }

    override suspend fun getAllKeys(): Iterator<ByteArray> {
        try {
            val i = db.allKeysIterator()

            return object : Iterator<ByteArray> {
                override fun hasNext(): Boolean {
                    val hasNext = i.hasNext()

                    if (!hasNext) {
                        i.close()
                    }

                    return hasNext
                }

                override fun next(): ByteArray {
                    return getKey(i.next(1)[0])
                }
            }
        } catch (dbException: SnappydbException) {
            return arrayOf<ByteArray>().iterator()
        }
    }

    override suspend fun read(key: ByteArray): ByteArray? {
        var result: ByteArray? = null
        try {
            result = db.getBytes(makeKey(key))
        } catch (dbException: SnappydbException) {

        }
        return result
    }

    override suspend fun write(key: ByteArray, value: ByteArray) {
        try {
            db.put(makeKey(key), value)
        } catch (dbException: SnappydbException) {
            throw XyoStorageException("Failed to write: $dbException")
        }
    }

    fun reset() {
        db.destroy()
    }

    private fun makeKey(byteArray: ByteArray): String {
        return String(Base64.encode(byteArray, 0))
    }

    private fun getKey(string: String): ByteArray {
        return Base64.decode(string, 0)
    }

    companion object {
        const val ARCHIVIST_LIST = "archlist"
    }
}
