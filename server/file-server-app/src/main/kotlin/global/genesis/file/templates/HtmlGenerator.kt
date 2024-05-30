package global.genesis.file.templates

import com.google.inject.Inject
import global.genesis.db.rx.entity.multi.AsyncEntityDb
import global.genesis.file.storage.AbstractFileStorageManager
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templateresolver.FileTemplateResolver
import java.nio.file.Path
import java.util.Locale

/**
 * Utility class for generating an HTML output based on a html template pre-uploaded to FILE_STORAGE.
 *
 * This class uses Thymeleaf to process the template and substitute the provided data.
 * The resulting HTML file can either be uploaded to file storage or it's content returned as a raw string alongside any
 * asset file storage ids linked to the original template.
 *
 * @author ZuhaaJamani
 */
class HtmlGenerator @Inject constructor(
    db: AsyncEntityDb,
    private val abstractFileStorageManager: AbstractFileStorageManager
) : AbstractGenerator(db, abstractFileStorageManager) {

    private val engine: TemplateEngine = TemplateEngine()

    init {
        val fileTemplateResolver = FileTemplateResolver()
        fileTemplateResolver.suffix = ".html"
        engine.setTemplateResolver(fileTemplateResolver)
    }

    suspend fun generateContent(
        documentContentConfiguration: DocumentContentConfiguration,
        workingDir: Path
    ): DocumentContentResult {
        val (templateFile, assetIds) = getTemplateFileAndAssets(
            documentContentConfiguration.templateId,
            workingDir
        )

        return DocumentContentResult(
            engine.process(
                templateFile.absolutePath,
                Context(Locale.ENGLISH, documentContentConfiguration.data)
            ),
            assetIds
        )
    }

    suspend fun generateAndStore(documentStorageConfiguration: DocumentStorageConfiguration, workingDir: Path): DocumentStorageResult {
        val (templateFile, _) = getTemplateFileAndAssets(documentStorageConfiguration.templateId, workingDir)

        val processedText = engine.process(templateFile.absolutePath, Context(Locale.ENGLISH, documentStorageConfiguration.data))
        templateFile.writeText(processedText)

        return getDocumentStorageResult(templateFile, abstractFileStorageManager, documentStorageConfiguration.userName)
    }
}
