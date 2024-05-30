package global.genesis.file.storage

import global.genesis.db.entity.TableEntity
import global.genesis.db.entity.UniqueEntityIndex
import global.genesis.db.updatequeue.GenericRecordUpdate
import global.genesis.file.storage.data.StoredFile
import global.genesis.gen.dao.FileStorage
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.asFlowable
import kotlinx.coroutines.rx3.rxMaybe
import kotlinx.coroutines.rx3.rxSingle

internal class FileStorageManagerRxImpl<INPUT : Any>(
    private val source: FileStorageManager<INPUT>
) : FileStorageManagerRx<INPUT> {
    override fun subscribe(): Flowable<GenericRecordUpdate<StoredFile>> =
        source.subscribe().asFlowable()

    override fun saveFile(
        fileName: String,
        dataSource: INPUT,
        userName: String
    ): Single<FileStorage> = rxSingle {
        source.saveFile(
            fileName = fileName,
            dataSource = dataSource,
            userName = userName
        )
    }

    override fun <LINK : TableEntity> saveFile(
        fileName: String,
        dataSource: INPUT,
        userName: String,
        fileStorageAware: FileStorage.Aware<LINK>
    ): Single<FileStorage> = rxSingle {
        source.saveFile(
            fileName = fileName,
            dataSource = dataSource,
            userName = userName,
            fileStorageAware = fileStorageAware
        )
    }

    override fun loadFile(index: UniqueEntityIndex<FileStorage, *>): Maybe<StoredFile> = rxMaybe {
        source.loadFile(index)
    }

    override fun loadFile(id: String): Maybe<StoredFile> = rxMaybe {
        source.loadFile(id)
    }

    override fun loadFiles(fileName: String): Flowable<StoredFile> = source.loadFiles(fileName).asFlowable()

    override fun <LINK : TableEntity> loadFiles(fileStorageAware: FileStorage.Aware<LINK>): Flowable<StoredFile> =
        source.loadFiles(fileStorageAware).asFlowable()

    override fun modifyFile(
        fileId: String,
        dataSource: INPUT,
        userName: String
    ): Single<FileStorage> = rxSingle {
        source.modifyFile(
            fileId = fileId,
            dataSource = dataSource,
            userName = userName
        )
    }

    override fun modifyFile(
        index: UniqueEntityIndex<FileStorage, *>,
        dataSource: INPUT,
        userName: String
    ): Single<FileStorage> = rxSingle {
        source.modifyFile(
            index = index,
            dataSource = dataSource,
            userName = userName
        )
    }

    override fun deleteFile(id: String): Single<Unit> = rxSingle {
        source.deleteFile(id)
    }

    override fun deleteFile(index: UniqueEntityIndex<FileStorage, *>): Single<Unit> = rxSingle {
        source.deleteFile(index)
    }

    override fun <NEW : Any> plus(transform: (NEW) -> INPUT): FileStorageManagerRx<NEW> =
        FileStorageManagerRxImpl(source + transform)
}
