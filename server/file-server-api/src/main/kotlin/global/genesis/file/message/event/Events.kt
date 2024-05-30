package global.genesis.file.message.event

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import global.genesis.file.message.common.FileContentReply
import global.genesis.jackson.serialization.Base64ByteArrayDeserializer
import global.genesis.jackson.serialization.Base64ByteArraySerializer
import global.genesis.message.core.Outbound
import global.genesis.message.core.error.GenesisError
import global.genesis.message.core.error.GenesisNackReply

data class GenerateDocumentContent(
    val templateReference: String,
    val dataContext: Map<String, Any> = emptyMap(),
    val entityName: String? = null,
    val entityId: String? = null
)

sealed class EventGenerateDocumentContentReply : Outbound() {
    data class EventGenerateDocumentContentAck(
        @JsonSerialize(using = Base64ByteArraySerializer::class)
        @JsonDeserialize(using = Base64ByteArrayDeserializer::class)
        val content: ByteArray,
        val assets: List<FileContentReply>
    ) : EventGenerateDocumentContentReply() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EventGenerateDocumentContentAck

            if (!content.contentEquals(other.content)) return false
            if (assets != other.assets) return false

            return true
        }

        override fun hashCode(): Int {
            var result = content.contentHashCode()
            result = 31 * result + assets.hashCode()
            return result
        }
    }

    data class EventGenerateDocumentContentNack(
        override val error: List<GenesisError>,
        override val warning: List<GenesisError>
    ) : EventGenerateDocumentContentReply(), GenesisNackReply
}
