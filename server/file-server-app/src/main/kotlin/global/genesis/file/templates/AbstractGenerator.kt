package global.genesis.file.templates

import global.genesis.db.rx.entity.multi.AsyncEntityDb
import global.genesis.file.storage.AbstractFileStorageManager
import global.genesis.gen.dao.FileStorage
import global.genesis.gen.dao.TemplateAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.file.FileSystems
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class AbstractGenerator(private val db: AsyncEntityDb, private val abstractFileStorageManager: AbstractFileStorageManager) {

    protected suspend fun getTemplateFileAndAssets(templateId: String, workingDir: Path): Pair<File, List<String>> {
        val templateFile = abstractFileStorageManager.loadFile(templateId)
        require(templateFile != null) { "Template could not be found in file storage for id: $templateId" }

        val assetIds = db.getRange(TemplateAsset.byTemplateIdAssetId(templateId)).map { it: TemplateAsset ->
            it.assetId
        }.toList()

        val outputFile = File(getFilePath(workingDir, templateFile.fileName))
        templateFile.inputStream.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return Pair(outputFile, assetIds)
    }

    protected fun getFilePath(workingDir: Path, fileName: String): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HH-mm-ss")
        return "$workingDir${FileSystems.getDefault().separator}${LocalDateTime.now().format(formatter)}_$fileName"
    }

    protected suspend fun getDocumentStorageResult(file: File, abstractFileStorageManager: AbstractFileStorageManager, userName: String): DocumentStorageResult {
        val fileStorage: FileStorage = withContext(Dispatchers.IO) {
            abstractFileStorageManager.saveFile(file.name, FileInputStream(file), userName)
        }

        return DocumentStorageResult(fileStorage.fileStorageId)
    }
}
