package global.genesis.file.storage.data

import global.genesis.gen.dao.FileStorage
import org.joda.time.DateTime
import java.io.IOException
import java.io.InputStream

fun interface FileHandler<T> {
    @Throws(IOException::class)
    fun handle(inputStream: InputStream): T
}

class StoredFile(
    private val details: FileStorage,
    @PublishedApi
    internal val provider: () -> InputStream
) {
    val fileStorageId: String get() = details.fileStorageId
    val storageManager: String get() = details.storageManager
    val fileName get() = details.fileName
    val modifiedAt: DateTime get() = details.modifiedAt
    val modifiedBy get() = details.modifiedBy
    val createdBy get() = details.createdBy
    val createdAt get() = details.createdAt
    val locationDetails get() = details.locationDetails
    val fileSize get() = details.fileSize

    val inputStream: InputStream by lazy(provider)

    fun <T> useStream(handler: FileHandler<T>): T = useStream { stream -> handler.handle(stream) }

    @JvmSynthetic
    inline fun <T> useStream(
        handler: (InputStream) -> T
    ): T = inputStream.use(handler)
}
