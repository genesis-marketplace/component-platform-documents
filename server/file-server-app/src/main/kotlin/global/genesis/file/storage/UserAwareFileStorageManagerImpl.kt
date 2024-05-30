package global.genesis.file.storage

import global.genesis.db.entity.TableEntity
import global.genesis.db.entity.UniqueEntityIndex
import global.genesis.db.rx.entity.transactionaware.UserContextAware
import global.genesis.db.updatequeue.GenericRecordUpdate
import global.genesis.file.storage.data.StoredFile
import global.genesis.gen.dao.FileStorage
import kotlinx.coroutines.flow.Flow

internal class UserAwareFileStorageManagerImpl<INPUT : Any>(
    private val base: FileStorageManager<INPUT>
) : UserContextAware(), UserAwareFileStorageManager<INPUT> {
    override fun subscribe(): Flow<GenericRecordUpdate<StoredFile>> = base.subscribe()

    override suspend fun saveFile(fileName: String, dataSource: INPUT): FileStorage =
        base.saveFile(fileName, dataSource, userNameOrUnknown())

    override suspend fun <LINK : TableEntity> saveFile(
        fileName: String,
        dataSource: INPUT,
        fileStorageAware: FileStorage.Aware<LINK>
    ): FileStorage = base.saveFile(
        fileName = fileName,
        dataSource = dataSource,
        fileStorageAware = fileStorageAware,
        userName = userNameOrUnknown()
    )

    override suspend fun loadFile(index: UniqueEntityIndex<FileStorage, *>): StoredFile? =
        base.loadFile(index)

    override suspend fun loadFile(id: String): StoredFile? = base.loadFile(id)

    override suspend fun loadFiles(fileName: String): Flow<StoredFile> =
        base.loadFiles(fileName)

    override suspend fun <LINK : TableEntity> loadFiles(
        fileStorageAware: FileStorage.Aware<LINK>
    ): Flow<StoredFile> = base.loadFiles(fileStorageAware)

    override suspend fun modifyFile(
        fileId: String,
        dataSource: INPUT
    ): FileStorage = base.modifyFile(
        fileId = fileId,
        dataSource = dataSource,
        userName = userNameOrUnknown()
    )

    override suspend fun modifyFile(
        index: UniqueEntityIndex<FileStorage, *>,
        dataSource: INPUT
    ): FileStorage = base.modifyFile(
        index = index,
        dataSource = dataSource,
        userName = userNameOrUnknown()
    )

    override suspend fun deleteFile(id: String) = base.deleteFile(id)

    override suspend fun deleteFile(index: UniqueEntityIndex<FileStorage, *>) = base.deleteFile(index)
}
