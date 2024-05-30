package global.genesis.file.storage

import global.genesis.db.entity.TableEntity
import global.genesis.db.entity.UniqueEntityIndex
import global.genesis.db.updatequeue.GenericRecordUpdate
import global.genesis.file.storage.data.StoredFile
import global.genesis.gen.dao.FileStorage
import kotlinx.coroutines.flow.Flow

interface UserAwareFileStorageManager<INPUT : Any> {
    @JvmSynthetic
    fun subscribe(): Flow<GenericRecordUpdate<StoredFile>>

    @JvmSynthetic
    suspend fun saveFile(
        fileName: String,
        dataSource: INPUT
    ): FileStorage

    @JvmSynthetic
    // TODO add createOrReplace, or check for uniqueness of filename
    suspend fun <LINK : TableEntity> saveFile(
        fileName: String,
        dataSource: INPUT,
        fileStorageAware: FileStorage.Aware<LINK>
    ): FileStorage

    @JvmSynthetic
    suspend fun loadFile(index: UniqueEntityIndex<FileStorage, *>): StoredFile?

    @JvmSynthetic
    suspend fun loadFile(id: String): StoredFile?

    @JvmSynthetic
    suspend fun loadFiles(fileName: String): Flow<StoredFile>

    @JvmSynthetic
    suspend fun <LINK : TableEntity> loadFiles(
        fileStorageAware: FileStorage.Aware<LINK>
    ): Flow<StoredFile>

    @JvmSynthetic
    suspend fun modifyFile(
        fileId: String,
        dataSource: INPUT
    ): FileStorage

    @JvmSynthetic
    suspend fun modifyFile(
        index: UniqueEntityIndex<FileStorage, *>,
        dataSource: INPUT
    ): FileStorage

    @JvmSynthetic
    suspend fun deleteFile(id: String)

    @JvmSynthetic
    suspend fun deleteFile(index: UniqueEntityIndex<FileStorage, *>)
}
