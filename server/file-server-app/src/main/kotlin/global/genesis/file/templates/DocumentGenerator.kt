package global.genesis.file.templates

import global.genesis.commons.annotation.Module
import global.genesis.file.storage.AbstractFileStorageManager
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.createTempDirectory

/**
 * Utility class for generating documents based on a pre-uploaded template to FILE_STORAGE.
 * It has the ability to generate the following:
 * html -> html [HtmlGenerator]
 * html -> pdf [PdfGenerator]
 * txt -> txt [TxtGenerator]
 *
 * Configuration should be provided by the user depending on whether they want to store the file [DocumentStorageConfiguration] or just receive it's content [DocumentContentConfiguration].
 * Users have the option to provide a working directory for which the interim files will be created, otherwise a temporary directory
 * will be created in the system - this is defaulted to be empty.
 * Users can also specify whether this directory should be deleted post document generation or not - this is defaulted to be true.
 *
 * It can either return the file storage id after being uploaded to file storage [DocumentStorageResult] or the raw string content of the output
 * as well as any storage ids for assets linked to the original template [DocumentContentResult].
 *
 * @author ZuhaaJamani
 */
@Module
class DocumentGenerator @Inject constructor(
    private val abstractFileStorageManager: AbstractFileStorageManager,
    private val pdfGenerator: PdfGenerator,
    private val htmlGenerator: HtmlGenerator,
    private val txtGenerator: TxtGenerator
) {

    suspend fun generateContent(documentContentConfiguration: DocumentContentConfiguration): DocumentContentResult {
        val workingDir = getWorkingDir(documentContentConfiguration.workingDirectory)

        val templateFile = abstractFileStorageManager.loadFile(documentContentConfiguration.templateId)
        require(templateFile != null) { "Template could not be found in file storage for id: ${documentContentConfiguration.templateId}" }

        val fileType = getFileType(templateFile.fileName)

        val contentResult: DocumentContentResult = when (fileType) {
            "html" -> htmlGenerator.generateContent(documentContentConfiguration, workingDir)
            "txt" -> txtGenerator.generateContent(documentContentConfiguration, workingDir)
            "pdf" -> throw IllegalArgumentException("PDF is not a supported template file type.")
            else -> throw IllegalArgumentException("File type not recognised for file name: ${templateFile.fileName}")
        }

        conditionallyDeleteDir(workingDir, documentContentConfiguration.deleteOnExit)

        return contentResult
    }

    suspend fun generateAndStore(documentStorageConfiguration: DocumentStorageConfiguration): DocumentStorageResult {
        val workingDir = getWorkingDir(documentStorageConfiguration.workingDirectory)

        val fileType = getFileType(documentStorageConfiguration.fileName)

        val storageResult: DocumentStorageResult = when (fileType) {
            "html" -> htmlGenerator.generateAndStore(documentStorageConfiguration, workingDir)
            "txt" -> txtGenerator.generateAndStore(documentStorageConfiguration, workingDir)
            "pdf" -> pdfGenerator.generatePdf(documentStorageConfiguration, workingDir)
            else -> throw IllegalArgumentException("File type not recognised for file name: ${documentStorageConfiguration.fileName}")
        }

        conditionallyDeleteDir(workingDir, documentStorageConfiguration.deleteOnExit)

        return storageResult
    }

    private fun getWorkingDir(workingDirPath: String): Path {
        return if (workingDirPath.isBlank()) {
            createTempDirectory("FILE_GENERATION_")
        } else {
            val dirPath = Path.of(workingDirPath)
            Files.createDirectories(dirPath)
        }
    }

    private fun conditionallyDeleteDir(workingDir: Path, deleteOnExit: Boolean) {
        if (deleteOnExit) {
            workingDir.toFile().deleteRecursively()
        }
    }

    private fun getFileType(fileName: String): String {
        val fileType = fileName.substringAfterLast(".")
        require(fileType.isNotEmpty()) { "File extension must be provided in template file name: $fileName" }
        return fileType
    }
}
