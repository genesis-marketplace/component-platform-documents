package global.genesis.file.service.util

import global.genesis.file.message.common.FileContentReply
import global.genesis.file.storage.data.StoredFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun StoredFile.toReply(): FileContentReply = withContext(Dispatchers.IO) {
    buildReply(fileStorageId, fileName, inputStream.readAllBytes())
}

private fun buildReply(fileStorageId: String, fileName: String, content: ByteArray): FileContentReply {
    return FileContentReply(
        fileStorageId = fileStorageId,
        fileName = fileName,
        fileContent = content
    )
}
