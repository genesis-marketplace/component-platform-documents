package global.genesis.file.message.request

class FileContentRequest(
    val fileStorageIds: Set<String> = emptySet(),
    val fileNames: Set<String> = emptySet()
)
