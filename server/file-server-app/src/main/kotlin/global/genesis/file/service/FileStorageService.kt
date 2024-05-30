@file:OptIn(DelicateCoroutinesApi::class)

package global.genesis.file.service

import global.genesis.commons.annotation.Module
import global.genesis.commons.model.GenesisSet
import global.genesis.commons.model.GenesisSet.Companion.genesisSet
import global.genesis.file.client.EVENT_GENERATE_DOCUMENT_CONTENT
import global.genesis.file.client.REQ_FILE_CONTENTS
import global.genesis.message.core.error.ErrorCode
import global.genesis.message.core.error.StandardError
import global.genesis.net.channel.GenesisChannel
import global.genesis.net.handler.MessageListener
import global.genesis.process.net.MessageDelegator
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.annotation.PostConstruct
import javax.inject.Inject

/**
 * [MessageListener] class to implement request endpoints to retrieve
 * stored file contents, as well as invoke document generation operations.
 *
 * This class exists so that other platform components do not need a compile
 * dependency on the file storage module. While this is implemented as a legacy
 * message listener so as not to automatically advertise this API to UI and
 * external clients, it is implemented using the same message format as
 * standard platform endpoints. As such, if and when we have a better solution
 * to selectively hide endpoints from the UI, we can convert this to use more
 * modern framework tools without having to modify client code.
 *
 * @author tgosling
 */
@Module
class FileStorageService @Inject constructor(
    private val messageDelegator: MessageDelegator,
    private val fileContentHandler: FileContentRequestHandler,
    private val documentGenerationHandler: GenerateDocumentContentEventHandler
) : MessageListener {

    @PostConstruct
    fun initialise() {
        messageDelegator.addMessageListener(REQ_FILE_CONTENTS, this)
        messageDelegator.addMessageListener(EVENT_GENERATE_DOCUMENT_CONTENT, this)
    }

    override fun onNewMessage(message: GenesisSet?, channel: GenesisChannel?) {
        require(message != null && channel != null) {
            "Received invalid parameters in callback, message: $message, channel: $channel"
        }

        GlobalScope.launch {
            val sourceRef = message.getString("SOURCE_REF") ?: "UNKNOWN"
            try {
                val messageType = message.getString("MESSAGE_TYPE")
                val response = when (messageType) {
                    REQ_FILE_CONTENTS -> {
                        fileContentHandler.handleFileContentRequest(message, sourceRef)
                    }
                    EVENT_GENERATE_DOCUMENT_CONTENT -> {
                        documentGenerationHandler.generateDocumentContent(message, sourceRef)
                    }
                    else -> {
                        getErrorResponse("Unknown message type", sourceRef, ErrorCode.UNKNOWN_MESSAGE_TYPE)
                    }
                }
                channel.writeAndFlush(response)
            } catch (throwable: Throwable) {
                channel.writeAndFlush(getErrorResponse(throwable.message, sourceRef))
            }
        }
    }

    private fun getErrorResponse(
        text: String?,
        sourceRef: String?,
        errorCode: ErrorCode = ErrorCode.GENERIC_ERROR
    ): GenesisSet {
        val error = StandardError(errorCode, text)
        val message = genesisSet {
            "MESSAGE_TYPE" with "MSG_NACK"
            "SOURCE_REF" with (sourceRef ?: "UNKNOWN")
        }
        message.setArray("ERROR", arrayOf(error))
        return message
    }
}
