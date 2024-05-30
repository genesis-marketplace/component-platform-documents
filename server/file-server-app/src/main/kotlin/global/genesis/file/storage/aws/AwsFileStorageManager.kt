package global.genesis.file.storage.aws

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PartETag
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.UploadPartRequest
import global.genesis.config.system.SystemDefinitionService
import global.genesis.db.rx.entity.multi.AsyncEntityDb
import global.genesis.file.storage.AbstractFileStorageManager
import global.genesis.file.storage.InputStreamProvider
import global.genesis.file.storage.data.InitMultipartUploadResult
import global.genesis.file.storage.data.OverwriteBehaviour
import global.genesis.file.storage.data.RemotePath
import global.genesis.file.storage.data.StorageDetails
import global.genesis.file.storage.data.UploadedParts
import global.genesis.file.storage.util.RemoteStorageManagerUtils.toRemotePath
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.jvm.optionals.getOrElse

private const val S3_MINIMUM_MUTLI_PART_UPLOAD_SIZE_IN_BYTES: Int = 1_048_576 // 1 MiB in bytes

class AwsFileStorageManager(
    db: AsyncEntityDb,
    private val definitionService: SystemDefinitionService,
    private val s3: AmazonS3,
    private val bucketName: String,
    private val folderPrefix: String,
    private val singleUploadMaxSize: Int = S3_MINIMUM_MUTLI_PART_UPLOAD_SIZE_IN_BYTES
) : AbstractFileStorageManager(
    db = db
) {
    override val storageManager: String
        get() = "REMOTE_STORAGE"

    override suspend fun init() {
        initialize()
    }

    override suspend fun saveFileStream(
        originalName: String,
        inputStream: InputStream
    ) = saveOrUpdate(bucketName, toRemotePath(folderPrefix, originalName), inputStream, OverwriteBehaviour.FORBID)

    override suspend fun openFileStream(
        storageDetails: StorageDetails
    ): InputStreamProvider? {
        return when (val inputStream = getObject(toRemotePath(folderPrefix, storageDetails.fileName), bucketName)) {
            null -> null
            else -> ByteArrayStreamProvider(inputStream)
        }
    }

    override suspend fun replaceFileStream(
        storageDetails: StorageDetails,
        inputStream: InputStream
    ): StorageDetails {
        return saveOrUpdate(
            bucketName,
            toRemotePath(folderPrefix, storageDetails.fileName),
            inputStream,
            OverwriteBehaviour.ALLOW
        )
    }

    override suspend fun deleteFileStream(storageDetails: StorageDetails) {
        deleteObject(toRemotePath(folderPrefix, storageDetails.fileName), bucketName)
    }

    private fun saveOrUpdate(
        bucketName: String,
        remotePath: RemotePath,
        inputStream: InputStream,
        overwriteBehaviour: OverwriteBehaviour
    ): StorageDetails {
        val resolvedRemotePath = resolvedRemotePath(bucketName, remotePath, overwriteBehaviour)
        return when (inputStream.available().toLong() > singleUploadMaxSize) {
            false -> saveObject(resolvedRemotePath, bucketName, inputStream)
            true -> saveObjectInParts(resolvedRemotePath, bucketName, inputStream)
        }
    }

    private fun resolvedRemotePath(
        bucketName: String,
        remotePath: RemotePath,
        overwriteBehaviour: OverwriteBehaviour
    ): RemotePath {
        return when (overwriteBehaviour) {
            OverwriteBehaviour.ALLOW -> remotePath
            OverwriteBehaviour.FORBID -> when (doesObjectExist(remotePath.fullPath, bucketName)) {
                true -> resolveUniquePath(remotePath, bucketName)
                false -> remotePath
            }
        }
    }

    private fun doesObjectExist(
        fullPath: String,
        bucketName: String
    ): Boolean {
        return try {
            s3.doesObjectExist(bucketName, fullPath)
        } catch (e: Exception) {
            LOG.error("Failed to check if object exist.", e)
            throw e
        }
    }

    private fun resolveUniquePath(
        remotePath: RemotePath,
        bucketName: String
    ): RemotePath {
        var attempt = 1
        var doesExist = true
        var potentialUniqueFileName = ""
        var potentialUniqueFullPath: String?
        while (doesExist) {
            potentialUniqueFileName = "${remotePath.fileNameExcludingExtension}($attempt)${remotePath.fileExtension}"
            potentialUniqueFullPath = "${remotePath.directoryPath}${remotePath.fileNameExcludingExtension}($attempt)${remotePath.fileExtension}"
            when (doesObjectExist(potentialUniqueFullPath, bucketName)) {
                true -> attempt++
                false -> doesExist = false
            }
        }
        return toRemotePath(remotePath.directoryPath, potentialUniqueFileName)
    }

    private fun saveObject(
        remotePath: RemotePath,
        bucketName: String,
        inputStream: InputStream
    ): StorageDetails {
        return try {
            val metadata = ObjectMetadata()
            metadata.contentType = remotePath.fileExtension
            metadata.contentLength = inputStream.available().toLong()

            s3.putObject(PutObjectRequest(bucketName, remotePath.fullPath, inputStream, metadata))
            StorageDetails(fileName = remotePath.fileName, filePath = remotePath.fullPath)
        } catch (e: Exception) {
            LOG.error("Failed to save object to S3.", e)
            throw e
        }
    }

    private fun saveObjectInParts(
        remotePath: RemotePath,
        bucketName: String,
        inputStream: InputStream
    ): StorageDetails {
        lateinit var uploadId: String
        return try {
            val initResult = initMultipartUpload(bucketName, remotePath)
            uploadId = initResult.uploadId
            val uploadedParts = writeToBucketInParts(initResult, inputStream)
            completeMultipartUpload(uploadedParts)
            StorageDetails(remotePath.fileName, remotePath.fullPath)
        } catch (e: Exception) {
            LOG.error("Failed to save object to S3.", e)
            abortMultiPartUpload(bucketName, remotePath.fullPath, uploadId)
            throw e
        }
    }

    private fun initMultipartUpload(
        bucketName: String,
        remotePath: RemotePath
    ): InitMultipartUploadResult {
        return try {
            val metadata = ObjectMetadata()
            metadata.contentType = remotePath.fileExtension

            val request = InitiateMultipartUploadRequest(bucketName, remotePath.fullPath, metadata)
            val result = s3.initiateMultipartUpload(request)
            InitMultipartUploadResult(
                uploadId = result.uploadId!!,
                fileName = remotePath.fileName,
                fullPath = remotePath.fullPath,
                bucketName = bucketName
            )
        } catch (e: Exception) {
            LOG.error("Failed to initialize multi part upload.", e)
            throw e
        }
    }

    private fun writeToBucketInParts(
        initResult: InitMultipartUploadResult,
        inputStream: InputStream
    ): UploadedParts<PartETag> {
        var counter = 1
        val parts = mutableListOf<PartETag>()
        while (true) {
            try {
                val buffer = ByteArray(singleUploadMaxSize)
                val chunkSize = inputStream.read(buffer)
                if (chunkSize == -1) break

                val uploadRequest = UploadPartRequest()
                    .withBucketName(initResult.bucketName)
                    .withKey(initResult.fullPath)
                    .withUploadId(initResult.uploadId)
                    .withPartNumber(counter)
                    .withPartSize(chunkSize.toLong())
                    .withInputStream(ByteArrayInputStream(buffer))
                val result = s3.uploadPart(uploadRequest)
                parts.add(result.partETag)
                if (chunkSize < buffer.size) break
                counter++
            } catch (e: Exception) {
                LOG.error("Failed to upload file part.", e)
                throw e
            }
        }

        return UploadedParts(
            uploadId = initResult.uploadId,
            bucketName = initResult.bucketName,
            fileName = initResult.fileName,
            fullPath = initResult.fullPath,
            parts = parts
        )
    }

    private fun completeMultipartUpload(uploadedParts: UploadedParts<PartETag>) {
        val bucketName = uploadedParts.bucketName
        val fullPath = uploadedParts.fullPath
        val uploadId = uploadedParts.uploadId
        val parts = uploadedParts.parts

        try {
            s3.completeMultipartUpload(
                CompleteMultipartUploadRequest(
                    bucketName,
                    fullPath,
                    uploadId,
                    parts
                )
            )
        } catch (e: Exception) {
            LOG.error("Failed to complete multi part upload.", e)
        }
    }

    private fun deleteObject(
        remotePath: RemotePath,
        bucketName: String
    ) {
        try {
            val request = DeleteObjectsRequest(bucketName).withKeys(remotePath.fullPath)
            s3.deleteObjects(request)
        } catch (e: Exception) {
            LOG.error("Failed to delete object: ${remotePath.fullPath}.", e)
            throw e
        }
    }

    private fun getObject(
        remotePath: RemotePath,
        bucketName: String
    ): InputStream? {
        return try {
            val s3Object = s3.getObject(GetObjectRequest(bucketName, remotePath.fullPath))
            return s3Object.objectContent
        } catch (e: Exception) {
            LOG.error("Failed to get object: ${remotePath.fullPath}.", e)
            null
        }
    }

    private fun abortMultiPartUpload(
        bucketName: String?,
        fullPath: String?,
        uploadId: String?
    ) {
        try {
            val req = AbortMultipartUploadRequest(bucketName, fullPath, uploadId)
            LOG.info("Attempting to abort multi part upload.")
            s3.abortMultipartUpload(req)
        } catch (e: Exception) {
            LOG.error("Failed to abort multi part upload.", e)
            throw e
        }
    }

    private fun initialize() {
        val storageStrategy = definitionService.get("STORAGE_STRATEGY").getOrElse { "" }
        val s3StorageMode = definitionService.get("S3_STORAGE_MODE").getOrElse { "" }
        val awsRegion = definitionService.get("AWS_REGION").getOrElse { "" }
        val awsAccessKey = definitionService.get("AWS_ACCESS_KEY").getOrElse { "" }
        val awsSecretAccessKey = definitionService.get("AWS_SECRET_ACCESS_KEY").getOrElse { "" }
        val awsHost = definitionService.get("AWS_HOST").getOrElse { "" }

        require(storageStrategy.isNotBlank()) { "STORAGE_STRATEGY must be set." }
        require(s3StorageMode.isNotBlank()) { "S3_STORAGE_MODE must be set." }
        require(bucketName.isNotBlank()) { "S3_BUCKET_NAME must be set." }
        require(s3StorageMode in listOf("DEV", "LOCAL", "AWS")) { "S3_STORAGE_MODE must be either DEV, LOCAL or AWS." }

        when (s3StorageMode) {
            "DEV" -> {
                require(awsRegion.isNotBlank()) { "AWS_REGION must be set." }
                require(awsAccessKey.isNotBlank()) { "AWS_ACCESS_KEY must be set." }
                require(awsSecretAccessKey.isNotBlank()) { "AWS_SECRET_ACCESS_KEY must be set." }
            }
            "LOCAL" -> {
                require(awsHost.isNotBlank()) { "AWS_HOST must be set." }
            }
        }
    }

    private inner class ByteArrayStreamProvider(
        private val inputStream: InputStream
    ) : InputStreamProvider {

        override fun invoke(): InputStream = inputStream
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AwsFileStorageManager::class.java)
    }
}
