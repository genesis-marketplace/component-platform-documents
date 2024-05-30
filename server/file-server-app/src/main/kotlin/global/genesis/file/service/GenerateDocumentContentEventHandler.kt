package global.genesis.file.service

import global.genesis.commons.model.GenesisSet
import global.genesis.commons.standards.MessageType.MESSAGE_TYPE
import global.genesis.dictionary.proposed.codegen.toLowerCamelCase
import global.genesis.file.message.event.EventGenerateDocumentContentReply
import global.genesis.file.message.event.GenerateDocumentContent
import global.genesis.file.service.util.EntityResolver
import global.genesis.file.service.util.EntityResult
import global.genesis.file.service.util.toReply
import global.genesis.file.storage.AbstractFileStorageManager
import global.genesis.file.storage.data.StoredFile
import global.genesis.file.templates.DocumentContentConfiguration
import global.genesis.file.templates.DocumentGenerator
import global.genesis.jackson.core.GenesisJacksonMapper.Companion.toGenesisSet
import global.genesis.jackson.core.GenesisJacksonMapper.Companion.toObject
import global.genesis.message.core.error.ErrorCode
import global.genesis.message.core.error.StandardError
import kotlinx.coroutines.flow.first
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.slf4j.LoggerFactory
import javax.inject.Inject

private const val EVENT_GENERATE_DOCUMENT_CONTENT_ACK = "EVENT_GENERATE_DOCUMENT_CONTENT_ACK"
private const val EVENT_GENERATE_DOCUMENT_CONTENT_NACK = "EVENT_GENERATE_DOCUMENT_CONTENT_NACK"

class GenerateDocumentContentEventHandler @Inject constructor(
    private val documentGenerator: DocumentGenerator,
    private val fileStorageManager: AbstractFileStorageManager,
    private val entityResolver: EntityResolver
) {
    suspend fun generateDocumentContent(message: GenesisSet, sourceRef: String): GenesisSet {
        return try {
            generateDocumentContentReply(message, sourceRef)
        } catch (e: Exception) {
            val errorSet = EventGenerateDocumentContentReply.EventGenerateDocumentContentNack(
                error = listOf(
                    StandardError(
                        ErrorCode.GENERIC_ERROR,
                        e.message
                    )
                ),
                warning = emptyList()
            ).toGenesisSet()
            errorSet.setString("MESSAGE_TYPE", EVENT_GENERATE_DOCUMENT_CONTENT_NACK)
            errorSet.sourceRef = sourceRef
            return errorSet
        }
    }

    private suspend fun GenerateDocumentContentEventHandler.generateDocumentContentReply(
        message: GenesisSet,
        sourceRef: String
    ): GenesisSet {
        val userName = message.getString("USER_NAME")
        require(userName != null) { "USER_NAME must be provided" }
        val detailsSet = message.getGenesisSet("DETAILS")

        require(detailsSet != null) { "Malformed message received, DETAILS required." }
        val event = detailsSet.toObject<GenerateDocumentContent>()
        val templateId = resolveTemplate(event.templateReference)
        val dataContext = event.dataContext.mapKeys {
            it.key.toLowerCaseAsciiOnly()
        }.toMutableMap()

        val entityName = event.entityName
        val entityId = event.entityId
        if (entityName != null && entityId != null) {
            addEntityToDataContext(entityName, entityId, dataContext)
        }

        val result = documentGenerator.generateContent(
            DocumentContentConfiguration(
                templateId = templateId,
                userName = userName,
                data = dataContext
            )
        )

        val assetContent = result.assetIds.mapNotNull {
            fileStorageManager.loadFile(it)
        }.map {
            it.toReply()
        }.toList()

        val eventReply = EventGenerateDocumentContentReply.EventGenerateDocumentContentAck(
            result.rawContent.toByteArray(),
            assetContent
        ).toGenesisSet()

        eventReply.sourceRef = sourceRef
        eventReply.setString(MESSAGE_TYPE, EVENT_GENERATE_DOCUMENT_CONTENT_ACK)

        return eventReply
    }

    private suspend fun resolveTemplate(templateRef: String): String {
        val templateFile: StoredFile? = fileStorageManager.loadFile(templateRef)
        val templateId = templateFile?.fileStorageId ?: resolveTemplateByName(templateRef)
        return templateId
    }

    private suspend fun resolveTemplateByName(templateRef: String): String {
        return try {
            fileStorageManager.loadFiles(templateRef).first().fileStorageId
        } catch (e: NoSuchElementException) {
            val message = "Template could not be found in file storage for file name: $templateRef"
            LOG.warn(message)
            throw IllegalArgumentException(message)
        }
    }

    private suspend fun addEntityToDataContext(entityName: String, entityId: String, dataContext: MutableMap<String, Any>) {
        val entity = when (val entityResult = entityResolver.resolveEntity(entityName)) {
            EntityResult.NoResult -> {
                LOG.warn("Unable to resolve entity for entity name $entityName, generating document content without linked object data")
                null
            }

            is EntityResult.TableResult -> {
                entityResolver.getTableEntity(entityResult, entityId)
            }

            is EntityResult.ViewResult -> {
                entityResolver.getViewEntity(entityResult, entityId)
            }
        }
        if (entity != null) {
            dataContext[entityName.toLowerCamelCase()] = entity
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(GenerateDocumentContentEventHandler::class.java)
    }
}
