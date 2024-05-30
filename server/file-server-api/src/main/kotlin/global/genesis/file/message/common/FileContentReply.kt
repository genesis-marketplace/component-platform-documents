package global.genesis.file.message.common

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import global.genesis.jackson.serialization.Base64ByteArrayDeserializer
import global.genesis.jackson.serialization.Base64ByteArraySerializer

class FileContentReply(
    val fileStorageId: String,
    val fileName: String,
    @JsonSerialize(using = Base64ByteArraySerializer::class)
    @JsonDeserialize(using = Base64ByteArrayDeserializer::class)
    val fileContent: ByteArray
)
