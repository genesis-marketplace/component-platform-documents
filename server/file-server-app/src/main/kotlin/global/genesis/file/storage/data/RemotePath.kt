package global.genesis.file.storage.data

data class RemotePath(
    val fullPath: String,
    val directoryPath: String,
    val fileName: String,
    val fileNameExcludingExtension: String,
    val fileExtension: String
)
