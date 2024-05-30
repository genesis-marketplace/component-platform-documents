package global.genesis.file.storage.data

data class UploadedParts<INPUT : Any>(
    val uploadId: String,
    val bucketName: String,
    val fileName: String,
    val fullPath: String,
    val parts: List<INPUT>?
)
