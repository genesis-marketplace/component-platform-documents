package global.genesis.file.service

import global.genesis.commons.model.GenesisSet
import global.genesis.file.client.FileStorageClient
import global.genesis.file.message.event.EventGenerateDocumentContentReply
import global.genesis.file.message.event.GenerateDocumentContent
import global.genesis.file.storage.AbstractFileStorageManager
import global.genesis.gen.dao.FileStorage
import global.genesis.gen.dao.TemplateAsset
import global.genesis.gen.dao.Trade
import global.genesis.testsupport.AbstractGenesisTestSupport
import global.genesis.testsupport.GenesisTestConfig
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import javax.inject.Inject
import kotlin.io.path.absolute
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventGenerateDocumentContentHandlerTest : AbstractGenesisTestSupport<GenesisSet>(
    GenesisTestConfig {
        addPackageName("global.genesis.file")
        genesisHome = "/GenesisHome/"
        useTempClassloader = true
        parser = { it }
    }
) {
    private lateinit var tempFolder: Path
    override fun systemDefinition(): Map<String, Any> {
        if (!this::tempFolder.isInitialized) tempFolder = Files.createTempDirectory("localStorage")
        return mapOf(
            "STORAGE_STRATEGY" to "LOCAL",
            "LOCAL_STORAGE_FOLDER" to tempFolder.absolute()
        )
    }

    @Inject
    private lateinit var fileStorageManager: AbstractFileStorageManager

    @Inject
    private lateinit var fileStorageClient: FileStorageClient

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        // Use static date time so generated output is deterministic
        val dateTime = DateTime(1712827020972)
        entityDb.insert(
            Trade {
                tradeId = "12345"
                currencyId = "GBP"
                price = 1.2
                quantity = 1000
                tradeDate = dateTime
                tradeType = "Type"
            }
        )
    }

    private suspend fun insertFile(fileName: String): String {
        val dataFile = File("src/test/resources/$fileName")
        val dataFileStorage: FileStorage = fileStorageManager.saveFile(
            fileName.substringAfterLast('/'),
            dataFile.inputStream(),
            "SYSTEM"
        )
        return dataFileStorage.fileStorageId
    }

    @AfterEach
    fun clean() {
        if (!Files.isDirectory(tempFolder)) return
        Files.walkFileTree(
            tempFolder,
            object : SimpleFileVisitor<Path>() {
                override fun postVisitDirectory(
                    dir: Path,
                    exc: IOException?
                ): FileVisitResult {
                    Files.delete(dir)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes
                ): FileVisitResult {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }
            }
        )
    }

    @Test
    fun `generate document content - no assets, no injected entity`() = runBlocking<Unit> {
        // Sneakily load a file into storage by using an injected instance of the storage manager
        val storageId = insertFile("simple-email-template.html")

        // Then send an event to the API, and check the content is correct
        val event = GenerateDocumentContent(
            templateReference = storageId,
            dataContext = emptyMap()
        )
        val reply = fileStorageClient.generateDocumentContent(event, "JohnDoe", messageClient)
        assertIs<EventGenerateDocumentContentReply.EventGenerateDocumentContentAck>(reply)
        val expectedContent = File("src/test/resources/simple-email-template.html").readText()
        assertEquals(expectedContent, String(reply.content))
    }

    @Test
    fun `generate document content - no assets, injected entity`() = runBlocking {
        val storageId = insertFile("trade-report_template.html")

        val event = GenerateDocumentContent(
            templateReference = storageId,
            dataContext = emptyMap(),
            entityName = "TRADE",
            entityId = "12345"
        )
        val reply = fileStorageClient.generateDocumentContent(event, "JohnDoe", messageClient)
        assertIs<EventGenerateDocumentContentReply.EventGenerateDocumentContentAck>(reply)
        val expectedContent = File("src/test/resources/trade-report.html").readText()
        assertEquals(expectedContent, String(reply.content))
    }

    @Test
    fun `generate document content with assets and injected entity`() = runBlocking {
        val storageId = insertFile("trade-report-template-with-assets.html")
        val cssId = insertFile("trade-report-with-assets.css")
        val logoId = insertFile("templates-resources/genesis-logo.png")

        entityDb.insertAll(
            listOf(
                TemplateAsset {
                    templateId = storageId
                    assetId = cssId
                },
                TemplateAsset {
                    templateId = storageId
                    assetId = logoId
                }
            )
        )

        val event = GenerateDocumentContent(
            templateReference = storageId,
            dataContext = emptyMap(),
            entityName = "TRADE",
            entityId = "12345"
        )

        val reply = fileStorageClient.generateDocumentContent(event, "JohnDoe", messageClient)
        assertIs<EventGenerateDocumentContentReply.EventGenerateDocumentContentAck>(reply)

        val expectedContent = File("src/test/resources/trade-report-with-assets.html").readText()
        assertEquals(expectedContent, String(reply.content))

        assertEquals(2, reply.assets.size)
        val assetsByFileName = reply.assets.associateBy {
            it.fileName
        }
        var asset = assetsByFileName["trade-report-with-assets.css"]
        assertNotNull(asset)
        assertEquals(cssId, asset.fileStorageId)
        assertTrue(asset.fileContent.contentEquals(File("src/test/resources/trade-report-with-assets.css").readBytes()))

        asset = assetsByFileName["genesis-logo.png"]
        assertNotNull(asset)
        assertEquals(logoId, asset.fileStorageId)
        assertTrue(asset.fileContent.contentEquals(File("src/test/resources/templates-resources/genesis-logo.png").readBytes()))
    }
}
