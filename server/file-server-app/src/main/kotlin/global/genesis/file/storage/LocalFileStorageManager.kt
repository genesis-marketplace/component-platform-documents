package global.genesis.file.storage

import global.genesis.commons.standards.GenesisPaths
import global.genesis.db.rx.entity.multi.AsyncEntityDb
import global.genesis.file.storage.data.StorageDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.name
import kotlin.io.path.outputStream

class LocalFileStorageManager(
    private val storagePath: Path,
    db: AsyncEntityDb
) : AbstractFileStorageManager(
    db = db
) {

    constructor(
        locationName: String,
        db: AsyncEntityDb
    ) : this(
        storagePath = Paths.get(GenesisPaths.runtime())
            .resolve("fileupload")
            .resolve(locationName),
        db = db
    )

    override val storageManager: String
        get() = "LOCAL_STORAGE"

    init {
        require(storageManager.matches("\\w+".toRegex())) {
            "Location name should only contain numbers and letters"
        }

        if (!Files.isDirectory(storagePath)) {
            Files.createDirectories(storagePath)
        }
    }

    override suspend fun init() {
        withContext(Dispatchers.IO) {
            Files.createDirectories(storagePath)
        }
    }

    // TODO perhaps we should take the row id into account?
    private tailrec fun resolveUniqueStorageDetails(
        fileName: String,
        attempt: Int = 0
    ): Path {
        val resolvedPath = when (attempt) {
            0 -> storagePath.resolve(fileName)
            else -> storagePath.resolve("$fileName.$attempt")
        }

        when {
            !Files.isRegularFile(resolvedPath) -> return resolvedPath
            attempt == 0 -> LOG.warn("Duplicate file name detected {}", fileName)
        }

        return resolveUniqueStorageDetails(fileName, attempt + 1)
    }

    override suspend fun saveFileStream(
        originalName: String,
        inputStream: InputStream
    ): StorageDetails {
        val path = withContext(Dispatchers.IO) {
            val path = resolveUniqueStorageDetails(originalName)
            writeFile(path, inputStream)
            path
        }
        return StorageDetails(
            fileName = path.fileName.name,
            filePath = storagePath.relativize(path).toString()
        )
    }

    private fun writeFile(path: Path, inputStream: InputStream) {
        path.outputStream(StandardOpenOption.CREATE_NEW).use { output ->
            inputStream.copyTo(output)
        }
    }

    override suspend fun openFileStream(
        storageDetails: StorageDetails
    ): InputStreamProvider? {
        val path = storagePath.resolve(storageDetails.filePath)
        return when (Files.isRegularFile(path)) {
            true -> LocalStreamProvider(path)
            false -> null
        }
    }

    override suspend fun replaceFileStream(
        storageDetails: StorageDetails,
        inputStream: InputStream
    ): StorageDetails {
        val path = withContext(Dispatchers.IO) {
            val path = storagePath.resolve(storageDetails)
            Files.deleteIfExists(path)
            writeFile(path, inputStream)
            path
        }
        return StorageDetails(
            fileName = path.fileName.name,
            filePath = storagePath.relativize(path).toString()
        )
    }

    override suspend fun deleteFileStream(storageDetails: StorageDetails) {
        val path = storagePath.resolve(storageDetails)
        withContext(Dispatchers.IO) {
            Files.deleteIfExists(path)
        }
    }

    private fun Path.resolve(storageDetails: StorageDetails): Path = resolve(storageDetails.filePath)

    private inner class LocalStreamProvider(
        private val path: Path
    ) : InputStreamProvider {

        override fun invoke(): InputStream = BufferedInputStream(Files.newInputStream(path))
    }

    companion object {
        private const val DEFAULT_NAME = "default"
        private val LOG = LoggerFactory.getLogger(LocalFileStorageManager::class.java)
    }
}
