package gpal.extn.http

import global.genesis.db.entity.TableEntity
import global.genesis.db.entity.UniqueEntityIndex
import global.genesis.file.storage.UserAwareFileStorageManager
import global.genesis.file.storage.data.StoredFile
import global.genesis.file.storage.provider.FileStorageProvider
import global.genesis.gen.dao.FileStorage
import global.genesis.router.data.FileUploadFileData
import global.genesis.router.data.MultipartHttpBody
import global.genesis.router.extension.WebHandlerBuilder
import global.genesis.router.extension.builder.EndpointBuilder
import global.genesis.router.extension.transform.response.FileDownload
import global.genesis.router.server.web.http.extensions.RequestType
import kotlinx.coroutines.flow.toList
import java.io.InputStream

fun WebHandlerBuilder.fileStorageProvider(): UserAwareFileStorageManager<InputStream> {
    val fileStorageProvider = injector<FileStorageProvider>()
    return fileStorageProvider.get().userAware()
}

@JvmName("fileStorageDownloadEndpointById")
inline fun <reified AWARE : FileStorage.Aware<*>> WebHandlerBuilder.fileStorageDownloadEndpoint(
    path: String,
    storage: UserAwareFileStorageManager<InputStream>,
    noinline builder: EndpointBuilder<AWARE, InputStream>.() -> Unit = {}
) {
    endpoint<AWARE, InputStream>(RequestType.GET, path) {
        handleRequest {
            storage.loadFiles(body)
                .toList()
                .sortedByDescending { it.modifiedAt }
                .map { it.inputStream }
                .firstOrNull()
        }
        builder()
    }
}

fun WebHandlerBuilder.fileStorageDownloadEndpoint(
    path: String = "download",
    storage: UserAwareFileStorageManager<InputStream>,
    builder: EndpointBuilder<Unit, FileDownload>.() -> Unit = {}
) {
    endpoint(RequestType.GET, path) {
        val fileStorageId by queryParameter("fileStorageId")

        handleRequest {
            storage.loadFile(FileStorage.ById(fileStorageId))
                ?.asFileDownload()
        }
        builder()
    }
}

fun StoredFile.asFileDownload(): FileDownload = FileDownload(
    fileName = fileName,
    inputStream = inputStream,
    size = fileSize
)

@JvmName("fileStorageUploadEndpointAware")
inline fun <reified AWARE> WebHandlerBuilder.fileStorageUploadEndpoint(
    path: String,
    storage: UserAwareFileStorageManager<InputStream>,
    crossinline builder: EndpointBuilder<MultipartHttpBody, *>.() -> Unit = {}
) where AWARE : FileStorage.Aware<*>, AWARE : UniqueEntityIndex<*, *> {
    multipartEndpoint(path) {
        val aware by queryParameters<AWARE>()
        handleRequest {
            body.fileUploads
                .map { fileData -> storage.saveFile(fileData, aware) }
                .map { it.byId() }
        }
        builder()
    }
}

fun WebHandlerBuilder.fileStorageUploadEndpoint(
    path: String = "upload",
    storage: UserAwareFileStorageManager<InputStream>,
    builder: EndpointBuilder<MultipartHttpBody, List<FileStorage.ById>>.() -> Unit
) {
    multipartEndpoint(path) {
        handleRequest {
            require(body.fileUploads.isNotEmpty()) { "No files uploaded" }
            body.fileUploads
                .map { fileData -> storage.saveFile(fileData) }
                .map { it.byId() }
        }
        builder()
    }
}

suspend fun UserAwareFileStorageManager<InputStream>.saveFile(
    fileData: FileUploadFileData
): FileStorage = fileData.use {
    saveFile(
        fileName = fileData.fileName,
        dataSource = fileData
    )
}

suspend fun <LINK : TableEntity> UserAwareFileStorageManager<InputStream>.saveFile(
    fileData: FileUploadFileData,
    aware: FileStorage.Aware<LINK>
): FileStorage = fileData.use {
    saveFile(
        fileName = fileData.fileName,
        dataSource = it,
        fileStorageAware = aware
    )
}
