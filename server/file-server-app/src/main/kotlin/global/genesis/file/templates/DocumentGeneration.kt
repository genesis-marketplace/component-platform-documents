package global.genesis.file.templates

data class DocumentStorageConfiguration(
    val templateId: String,
    val fileName: String,
    val userName: String,
    val data: Map<String, Any>,
    val deleteOnExit: Boolean = true,
    val workingDirectory: String = ""
)

data class DocumentContentConfiguration(
    val templateId: String,
    val userName: String,
    val data: Map<String, Any>,
    val deleteOnExit: Boolean = true,
    val workingDirectory: String = ""
)

data class DocumentStorageResult(
    val fileStorageId: String
)

data class DocumentContentResult(
    val rawContent: String,
    val assetIds: List<String>
)
