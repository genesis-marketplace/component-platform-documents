package global.genesis.file.storage.provider

import com.google.inject.Inject
import com.google.inject.Singleton
import global.genesis.commons.annotation.ProviderOf
import global.genesis.config.system.SystemDefinitionService
import global.genesis.db.rx.entity.multi.AsyncEntityDb
import global.genesis.file.storage.AWS
import global.genesis.file.storage.AbstractFileStorageManager
import global.genesis.file.storage.LOCAL
import global.genesis.file.storage.LOCAL_STORAGE_FOLDER
import global.genesis.file.storage.LocalFileStorageManager
import global.genesis.file.storage.S3_BUCKET_NAME
import global.genesis.file.storage.S3_FOLDER_PREFIX
import global.genesis.file.storage.S3_STORAGE_MODE
import global.genesis.file.storage.SHAREPOINT
import global.genesis.file.storage.SHAREPOINT_CLIENT_ID
import global.genesis.file.storage.SHAREPOINT_CLIENT_SECRET
import global.genesis.file.storage.SHAREPOINT_FOLDER
import global.genesis.file.storage.SHAREPOINT_ON_PREM
import global.genesis.file.storage.SHAREPOINT_ROOT_URL
import global.genesis.file.storage.SHAREPOINT_SITE_ID
import global.genesis.file.storage.SHAREPOINT_SITE_URL
import global.genesis.file.storage.SHAREPOINT_TENANT_ID
import global.genesis.file.storage.STORAGE_STRATEGY
import global.genesis.file.storage.SharePointGraphFileStorageManager
import global.genesis.file.storage.SharePointOnPremFileStorageManager
import global.genesis.file.storage.aws.AwsFileStorageManager
import javax.inject.Provider
import kotlin.jvm.optionals.getOrElse

@Singleton
@ProviderOf(type = AbstractFileStorageManager::class)
class FileStorageProvider @Inject constructor(
    private val entityDb: AsyncEntityDb,
    private val definitionService: SystemDefinitionService
) : Provider<AbstractFileStorageManager> {

    override fun get(): AbstractFileStorageManager {
        val storageStrategy = definitionService.get(STORAGE_STRATEGY).getOrElse { "" }
        return when (storageStrategy) {
            LOCAL -> localFileStorage()
            AWS -> awsFileStorage(definitionService)
            SHAREPOINT_ON_PREM -> sharePointOnPremFileStorage()
            SHAREPOINT -> sharePointGraphFileStorage()

            else -> throw Exception("Unable to provide file storage manager as an unknown storage strategy was provided.")
        }
    }

    private fun sharePointOnPremFileStorage(): AbstractFileStorageManager = SharePointOnPremFileStorageManager(
        definitionService.get(SHAREPOINT_ROOT_URL).toString(),
        definitionService.get(SHAREPOINT_SITE_URL).get(),
        definitionService.get(SHAREPOINT_FOLDER).get(),
        definitionService.get(SHAREPOINT_CLIENT_ID).get(),
        definitionService.get(SHAREPOINT_CLIENT_SECRET).get(),
        definitionService.get(SHAREPOINT_TENANT_ID).get(),
        entityDb
    )

    private fun sharePointGraphFileStorage(): AbstractFileStorageManager = SharePointGraphFileStorageManager(
        definitionService.get(SHAREPOINT_SITE_ID).toString(),
        definitionService.get(SHAREPOINT_CLIENT_ID).toString(),
        definitionService.get(SHAREPOINT_CLIENT_SECRET).toString(),
        definitionService.get(SHAREPOINT_TENANT_ID).toString(),
        entityDb
    )

    private fun localFileStorage(): AbstractFileStorageManager = LocalFileStorageManager(
        definitionService.get(LOCAL_STORAGE_FOLDER).get(),
        entityDb
    )

    private fun awsFileStorage(definitionService: SystemDefinitionService): AbstractFileStorageManager = AwsFileStorageManager(
        entityDb,
        definitionService,
        AwsS3ClientBuilder.build(definitionService.get(S3_STORAGE_MODE).get(), definitionService),
        definitionService.get(S3_BUCKET_NAME).get(),
        definitionService.get(S3_FOLDER_PREFIX).getOrElse { "" }
    )
}
