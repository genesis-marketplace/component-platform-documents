package global.genesis.file.storage

import com.azure.identity.ClientSecretCredentialBuilder
import com.microsoft.graph.authentication.TokenCredentialAuthProvider
import com.microsoft.graph.requests.GraphServiceClient
import global.genesis.db.rx.entity.multi.AsyncEntityDb
import global.genesis.file.storage.data.StorageDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream

/**
 * Implementation of [AbstractFileStorageManager] that uses the new MS Graph
 * API to interact with a sharepoint filesystem.
 *
 * Token management is handled by the Azure auth SDK and calls are made using
 * the Graph SDK.
 */
class SharePointGraphFileStorageManager(
    private val siteId: String,
    clientId: String,
    clientSecret: String,
    tenantId: String,
    db: AsyncEntityDb
) : AbstractFileStorageManager(
    db = db
) {
    private val client: GraphServiceClient<Request>

    init {
        val credential = ClientSecretCredentialBuilder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .tenantId(tenantId)
            .build()

        val authProvider = TokenCredentialAuthProvider(
            listOf("https://graph.microsoft.com/.default"),
            credential
        )

        client = GraphServiceClient.builder()
            .authenticationProvider(authProvider)
            .buildClient()
    }

    override val storageManager: String
        get() = "SHAREPOINT_STORAGE"

    override suspend fun init() {
        LOG.info("Initializing $storageManager with siteId = $siteId")
    }

    override suspend fun saveFileStream(originalName: String, inputStream: InputStream): StorageDetails {
        writeToSharepoint(originalName, inputStream)

        val (fileName, filePath) = if (originalName.contains("/")) {
            originalName.substringAfterLast('/') to originalName.substringBeforeLast('/')
        } else {
            originalName to ""
        }

        return StorageDetails(fileName, filePath)
    }

    override suspend fun openFileStream(storageDetails: StorageDetails): InputStreamProvider? {
        val path = getFullPath(storageDetails)

        val item = withContext(Dispatchers.IO) {
            client.sites(siteId).drive()
                .root()
                .itemWithPath(path)
                .content()
                .buildRequest()
                .get()
        }
        require(item != null) { "Unable to retrieve access token" }
        return { item }
    }

    override suspend fun replaceFileStream(storageDetails: StorageDetails, inputStream: InputStream): StorageDetails {
        writeToSharepoint(getFullPath(storageDetails), inputStream)
        return storageDetails
    }

    override suspend fun deleteFileStream(storageDetails: StorageDetails) {
        withContext(Dispatchers.IO) {
            client.sites(siteId).drive()
                .root()
                .itemWithPath(getFullPath(storageDetails))
                .buildRequest()
                .delete()
        }
    }

    private suspend fun writeToSharepoint(
        path: String,
        inputStream: InputStream
    ) {
        withContext(Dispatchers.IO) {
            client.sites(siteId).drive()
                .root()
                .itemWithPath(path)
                .content()
                .buildRequest()
                .put(inputStream.readAllBytes())
        }
    }

    private fun getFullPath(storageDetails: StorageDetails): String {
        val path = if (storageDetails.filePath.isBlank()) {
            storageDetails.fileName
        } else {
            storageDetails.filePath + "/" + storageDetails.fileName
        }
        return path
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(SharePointGraphFileStorageManager::class.java)
    }
}
