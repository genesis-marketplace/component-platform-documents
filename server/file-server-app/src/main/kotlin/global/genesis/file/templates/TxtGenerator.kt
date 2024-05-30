package global.genesis.file.templates

import com.google.inject.Inject
import global.genesis.db.rx.entity.multi.AsyncEntityDb
import global.genesis.environment.install.templating.MustacheParser
import global.genesis.file.storage.AbstractFileStorageManager
import java.nio.file.Path

/**
 * Utility class for generating a txt output based on a txt template pre-uploaded to FILE_STORAGE.
 *
 * This class uses the MustacheParser to process the template and substitute the provided data.
 * The resulting txt file can either be uploaded to file storage or it's content returned as a raw string alongside any
 * asset file storage ids linked to the original template.
 *
 * @author ZuhaaJamani
 */
class TxtGenerator @Inject constructor(
    db: AsyncEntityDb,
    private val abstractFileStorageManager: AbstractFileStorageManager,
    private val mustacheParser: MustacheParser
) : AbstractGenerator(db, abstractFileStorageManager) {

    suspend fun generateContent(documentContentConfiguration: DocumentContentConfiguration, workingDir: Path): DocumentContentResult {
        val (templateFile, assetIds) = getTemplateFileAndAssets(documentContentConfiguration.templateId, workingDir)

        return DocumentContentResult(mustacheParser.createStringFromTemplate(documentContentConfiguration.data, templateFile.readText()), assetIds)
    }

    suspend fun generateAndStore(documentStorageConfiguration: DocumentStorageConfiguration, workingDir: Path): DocumentStorageResult {
        val (templateFile, _) = getTemplateFileAndAssets(documentStorageConfiguration.templateId, workingDir)

        templateFile.writeText(mustacheParser.createStringFromTemplate(documentStorageConfiguration.data, templateFile.readText()))

        return getDocumentStorageResult(templateFile, abstractFileStorageManager, documentStorageConfiguration.userName)
    }
}
