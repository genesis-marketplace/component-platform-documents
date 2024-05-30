package global.genesis.file.service

import global.genesis.commons.model.GenesisSet
import global.genesis.file.client.FileStorageClient
import global.genesis.file.message.request.FileContentRequest
import global.genesis.file.storage.AbstractFileStorageManager
import global.genesis.testsupport.AbstractGenesisTestSupport
import global.genesis.testsupport.GenesisTestConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import javax.inject.Inject
import kotlin.io.path.absolute
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FileContentRequestHandlerTest : AbstractGenesisTestSupport<GenesisSet>(
    GenesisTestConfig {
        addPackageName("global.genesis.file")
        genesisHome = "/GenesisHome/"
        parser = { it }
        useTempClassloader = true
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
    fun `request file content by id`() = runBlocking {
        // Sneakily load a file into storage by using an injected instance of the storage manager
        val file = fileStorageManager.saveFile(
            fileName = "test.txt",
            dataSource = ByteArrayInputStream("hello world".toByteArray()),
            userName = "user"
        )
        assertNotNull(file.fileStorageId)

        // Then send a request to the API, and check the content is correct
        val req = FileContentRequest(
            fileStorageIds = setOf(file.fileStorageId)
        )
        val reply = fileStorageClient.getFileContents(req, "JohnDoe", messageClient)
        assertEquals(1, reply.size)
        val fileReply = reply[0]
        assertEquals(file.fileStorageId, fileReply.fileStorageId)
        assertEquals("test.txt", fileReply.fileName)
        val expectedContent = "hello world".toByteArray()
        assertTrue(expectedContent.contentEquals(fileReply.fileContent))
    }

    @Test
    fun `request file content by name`() = runBlocking {
        // Sneakily load a file into storage by using an injected instance of the storage manager
        val file = fileStorageManager.saveFile(
            fileName = "test.txt",
            dataSource = ByteArrayInputStream("hello world".toByteArray()),
            userName = "user"
        )
        assertNotNull(file.fileStorageId)

        // Then send a request to the API, and check the content is correct
        val req = FileContentRequest(
            fileNames = setOf("test.txt")
        )
        val reply = fileStorageClient.getFileContents(req, "JohnDoe", messageClient)
        assertEquals(1, reply.size)
        val fileReply = reply[0]
        assertEquals(file.fileStorageId, fileReply.fileStorageId)
        assertEquals("test.txt", fileReply.fileName)
        val expectedContent = "hello world".toByteArray()
        assertTrue(expectedContent.contentEquals(fileReply.fileContent))
    }
}
