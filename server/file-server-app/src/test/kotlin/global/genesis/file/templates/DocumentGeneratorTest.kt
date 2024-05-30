package global.genesis.file.templates

import global.genesis.commons.model.GenesisSet
import global.genesis.environment.install.templating.MustacheParser
import global.genesis.file.storage.AbstractFileStorageManager
import global.genesis.gen.dao.TemplateAsset
import global.genesis.pal.shared.inject
import global.genesis.testsupport.AbstractGenesisTestSupport
import global.genesis.testsupport.GenesisTestConfig
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.IOUtils
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DocumentGeneratorTest : AbstractGenesisTestSupport<GenesisSet>(
    GenesisTestConfig {
        addPackageName("global.genesis.file.templates")
        addPackageName("global.genesis.file.storage.provider")
        genesisHome = "${File.separator}GenesisHome${File.separator}"
        initialDataFile = "${File.separator}templates-resources${File.separator}template-generator-data.csv"
        parser = { it }
        useTempClassloader = true
    }
) {
    private lateinit var abstractFileStorageManager: AbstractFileStorageManager
    private lateinit var documentGenerator: DocumentGenerator
    private lateinit var htmlGenerator: HtmlGenerator
    private lateinit var txtGenerator: TxtGenerator
    private lateinit var pdfGenerator: PdfGenerator

    override fun systemDefinition(): Map<String, Any> = mapOf(
        "STORAGE_STRATEGY" to "LOCAL",
        "LOCAL_STORAGE_FOLDER" to createTempDirectory("LOCAL_STORAGE_FOLDER")
    )

    @BeforeEach
    fun setUp() {
        abstractFileStorageManager = bootstrap.injector.inject<AbstractFileStorageManager>()
        htmlGenerator = HtmlGenerator(entityDb, abstractFileStorageManager)
        pdfGenerator = PdfGenerator(entityDb, abstractFileStorageManager, htmlGenerator)
        txtGenerator = TxtGenerator(entityDb, abstractFileStorageManager, MustacheParser())
        documentGenerator = DocumentGenerator(abstractFileStorageManager, pdfGenerator, htmlGenerator, txtGenerator)
        Files.createDirectories(Path.of("build/file_gen"))
    }

    @Test
    fun `test generate and store pdf using provided working directory`(): Unit = runBlocking {
        Path.of("build/test_provided_dir").toFile().deleteRecursively()

        val templateFile = File("src/test/resources/templates-resources/simple-pdf-gen.html")
        val fileStorage = abstractFileStorageManager.saveFile("simple-pdf-gen.html", templateFile.inputStream(), "JohnDoe")

        val pdfStorageResult = documentGenerator.generateAndStore(DocumentStorageConfiguration(fileStorage.fileStorageId, "simple-pdf-gen.pdf", "JohnDoe", emptyMap(), false, "build/test_provided_dir"))

        val pdf = abstractFileStorageManager.loadFile(pdfStorageResult.fileStorageId)
        assertNotNull(pdf)

        val pdfFile = File("build/test_provided_dir/${pdf.fileName}")
        assertTrue(pdfFile.exists())
    }

    @Test
    fun `test generate simple pdf - no assets or data`(): Unit = runBlocking {
        val templateFile = File("src/test/resources/templates-resources/simple-pdf-gen.html")
        val fileStorage = abstractFileStorageManager.saveFile("simple-pdf-gen.html", templateFile.inputStream(), "JohnDoe")

        val pdfStorageResult = documentGenerator.generateAndStore(DocumentStorageConfiguration(fileStorage.fileStorageId, "simple-pdf-gen.pdf", "JohnDoe", emptyMap(), true))

        val pdf = abstractFileStorageManager.loadFile(pdfStorageResult.fileStorageId)
        assertNotNull(pdf)

        val pdfFile = File("build/file_gen/${pdf.fileName}")
        pdf.inputStream.use { input ->
            pdfFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val text = getPdfText(pdfFile)
        assertTrue(text.contains("A Blue Heading"))
        assertTrue(text.contains("A red paragraph."))
    }

    @Test
    fun `test generate pdf - with assets & data`(): Unit = runBlocking {
        val templateFile = File("src/test/resources/templates-resources/trades-report.html")
        val templateFileStorage = abstractFileStorageManager.saveFile("trades-report.html", templateFile.inputStream(), "JohnDoe")
        val cssFile = File("src/test/resources/templates-resources/trades.css")
        val cssFileStorage = abstractFileStorageManager.saveFile("trades.css", cssFile.inputStream(), "JohnDoe")
        entityDb.insert(TemplateAsset(templateFileStorage.fileStorageId, cssFileStorage.fileStorageId))
        val imageFile = File("src/test/resources/templates-resources/genesis-logo.png")
        val imageFileStorage = abstractFileStorageManager.saveFile("genesis-logo.png", imageFile.inputStream(), "JohnDoe")
        entityDb.insert(TemplateAsset(templateFileStorage.fileStorageId, imageFileStorage.fileStorageId))

        val pdfStorageResult = documentGenerator.generateAndStore(DocumentStorageConfiguration(templateFileStorage.fileStorageId, "trades-pdf-gen.pdf", "JohnDoe", getTradesData(), true))

        val pdf = abstractFileStorageManager.loadFile(pdfStorageResult.fileStorageId)
        assertNotNull(pdf)

        val pdfFile = File("build/file_gen/${pdf.fileName}")
        pdf.inputStream.use { input ->
            pdfFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val text = getPdfText(pdfFile)
        assertTrue(text.contains("551.6757180449 884 1 BUY 1"))
        assertTrue(text.contains("739.0135172755 958 10 SELL 10"))
    }

    @Test
    fun `generate and store file with template id that has not been uploaded to file storage shouldn't work`(): Unit = runBlocking {
        val exception = assertThrows<IllegalArgumentException> {
            documentGenerator.generateAndStore(
                DocumentStorageConfiguration("non_existent", "file_name.pdf", "JohnDoe", emptyMap(), true)
            )
        }
        assertEquals("Template could not be found in file storage for id: non_existent", exception.message)
    }

    @Test
    fun `generate content with template id that has not been uploaded to file storage shouldn't work`(): Unit = runBlocking {
        val exception = assertThrows<IllegalArgumentException> {
            documentGenerator.generateContent(
                DocumentContentConfiguration("non_existent", "JohnDoe", emptyMap(), true)
            )
        }
        assertEquals("Template could not be found in file storage for id: non_existent", exception.message)
    }

    @Test
    fun `generate file without file extension in file name shouldn't work`(): Unit = runBlocking {
        val exception = assertThrows<IllegalArgumentException> {
            documentGenerator.generateAndStore(
                DocumentStorageConfiguration("non_existent", "file_name", "JohnDoe", emptyMap(), true)
            )
        }
        assertEquals("File type not recognised for file name: file_name", exception.message)
    }

    @Test
    fun `generate html only`(): Unit = runBlocking {
        val templateFile = File("src/test/resources/templates-resources/trades-report.html")
        val templateFileStorage = abstractFileStorageManager.saveFile("trades-report.html", templateFile.inputStream(), "JohnDoe")

        val htmlStorageResult = documentGenerator.generateAndStore(DocumentStorageConfiguration(templateFileStorage.fileStorageId, "trades-pdf-gen.html", "JohnDoe", getTradesData(), true))

        val htmlFileStorage = abstractFileStorageManager.loadFile(htmlStorageResult.fileStorageId)
        assertNotNull(htmlFileStorage)

        val result = IOUtils.toString(htmlFileStorage.inputStream, StandardCharsets.UTF_8)
        assertTrue(result.contains("<td>551.6757180449</td>"))
        assertTrue(result.contains("<td>884</td>"))
        assertTrue(result.contains("<td>1</td>"))
        assertTrue(result.contains("<td>BUY</td>"))
    }

    @Test
    fun `generate html content + asset only`(): Unit = runBlocking {
        val templateFile = File("src/test/resources/templates-resources/trades-report.html")
        val templateFileStorage = abstractFileStorageManager.saveFile("trades-report.html", templateFile.inputStream(), "JohnDoe")

        val imageFile = File("src/test/resources/templates-resources/genesis-logo.png")
        val imageFileStorage = abstractFileStorageManager.saveFile("genesis-logo.png", imageFile.inputStream(), "JohnDoe")
        entityDb.insert(TemplateAsset(templateFileStorage.fileStorageId, imageFileStorage.fileStorageId))

        val htmlContentResult = documentGenerator.generateContent(
            DocumentContentConfiguration(
                templateFileStorage.fileStorageId,
                "JohnDoe",
                mapOf("trades" to Trade(price = BigDecimal.ONE, quantity = 1, counterparty = "counterparty", side = Side.BUY, account = "account")),
                true
            )
        )

        assertTrue(htmlContentResult.rawContent.contains("<td>1</td>"))
        assertTrue(htmlContentResult.rawContent.contains("<td>counterparty</td>"))
        assertTrue(htmlContentResult.rawContent.contains("<td>BUY</td>"))
        assertTrue(htmlContentResult.rawContent.contains("<td>account</td>"))

        assertTrue(htmlContentResult.assetIds.contains(imageFileStorage.fileStorageId))
    }

    @Test
    fun `generate file with txt template file`(): Unit = runBlocking {
        val templateFile = File("src/test/resources/templates-resources/text-template.txt")
        val templateFileStorage = abstractFileStorageManager.saveFile("text-template.txt", templateFile.inputStream(), "JohnDoe")

        val txtStorageResult = documentGenerator.generateAndStore(
            DocumentStorageConfiguration(
                templateFileStorage.fileStorageId,
                "text-gen.txt",
                "JohnDoe",
                mapOf("PRICE" to 1, "QUANTITY" to 5, "COUNTERPARTY" to "counterparty_id", "SIDE" to Side.BUY, "ACCOUNT" to "account_id"),
                true
            )
        )

        val txtFileStorage = abstractFileStorageManager.loadFile(txtStorageResult.fileStorageId)
        assertNotNull(txtFileStorage)

        val result = IOUtils.toString(txtFileStorage.inputStream, StandardCharsets.UTF_8)
        assertTrue(result.contains("Price: 1"))
        assertTrue(result.contains("Quantity: 5"))
        assertTrue(result.contains("Counterparty: counterparty_id"))
        assertTrue(result.contains("Side: BUY"))
        assertTrue(result.contains("Account: account_id"))
    }

    @Test
    fun `generate content with txt template file`(): Unit = runBlocking {
        val templateFile = File("src/test/resources/templates-resources/text-template.txt")
        val templateFileStorage = abstractFileStorageManager.saveFile("text-template.txt", templateFile.inputStream(), "JohnDoe")

        val txtContentResult = documentGenerator.generateContent(
            DocumentContentConfiguration(
                templateFileStorage.fileStorageId,
                "JohnDoe",
                mapOf("PRICE" to 1, "QUANTITY" to 5, "COUNTERPARTY" to "counterparty_id", "SIDE" to Side.BUY, "ACCOUNT" to "account_id"),
                true
            )
        )

        assertTrue(txtContentResult.rawContent.contains("Price: 1"))
        assertTrue(txtContentResult.rawContent.contains("Quantity: 5"))
        assertTrue(txtContentResult.rawContent.contains("Counterparty: counterparty_id"))
        assertTrue(txtContentResult.rawContent.contains("Side: BUY"))
        assertTrue(txtContentResult.rawContent.contains("Account: account_id"))
    }

    private fun getPdfText(file: File): String {
        val document = PDDocument.load(file)
        val textStripper = PDFTextStripper()
        val text = textStripper.getText(document)
        document.close()
        return text
    }

    private fun getTradesData(): Map<String, Any> {
        val trades = File("src/test/resources/templates-resources/trades.csv")
            .readLines()
            .drop(1)
            .map {
                val (price, quantity, counterparty, side, account) = it.split(',')
                Trade(
                    price = price.toBigDecimal(),
                    quantity = quantity.toInt(),
                    counterparty = counterparty,
                    side = Side.valueOf(side),
                    account = account
                )
            }
        return mapOf(
            "trades" to trades
        )
    }

    enum class Side {
        BUY, SELL
    }

    data class Trade(
        val price: BigDecimal,
        val quantity: Int,
        val counterparty: String,
        val side: Side,
        val account: String
    )
}
