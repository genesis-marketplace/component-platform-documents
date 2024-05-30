package global.genesis.file.storage

import global.genesis.db.EntityModifyDetails
import global.genesis.db.entity.TableEntity
import global.genesis.db.entity.UniqueEntityIndex
import global.genesis.db.rx.entity.multi.AsyncEntityDb
import global.genesis.db.rx.entity.transactionaware.WriteTransactionAware
import global.genesis.db.updatequeue.GenericRecordUpdate
import global.genesis.db.updatequeue.flatMapUpdate
import global.genesis.file.storage.data.StorageDetails
import global.genesis.file.storage.data.StoredFile
import global.genesis.gen.config.tables.FILE_STORAGE
import global.genesis.gen.dao.FileStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import javax.annotation.PostConstruct

typealias InputStreamProvider = () -> InputStream

abstract class AbstractFileStorageManager(
    protected val db: AsyncEntityDb
) : WriteTransactionAware(), FileStorageManager<InputStream> {

    abstract val storageManager: String

    private suspend fun currentTxn() = db.currentTxn()

    @PostConstruct
    fun initialise() = runBlocking {
        init()
    }

    protected abstract suspend fun init()
    protected abstract suspend fun saveFileStream(
        originalName: String,
        inputStream: InputStream
    ): StorageDetails

    protected abstract suspend fun openFileStream(
        storageDetails: StorageDetails
    ): InputStreamProvider?

    protected abstract suspend fun replaceFileStream(
        storageDetails: StorageDetails,
        inputStream: InputStream
    ): StorageDetails

    protected abstract suspend fun deleteFileStream(
        storageDetails: StorageDetails
    )

    @JvmSynthetic
    override fun subscribe(): Flow<GenericRecordUpdate<StoredFile>> = db.subscribe<FileStorage>()
        .flatMapUpdate {
            when (it.storageManager) {
                storageManager -> loadFile(it)
                else -> null
            }
        }

    override suspend fun <LINK : TableEntity> saveFile(
        fileName: String,
        inputStream: InputStream,
        userName: String,
        fileStorageAware: FileStorage.Aware<LINK>
    ): FileStorage {
        val storage = saveFile(fileName, inputStream, userName)
        currentTxn().insert(fileStorageAware + storage.byId())
        return storage
    }

    override suspend fun saveFile(
        fileName: String,
        dataSource: InputStream,
        userName: String
    ): FileStorage {
        val fileSizeInBytes = dataSource.available().toLong()
        if (fileSizeInBytes <= 0L) {
            val message = String.format("Storage Manager %s was unable to save %s because no file data was detected.", storageManager, fileName)
            LOG.info(message)
            throw IOException(message)
        }
        val storageDetails = saveFileStream(fileName, dataSource)
        val now = DateTime.now()
        return currentTxn().insert(
            FileStorage {
                this.fileName = storageDetails.fileName
                this.storageManager = this@AbstractFileStorageManager.storageManager
                fileSize = fileSizeInBytes
                createdAt = now
                createdBy = userName
                modifiedAt = now
                modifiedBy = userName
                locationDetails = storageDetails.filePath
            }
        ).record
    }

    @JvmSynthetic
    override fun loadFiles(
        fileName: String
    ): Flow<StoredFile> = flow {
        currentTxn()
            .getRange(FileStorage.ByStorageManagerFileName(storageManager, fileName))
            .collect { emit(loadFile(it)) }
    }

    override fun <LINK : TableEntity> loadFiles(
        fileStorageAware: FileStorage.Aware<LINK>
    ): Flow<StoredFile> = flow {
        currentTxn()
            .getRange(fileStorageAware.linkToFileStorage())
            .collect { link ->
                val byId: FileStorage.ById = fileStorageAware + link
                val file = loadFile(byId)
                if (file != null) emit(file)
            }
    }

    @JvmSynthetic
    override suspend fun loadFile(
        id: String
    ): StoredFile? = loadFile(FileStorage.ById(id))

    @JvmSynthetic
    override suspend fun loadFile(
        index: UniqueEntityIndex<FileStorage, *>
    ): StoredFile? = when (val fileStorage = currentTxn().get(index)) {
        null -> {
            LOG.warn("Received request for unknown key {}", index)
            null
        }

        else -> loadFile(fileStorage)
    }

    private suspend fun loadFile(
        fileStorage: FileStorage
    ): StoredFile = when (val streamProvider = openFileStream(fileStorage.asStorageDetails())) {
        null -> {
            LOG.warn("Storage Manager {} was unable to load {}", storageManager, fileStorage.locationDetails)
            throw FileNotFoundException(fileStorage.toString())
        }

        else -> StoredFile(fileStorage, streamProvider)
    }

    override suspend fun modifyFile(
        fileId: String,
        dataSource: InputStream,
        userName: String
    ): FileStorage = modifyFile(
        index = FileStorage.ById(fileId),
        dataSource = dataSource,
        userName = userName
    )

    override suspend fun modifyFile(
        index: UniqueEntityIndex<FileStorage, *>,
        dataSource: InputStream,
        userName: String
    ): FileStorage {
        val fileSizeInBytes = dataSource.available().toLong()
        if (fileSizeInBytes <= 0L) {
            val fileStorageId = index.toDbRecord().getString("FILE_STORAGE_ID")
            val message = String.format("Storage Manager %s was unable to modify file with id %s because no file data was detected.", storageManager, fileStorageId)
            LOG.info(message)
            throw IOException(message)
        }
        val fileStorage = currentTxn().get(index) ?: throw FileNotFoundException(index.toString())
        replaceFileStream(fileStorage.asStorageDetails(), dataSource)

        fileStorage.modifiedAt = DateTime.now()
        fileStorage.modifiedBy = userName
        return currentTxn().modify(EntityModifyDetails(fileStorage, fields = lastModifyFields)).record
    }

    override suspend fun deleteFile(id: String) = deleteFile(FileStorage.ById(id))

    override suspend fun deleteFile(index: UniqueEntityIndex<FileStorage, *>) {
        val fileStorage = currentTxn().get(index) ?: return
        deleteFileStream(fileStorage.asStorageDetails())
        currentTxn().delete(index)
    }

    private fun FileStorage.asStorageDetails() =
        StorageDetails(fileName = fileName, filePath = locationDetails)

    override fun userAware(): UserAwareFileStorageManager<InputStream> = UserAwareFileStorageManagerImpl(this)

    override fun <NEW : Any> plus(
        transform: (NEW) -> InputStream
    ): FileStorageManager<NEW> = TransformingFileStorageManager(
        source = this,
        transform = transform
    )

    companion object {
        private val LOG = LoggerFactory.getLogger(AbstractFileStorageManager::class.java)
        private val lastModifyFields = listOf(FILE_STORAGE.MODIFIED_AT.name, FILE_STORAGE.MODIFIED_BY.name)
    }
}
