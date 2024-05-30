package global.genesis.file.storage.data

data class InitMultipartUploadResult(
    val uploadId: String,
    val fileName: String,
    val fullPath: String,
    val bucketName: String
)
