package global.genesis.file.storage.util

import global.genesis.file.storage.data.RemotePath

object RemoteStorageManagerUtils {

    fun toRemotePath(folderPrefix: String, fileName: String): RemotePath {
        val fullPath = "${folderPrefix}$fileName"
        val fileNameExcludingExtension = fileName.substringBeforeLast(".")
        val fileExtension = ".${fileName.substringAfterLast(".")}"
        return RemotePath(
            fullPath = fullPath,
            directoryPath = folderPrefix,
            fileName = fileName,
            fileNameExcludingExtension = fileNameExcludingExtension,
            fileExtension = fileExtension
        )
    }
}
