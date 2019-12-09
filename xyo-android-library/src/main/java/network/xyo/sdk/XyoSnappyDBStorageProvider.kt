package network.xyo.sdk
import android.content.Context
import android.util.Base64
import com.snappydb.DB
import com.snappydb.DBFactory
import com.snappydb.SnappydbException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import network.xyo.sdkcorekotlin.persist.XyoStorageException
import network.xyo.sdkcorekotlin.persist.XyoKeyValueStore

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

    override fun containsKey(key: ByteArray): Deferred<Boolean> = GlobalScope.async {
        try {
            return@async db.exists(makeKey(key))
        } catch (dbException: SnappydbException) {
            throw XyoStorageException("Failed to read: $dbException")
        }
    }

    override fun delete(key: ByteArray) = GlobalScope.async {
        try {
            db.del(makeKey(key))
        } catch (dbException: SnappydbException) {
            throw XyoStorageException("Failed to delete: $dbException")
        }
    }

    override fun getAllKeys(): Deferred<Iterator<ByteArray>> = GlobalScope.async {
        try {
            val i = db.allKeysIterator()

            return@async object : Iterator<ByteArray> {
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
            return@async arrayOf<ByteArray>().iterator()
        }
    }

    override fun read(key: ByteArray): Deferred<ByteArray?> = GlobalScope.async {
        try {
            return@async db.getBytes(makeKey(key))
        } catch (dbException: SnappydbException) {
            return@async null
//            throw XyoStorageException("Failed to read: $dbException")
        }
    }

    override fun write(key: ByteArray, value: ByteArray) = GlobalScope.async {
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
