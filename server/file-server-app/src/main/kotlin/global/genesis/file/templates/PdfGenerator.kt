package global.genesis.file.templates

import com.google.inject.Inject
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import global.genesis.db.rx.entity.multi.AsyncEntityDb
import global.genesis.file.storage.AbstractFileStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.nio.file.FileSystems
import java.nio.file.Path

/**
 * Utility class for generating PDFs based on an HTML template pre-uploaded to FILE_STORAGE.
 *
 * The HTML template can contain assets such as css files, images etc. which would need to be uploaded
 * to FILE_STORAGE as well as TEMPLATE_ASSET which serves as a mapping table between the assets and
 * original template file.
 *
 * The PDF file will be uploaded to FILE_STORAGE and the file storage id will be returned.
 *
 * @author ZuhaaJamani
 */

class PdfGenerator @Inject constructor(
    db: AsyncEntityDb,
    private val abstractFileStorageManager: AbstractFileStorageManager,
    private val htmlGenerator: HtmlGenerator
) : AbstractGenerator(db, abstractFileStorageManager) {

    suspend fun generatePdf(documentStorageConfiguration: DocumentStorageConfiguration, workingDir: Path): DocumentStorageResult {
        val templateFileStorage = abstractFileStorageManager.loadFile(documentStorageConfiguration.templateId)
        require(templateFileStorage != null) { "Template could not be found in file storage for id: ${documentStorageConfiguration.templateId}" }

        val documentContentResult = htmlGenerator.generateContent(DocumentContentConfiguration(documentStorageConfiguration.templateId, documentStorageConfiguration.userName, documentStorageConfiguration.data), workingDir)
        val templateFile = File(getFilePath(workingDir, templateFileStorage.fileName))
        templateFile.writeText(documentContentResult.rawContent)

        val assetList = documentContentResult.assetIds.mapNotNull {
            val file = abstractFileStorageManager.loadFile(it)
            if (file == null) {
                LOG.warn("Asset could not be found in file storage for id: $it")
            }
            file
        }.toList()

        assetList.forEach { storedFile ->
            val file = File("$workingDir${FileSystems.getDefault().separator}${storedFile.fileName}")
            storedFile.inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        val document: Document = Jsoup.parse(templateFile, "UTF-8")
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml)

        val filePath = getFilePath(workingDir, documentStorageConfiguration.fileName)

        withContext(Dispatchers.IO) {
            FileOutputStream(filePath).use { os ->
                val builder = PdfRendererBuilder()
                    .withW3cDocument(W3CDom().fromJsoup(document), workingDir.toUri().toString())
                    .toStream(os)
                builder.run()
            }
        }

        return getDocumentStorageResult(File(filePath), abstractFileStorageManager, documentStorageConfiguration.userName)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PdfGenerator::class.java)
    }
}
