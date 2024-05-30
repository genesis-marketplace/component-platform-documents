package global.genesis.file.storage

import global.genesis.db.entity.TableEntity
import global.genesis.db.entity.UniqueEntityIndex
import global.genesis.db.updatequeue.GenericRecordUpdate
import global.genesis.file.storage.data.StoredFile
import global.genesis.gen.dao.FileStorage
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

interface FileStorageManagerRx<INPUT : Any> {

    fun subscribe(): Flowable<GenericRecordUpdate<StoredFile>>

    fun saveFile(
        fileName: String,
        dataSource: INPUT,
        userName: String
    ): Single<FileStorage>

    fun <LINK : TableEntity> saveFile(
        fileName: String,
        dataSource: INPUT,
        userName: String,
        fileStorageAware: FileStorage.Aware<LINK>
    ): Single<FileStorage>

    fun loadFile(index: UniqueEntityIndex<FileStorage, *>): Maybe<StoredFile>

    fun loadFile(id: String): Maybe<StoredFile>

    fun loadFiles(fileName: String): Flowable<StoredFile>

    fun <LINK : TableEntity> loadFiles(
        fileStorageAware: FileStorage.Aware<LINK>
    ): Flowable<StoredFile>

    fun modifyFile(
        fileId: String,
        dataSource: INPUT,
        userName: String
    ): Single<FileStorage>

    fun modifyFile(
        index: UniqueEntityIndex<FileStorage, *>,
        dataSource: INPUT,
        userName: String
    ): Single<FileStorage>

    fun deleteFile(id: String): Single<Unit>

    fun deleteFile(index: UniqueEntityIndex<FileStorage, *>): Single<Unit>

    operator fun <NEW : Any> plus(
        transform: (NEW) -> INPUT
    ): FileStorageManagerRx<NEW>
}
