package global.genesis.file.storage

import global.genesis.db.rx.entity.transactionaware.withUserContext
import global.genesis.db.util.AbstractDatabaseTest
import global.genesis.dictionary.GenesisDictionary
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

private const val MESSAGE_CLIENT_PROCESS_NAME: String = "MESSAGE_CLIENT_PROCESS_NAME"
private const val SHARED_DOCUMENTS_LOCATION: String = "https://netorg209792.sharepoint.com/sites/TestSite-TomGosling/Shared%20Documents"
private const val USERNAME: String = "Bianca"

@Disabled("These tests are disabled until we get a proper Sharepoint test env.")
class SharePointOnPremFileStorageManagerTest : AbstractDatabaseTest() {

    init {
        System.setProperty("jdk.httpclient.HttpClient.log", "requests")
    }

    override fun systemDefinition(): Map<String, Any> {
        return mutableMapOf(
            MESSAGE_CLIENT_PROCESS_NAME to "UNIT_TEST_PROCESS",
            STORAGE_STRATEGY to "SHAREPOINT",
            SHAREPOINT_ROOT_URL to "netorg209792.sharepoint.com",
            SHAREPOINT_SITE_URL to "",
            SHAREPOINT_FOLDER to "Shared Documents",
            SHAREPOINT_CLIENT_ID to "",
            SHAREPOINT_CLIENT_SECRET to "",
            SHAREPOINT_TENANT_ID to ""
        )
    }

    override fun createMockDictionary(): GenesisDictionary = prodDictionary()

    private fun sharePointFileStorage(): FileStorageManager<InputStream> = SharePointOnPremFileStorageManager(
        sharepointRootUrl = systemDefinition()[SHAREPOINT_ROOT_URL].toString(),
        siteUrl = systemDefinition()[SHAREPOINT_SITE_URL].toString(),
        folder = systemDefinition()[SHAREPOINT_FOLDER].toString(),
        clientId = systemDefinition()[SHAREPOINT_CLIENT_ID].toString(),
        clientSecret = systemDefinition()[SHAREPOINT_CLIENT_SECRET].toString(),
        tenantId = systemDefinition()[SHAREPOINT_TENANT_ID].toString(),
        db = asyncEntityDb
    ).also { it.initialise() }

    @Test
    fun `test storing file`() = runBlocking {
        val fileStorage = sharePointFileStorage()

        val storage = fileStorage.saveFile("hello.txt", inputStream("Hello World"), USERNAME)
        val loadFile = fileStorage.loadFile(storage.fileStorageId)?.useStream { String(it.readAllBytes()) }

        assert(storage.fileName == "hello.txt")
        assert(storage.locationDetails == SHARED_DOCUMENTS_LOCATION)
        assert(storage.createdBy == USERNAME)
        assert(storage.modifiedBy == USERNAME)
        assert(storage.createdAt == storage.modifiedAt)
        assert(loadFile == "Hello World")
    }

    @Test
    fun `test storing file - user aware`() = runBlocking {
        val fileStorage = sharePointFileStorage().userAware()
        val (storage, loadFile) = withUserContext(USERNAME, "test", null) {
            val storage = fileStorage.saveFile("hello.txt", inputStream("Hello World"))
            val loadFile = fileStorage.loadFile(storage.fileStorageId)?.useStream { String(it.readAllBytes()) }

            storage to loadFile
        }

        assert(storage.fileName == "hello.txt")
        assert(storage.locationDetails == SHARED_DOCUMENTS_LOCATION)
        assert(storage.createdBy == USERNAME)
        assert(storage.modifiedBy == USERNAME)
        assert(storage.createdAt == storage.modifiedAt)
        assert(loadFile == "Hello World")
    }

    @Test
    fun `test storage file getters`() = runBlocking {
        val fileStorage = sharePointFileStorage()

        val storage = fileStorage.saveFile("hello.txt", inputStream("Hello World"), USERNAME)
        val loadFile = fileStorage.loadFile(storage.fileStorageId)

        assert(loadFile?.fileStorageId == storage.fileStorageId)
        assert(loadFile?.storageManager == storage.storageManager)
        assert(loadFile?.fileName == storage.fileName)
        assert(loadFile?.modifiedAt == storage.modifiedAt)
        assert(loadFile?.modifiedBy == storage.modifiedBy)
        assert(loadFile?.createdBy == storage.createdBy)
        assert(loadFile?.createdAt == storage.createdAt)
        assert(loadFile?.locationDetails == storage.locationDetails)
    }

    @Test
    fun `test loading unknown file`() = runBlocking {
        val fileStorage = sharePointFileStorage()
        val loadFile = fileStorage.loadFile("abc")

        assert(loadFile == null)
    }

    @Test
    fun `test modify file`() = runBlocking {
        val fileStorage = sharePointFileStorage()

        val storage = fileStorage.saveFile("hello.txt", inputStream("Hello World"), USERNAME)
        fileStorage.modifyFile(storage.fileStorageId, inputStream("Good Morning"), "Tom")

        val loadFile = fileStorage.loadFile(storage.fileStorageId)?.useStream { String(it.readAllBytes()) }

        val record = asyncEntityDb.get(storage.byId())!!

        assert(storage.fileName == "hello.txt")
        assert(storage.locationDetails == SHARED_DOCUMENTS_LOCATION)
        assert(record.createdBy == USERNAME)
        assert(record.modifiedBy == "Tom")
        assert(record.modifiedAt > record.createdAt)

        assert(loadFile == "Good Morning")
    }

    @Test
    fun `test delete file`() = runBlocking {
        val fileStorage = sharePointFileStorage()

        val storage = fileStorage.saveFile("hello.txt", inputStream("Hello World"), USERNAME)
        delay(10000)
        fileStorage.deleteFile(storage.fileStorageId)

        val record = asyncEntityDb.get(storage.byId())

        assert(record == null)
    }

    private fun inputStream(s: String) = ByteArrayInputStream(s.toByteArray())
}
