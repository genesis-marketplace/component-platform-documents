package global.genesis.file.storage

import global.genesis.db.entity.TableEntity
import global.genesis.db.entity.UniqueEntityIndex
import global.genesis.db.updatequeue.GenericRecordUpdate
import global.genesis.file.storage.data.StoredFile
import global.genesis.gen.dao.FileStorage
import kotlinx.coroutines.flow.Flow

class TransformingFileStorageManager<OLD : Any, OUTPUT : Any>(
    private val source: FileStorageManager<OLD>,
    private val transform: (OUTPUT) -> OLD
) : FileStorageManager<OUTPUT> {
    override fun subscribe(): Flow<GenericRecordUpdate<StoredFile>> = source.subscribe()

    override suspend fun saveFile(fileName: String, dataSource: OUTPUT, userName: String): FileStorage =
        source.saveFile(fileName, transform(dataSource), userName)

    override suspend fun <LINK : TableEntity> saveFile(
        fileName: String,
        dataSource: OUTPUT,
        userName: String,
        fileStorageAware: FileStorage.Aware<LINK>
    ): FileStorage = source.saveFile(
        fileName = fileName,
        dataSource = transform(dataSource),
        userName = userName,
        fileStorageAware = fileStorageAware
    )

    override suspend fun loadFile(index: UniqueEntityIndex<FileStorage, *>): StoredFile? = source.loadFile(index)

    override suspend fun loadFile(id: String): StoredFile? = source.loadFile(FileStorage.byId(id))

    override fun loadFiles(fileName: String): Flow<StoredFile> = source.loadFiles(fileName)

    override fun <LINK : TableEntity> loadFiles(fileStorageAware: FileStorage.Aware<LINK>): Flow<StoredFile> =
        source.loadFiles(fileStorageAware)

    override suspend fun modifyFile(
        fileId: String,
        dataSource: OUTPUT,
        userName: String
    ): FileStorage = source.modifyFile(
        fileId = fileId,
        dataSource = transform(dataSource),
        userName = userName
    )

    override suspend fun modifyFile(
        index: UniqueEntityIndex<FileStorage, *>,
        dataSource: OUTPUT,
        userName: String
    ): FileStorage = source.modifyFile(
        index = index,
        dataSource = transform(dataSource),
        userName = userName
    )

    override suspend fun deleteFile(id: String) = source.deleteFile(id)

    override suspend fun deleteFile(index: UniqueEntityIndex<FileStorage, *>) = source.deleteFile(index)

    override fun userAware(): UserAwareFileStorageManager<OUTPUT> = UserAwareFileStorageManagerImpl(this)

    override fun <NEW : Any> plus(
        transform: (NEW) -> OUTPUT
    ): FileStorageManager<NEW> {
        val previousTransform = this.transform
        return TransformingFileStorageManager(
            source,
            transform = { it: NEW ->
                previousTransform(transform(it))
            }
        )
    }
}
