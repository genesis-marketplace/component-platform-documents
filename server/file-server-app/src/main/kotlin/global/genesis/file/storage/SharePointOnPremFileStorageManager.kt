package global.genesis.file.storage

import global.genesis.db.rx.entity.multi.AsyncEntityDb
import global.genesis.file.storage.data.StorageDetails
import global.genesis.jackson.core.GenesisJacksonMapper.Companion.jsonToObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

/**
 * Implementation of [AbstractFileStorageManager] that uses the legacy (retired)
 * MS sharepoint API by targeting the site URL of the actual Sharepoint installation.
 *
 * Given this API is deprecated, it should only be used when the client has their
 * own on-premises Sharepoint installation, as the new Graph API does not support this setup.
 */
class SharePointOnPremFileStorageManager(
    private val sharepointRootUrl: String,
    private val siteUrl: String,
    private val folder: String,
    private val clientId: String,
    private val clientSecret: String,
    private val tenantId: String,
    db: AsyncEntityDb
) : AbstractFileStorageManager(
    db = db
) {
    override val storageManager: String
        get() = "SHAREPOINT_ON_PREM_STORAGE"

    override suspend fun init() {
        LOG.info("Initializing $storageManager with siteUrl = $siteUrl")
    }

    private val httpClient = HttpClient.newHttpClient()

    private suspend fun getAccessToken(): String {
        val requestUrl = "https://accounts.accesscontrol.windows.net/$tenantId/tokens/OAuth/2"

        val urlEncodedBody = "grant_type=client_credentials&" +
            "client_id=$clientId@$tenantId&" +
            "client_secret=$clientSecret&" +
            "resource=00000003-0000-0ff1-ce00-000000000000/$sharepointRootUrl@$tenantId"

        val request = HttpRequest.newBuilder()
            .uri(URI.create(requestUrl))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(BodyPublishers.ofString(urlEncodedBody))
            .build()

        val response = withContext(Dispatchers.IO) {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        }
        require(response.statusCode() == 200) { "Request to retrieve access token returned unsuccessful status code ${response.statusCode()}" }
        val jsonMap = response.body().jsonToObject<Map<String, String>>()
        val accessToken = jsonMap["access_token"]
        require(accessToken != null) { "Request to retrieve access token returned successful code but access token was missing from body" }
        return accessToken
    }

    private suspend fun request(requestUrl: String): HttpRequest.Builder {
        val accessToken = getAccessToken()
        return HttpRequest.newBuilder()
            .uri(URI.create(requestUrl))
            .header("Accept", "application/json;odata=verbose")
            .header("Authorization", "Bearer $accessToken")
    }

    private fun encode(url: String) = URLEncoder.encode(url, StandardCharsets.UTF_8.name()).replace("+", "%20")

    override suspend fun saveFileStream(originalName: String, inputStream: InputStream): StorageDetails {
        val fileBytes = inputStream.readBytes()
        val encodedName = encode(originalName)
        val encodedFolder = encode(folder)

        val requestUrl =
            "$siteUrl/_api/web/GetFolderByServerRelativeUrl('$encodedFolder')/Files/Add(url='$encodedName',overwrite=true)"

        val httpRequest = request(requestUrl)
            .version(HttpClient.Version.HTTP_1_1)
            .header("Accept-Encoding", "gzip, deflate, br")
            .POST(BodyPublishers.ofByteArray(fileBytes))
            .build()

        return try {
            val response = withContext(Dispatchers.IO) {
                httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            }
            val statusCode = response.statusCode()
            LOG.info("Response status code: {}", statusCode)
            LOG.debug("Response body: {}", response.body())
            require(statusCode == 200 || statusCode == 201) {
                "Non 2xx status code received from file upload request"
            }
            StorageDetails(originalName, "$siteUrl/$encodedFolder")
        } catch (e: Exception) {
            LOG.error("Error uploading file to SharePoint", e)
            throw e
        }
    }

    override suspend fun openFileStream(storageDetails: StorageDetails): InputStreamProvider? {
        val folder = encode(URL(storageDetails.filePath).path)
        val fileName = encode(storageDetails.fileName)
        val fileUrl = "$siteUrl/_api/web/GetFileByServerRelativeUrl('$folder/$fileName')/\$value"

        val httpRequest = request(fileUrl)
            .GET()
            .build()

        return try {
            val response = withContext(Dispatchers.IO) {
                httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray())
            }
            require(response.statusCode() == 200) { "Error retrieving file from SharePoint: HTTP ${response.statusCode()}" }
            ByteArrayStreamProvider(ByteArrayInputStream(response.body()))
        } catch (e: Exception) {
            LOG.error("Error retrieving file from SharePoint", e)
            throw e
        }
    }

    override suspend fun replaceFileStream(storageDetails: StorageDetails, inputStream: InputStream): StorageDetails {
        val folder = encode(URL(storageDetails.filePath).path)
        val fileName = encode(storageDetails.fileName)
        val replaceUrl = "$siteUrl/_api/web/GetFileByServerRelativeUrl('$folder/$fileName')/\$value"

        val fileBytes = inputStream.readBytes()

        val httpRequest = request(replaceUrl)
            .header("X-HTTP-Method", "PUT") // Specify the PUT operation
            .PUT(BodyPublishers.ofByteArray(fileBytes))
            .build()

        return try {
            val response = withContext(Dispatchers.IO) {
                httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            }

            if (response.statusCode() == 204) {
                LOG.info("File replaced successfully. Response: ${response.statusCode()}")
                LOG.debug("Response body: ${response.body()}")
                storageDetails
            } else {
                val message = "Failed to replace file. Status code: ${response.statusCode()}, Body: ${response.body()}"
                LOG.error(message)
                throw Exception(message)
            }
        } catch (e: Exception) {
            LOG.error("Error replacing file in SharePoint", e)
            throw e
        }
    }

    override suspend fun deleteFileStream(storageDetails: StorageDetails) {
        val folder = encode(URL(storageDetails.filePath).path)
        val fileName = encode(storageDetails.fileName)
        val deleteUrl = "$siteUrl/_api/web/GetFileByServerRelativeUrl('$folder/$fileName')"

        val httpRequest = request(deleteUrl)
            .header("If-Match", "*") // To match any ETag value for the file
            .header("X-HTTP-Method", "DELETE") // Specify the delete operation
            .build()

        return try {
            val response = withContext(Dispatchers.IO) {
                httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            }

            if (response.statusCode() == 200) {
                LOG.info("File deleted successfully.")
            } else {
                val message = "Failed to delete file. Status code: ${response.statusCode()}"
                LOG.error(message)
                throw Exception(message)
            }
        } catch (e: Exception) {
            LOG.error("Error deleting file from SharePoint", e)
            throw e
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SharePointOnPremFileStorageManager::class.java)
    }

    private inner class ByteArrayStreamProvider(
        private val inputStream: InputStream
    ) : InputStreamProvider {
        override fun invoke(): InputStream = inputStream
    }
}
