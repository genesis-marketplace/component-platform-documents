package global.genesis.file.service

import global.genesis.commons.model.GenesisSet
import global.genesis.file.message.common.FileContentReply
import global.genesis.file.message.request.FileContentRequest
import global.genesis.file.service.util.toReply
import global.genesis.file.storage.AbstractFileStorageManager
import global.genesis.jackson.core.GenesisJacksonMapper.Companion.toGenesisSet
import global.genesis.jackson.core.GenesisJacksonMapper.Companion.toObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import javax.inject.Inject

private const val REP_FILE_CONTENTS = "REP_FILE_CONTENTS"

/**
 * Request handler to get the binary content of a file stored in
 * file storage and return it as a base64 encoded String on the response.
 *
 * @author tgosling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FileContentRequestHandler @Inject constructor(
    private val fileStorageManager: AbstractFileStorageManager
) {
    suspend fun handleFileContentRequest(message: GenesisSet, sourceRef: String): GenesisSet {
        val requestSet = message.getGenesisSet("REQUEST")
        require(requestSet != null) { "Malformed message received, REQUEST required." }
        val request = requestSet.toObject<FileContentRequest>()
        val fileStorageIds = request.fileStorageIds
        val fileNames = request.fileNames

        val returnArray = if (fileStorageIds.isNotEmpty()) {
            return getFileContentById(fileStorageIds, sourceRef)
        } else if (fileNames.isNotEmpty()) {
            getFileContentByName(fileNames, sourceRef)
        } else {
            throw IllegalArgumentException("One of FILE_STORAGE_ID or FILE_NAME must be provided")
        }

        return returnArray
    }

    private suspend fun getFileContentByName(fileNames: Set<String>, sourceRef: String): GenesisSet {
        val mutableList = mutableListOf<FileContentReply>()
        fileNames.asFlow()
            .flatMapMerge {
                fileStorageManager.loadFiles(it)
            }.map {
                it.toReply()
            }.toList(mutableList)
        return buildReplyMessage(mutableList, sourceRef, REP_FILE_CONTENTS)
    }

    private suspend fun getFileContentById(fileStorageIds: Set<String>, sourceRef: String): GenesisSet {
        val replies = mutableListOf<FileContentReply>()
        fileStorageIds
            .asFlow()
            .mapNotNull {
                fileStorageManager.loadFile(it)
            }
            .map {
                it.toReply()
            }.toList(replies)
        return buildReplyMessage(replies, sourceRef, REP_FILE_CONTENTS)
    }

    private fun buildReplyMessage(replies: MutableList<out Any>, sourceRef: String, messageType: String): GenesisSet {
        val response = GenesisSet()
        response.setFullArray("REPLY", replies.map { it.toGenesisSet() })
        response.setString("MESSAGE_TYPE", messageType)
        response.setString("PARAMETRIC_TYPE", FileContentReply::class.qualifiedName)
        response.sourceRef = sourceRef
        return response
    }
}
