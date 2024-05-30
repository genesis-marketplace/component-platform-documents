package global.genesis.file.client

import global.genesis.clustersupport.service.ServiceDiscovery
import global.genesis.commons.annotation.Module
import global.genesis.file.message.common.FileContentReply
import global.genesis.file.message.event.EventGenerateDocumentContentReply
import global.genesis.file.message.event.GenerateDocumentContent
import global.genesis.file.message.request.FileContentRequest
import global.genesis.message.core.event.Event
import global.genesis.message.core.request.Request
import global.genesis.message.core.workflow.message.EventWorkflow
import global.genesis.message.core.workflow.message.RequestReplyWorkflow
import global.genesis.message.core.workflow.message.eventWorkflowBuilder
import global.genesis.message.core.workflow.message.requestReplyWorkflowBuilder
import global.genesis.net.GenesisMessageClient
import org.slf4j.LoggerFactory
import javax.inject.Inject

const val REQ_FILE_CONTENTS = "REQ_FILE_CONTENTS"
const val EVENT_GENERATE_DOCUMENT_CONTENT = "EVENT_GENERATE_DOCUMENT_CONTENT"

private const val FILE_CONTENTS = "FILE_CONTENTS"
private const val FILE_MANAGER = "FILE_MANAGER"

/**
 * Message client class that uses [ServiceDiscovery] to communicate
 * with the remote endpoints exposed by the FILE_MANAGER process.
 *
 * Using the service API prevents an installation dependency between two
 * platform components. If the endpoints cannot be resolved by service
 * discovery, then it is assumed that the document management module is
 * not installed as part of the system.
 *
 * The messageClient argument in each method is designed to make this class
 * easy to use from Genesis integration tests in other modules. It is not
 * intended to pass a custom messageClient class in Production code.
 *
 * @author tgosling
 */
@Module
class FileStorageClient @Inject constructor(
    private val serviceDiscovery: ServiceDiscovery
) {
    object FileContentsWorkflow : RequestReplyWorkflow<FileContentRequest, FileContentReply> by requestReplyWorkflowBuilder(
        FILE_CONTENTS
    )
    object GenerateDocumentContentWorkflow : EventWorkflow<GenerateDocumentContent, EventGenerateDocumentContentReply> by eventWorkflowBuilder()
    suspend fun getFileContents(
        request: FileContentRequest,
        userName: String,
        messageClient:
            GenesisMessageClient? = serviceDiscovery.resolveClient(FILE_MANAGER)
    ): List<FileContentReply> {
        return if (messageClient == null) {
            LOG.warn(
                "Attempted to contact endpoint REQ_FILE_CONTENTS in order to get file data, but endpoint could not be found." +
                    " Ensure the Document Management components has been installed successfully and is running"
            )
            emptyList()
        } else {
            val req = Request(
                request = request,
                messageType = FILE_CONTENTS,
                userName = userName
            )
            messageClient.request(FileContentsWorkflow.withData(req)).reply
        }
    }

    suspend fun generateDocumentContent(
        event: GenerateDocumentContent,
        userName: String,
        messageClient: GenesisMessageClient? = serviceDiscovery.resolveClient(FILE_MANAGER)
    ): EventGenerateDocumentContentReply? {
        return if (messageClient == null) {
            LOG.warn("Attempted to contact endpoint EVENT_GENERATE_DOCUMENT_CONTENT in order to generate content from a template, but endpoint could not be found. Ensure the Document Management components has been installed successfully and is running")
            null
        } else {
            val message = Event(
                details = event,
                userName = userName
            )
            messageClient.suspendRequest(GenerateDocumentContentWorkflow.withData(message))
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(FileStorageClient::class.java)
    }
}
