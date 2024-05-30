package global.genesis.file

import global.genesis.commons.model.GenesisSet
import global.genesis.file.storage.AbstractFileStorageManager
import global.genesis.gen.dao.FileStorage
import global.genesis.jackson.core.GenesisJacksonMapper.Companion.jsonToObject
import global.genesis.testsupport.AbstractGenesisTestSupport
import global.genesis.testsupport.GenesisTestConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.apache.activemq.artemis.utils.FileUtil.deleteDirectory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.absolute
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class FileServerCRUDTest : AbstractGenesisTestSupport<GenesisSet>(
    GenesisTestConfig {
        addPackageName("global.genesis.router")
        addPackageName("global.genesis.file")
        genesisHome = "/GenesisHome/"
        scriptFileName = "genesis-router.kts"
        parser = { it }
        useTempClassloader = true
    }
) {

    private lateinit var tempPath: Path

    override fun systemDefinition(): Map<String, Any> {
        if (!this::tempPath.isInitialized) tempPath = Files.createTempDirectory("localStorage")
        return mapOf(
            "STORAGE_STRATEGY" to "LOCAL",
            "LOCAL_STORAGE_FOLDER" to tempPath.absolute()
        )
    }

    private val httpClient = HttpClient(CIO) { install(ContentNegotiation) { jackson() } }

    @Inject
    private lateinit var fileStorageManager: AbstractFileStorageManager

    @BeforeEach
    fun beforeEach() = validateSourceAndTargetWebHandlerScriptsEqual()

    @AfterEach
    fun clean() = deleteAllFilesFromLocalStorage()

    @Test
    fun `successfully upload a file`() = runBlocking {
        val expectedFileName = "hello.txt"
        val expectedFileContents = "This is amazing text inside the file"

        val httpResponse = httpClient.submitFormWithBinaryData(
            url = "http://localhost:9064/file-server/upload",
            formData = formData {
                append(
                    key = "key",
                    value = expectedFileContents.toByteArray(),
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "multipart/form-data")
                        append(HttpHeaders.ContentDisposition, "filename=$expectedFileName")
                    }
                )
            }
        )

        assert(HttpStatusCode.OK == httpResponse.status)
        assert(httpResponse.bodyAsText().contains("FILE_STORAGE_ID"))
        assertInStorage(expectedFileName, expectedFileContents)
    }

    @Test
    fun `successfully download a file`() = runBlocking {
        val expectedFileName = "hello.txt"
        val expectedFileContents = "This is amazing text inside the file"
        val inputStream = ByteArrayInputStream(expectedFileContents.toByteArray())
        val storedFile = fileStorageManager.saveFile(expectedFileName, inputStream, "userName")

        val httpResponse = httpClient.get(
            "http://localhost:9064/file-server/download?fileStorageId=${storedFile.fileStorageId}"
        )

        assert(expectedFileContents == httpResponse.bodyAsText())
        assert(HttpStatusCode.OK == httpResponse.status)
    }

    @Test
    fun `successfully retrieve all files meta data`() = runBlocking {
        val expectedFileName = "hello.txt"
        val inputStream = ByteArrayInputStream("some text".toByteArray())
        val storedFile = fileStorageManager.saveFile(expectedFileName, inputStream, "userName")

        val httpResponse = httpClient.get("http://localhost:9064/file-server/all-files")
        val responseBody = httpResponse.bodyAsText().jsonToObject<List<FileStorage>>()

        assert(HttpStatusCode.OK == httpResponse.status)
        assert(1 == responseBody.size)
        assert(expectedFileName == responseBody[0].fileName)
        assert(storedFile.fileStorageId == responseBody[0].fileStorageId)
    }

    @Test
    fun `successfully delete a file`() = runBlocking {
        val fileName = "hello.txt"
        val inputStream = ByteArrayInputStream("some text".toByteArray())
        val storedFile = fileStorageManager.saveFile(fileName, inputStream, "userName")

        when {
            storedFile.fileStorageId.isBlank() -> fail("Pre-condition not met: Expected file to be present in storage before assertions")
            else -> {
                val httpResponse = httpClient.delete(
                    "http://localhost:9064/file-server/delete?fileStorageId=${storedFile.fileStorageId}"
                )
                val responseBody = httpResponse.bodyAsText().jsonToObject<FileStorage.ById>()

                val files = fileStorageManager.loadFiles(storedFile.fileName).toList()
                assert(HttpStatusCode.OK == httpResponse.status)
                assert(FileStorage.ById(storedFile.fileStorageId) == responseBody)
                assertTrue(files.isEmpty())
            }
        }
    }

    private fun assertInStorage(fileName: String, fileContent: String) = runBlocking {
        val actualFile = fileStorageManager.loadFiles(fileName).toList().first()
        val expectedContent = fileContent.toByteArray()
        val inStorageAndCorrect = expectedContent.contentEquals(actualFile.inputStream.readAllBytes())
        assertTrue(inStorageAndCorrect)
    }

    private fun deleteAllFilesFromLocalStorage() {
        val tempFolder = tempPath.toFile()
        if (tempFolder.exists() && tempFolder.isDirectory) {
            tempFolder.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            }
            tempFolder.delete()
        }
    }

    // Currently unable to read in application level web-handler.kts script via AbstractGenesisTestSupport
    // Possible solutions, Extend Test support for web-handlers or extend gradle test task or AbstractGenesisTestSupport
    // to copy application level script into test/resources/GenesisHome/file-server/scripts/
    private fun validateSourceAndTargetWebHandlerScriptsEqual() {
        val applicationWebHandlerScriptText = Paths.get("").toAbsolutePath()
            .resolve("build/resources/main/scripts/file-server-web-handler.kts")
            .toFile()
            .readText()

        val testWebHandlerScriptText = Paths.get("").toAbsolutePath()
            .resolve("build/resources/test/GenesisHome/file-server/scripts/file-server-web-handler.kts")
            .toFile()
            .readText()
        assertEquals(applicationWebHandlerScriptText, testWebHandlerScriptText)
    }
}
