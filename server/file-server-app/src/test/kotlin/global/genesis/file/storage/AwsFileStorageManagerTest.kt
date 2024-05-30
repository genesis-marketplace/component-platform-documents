package global.genesis.file.storage

import com.amazonaws.SdkBaseException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import com.amazonaws.services.s3.model.DeleteObjectsResult
import com.amazonaws.services.s3.model.DeleteObjectsResult.DeletedObject
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult
import com.amazonaws.services.s3.model.PartETag
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.PutObjectResult
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.UploadPartRequest
import com.amazonaws.services.s3.model.UploadPartResult
import global.genesis.db.util.AbstractDatabaseTest
import global.genesis.dictionary.GenesisDictionary
import global.genesis.file.storage.aws.AwsFileStorageManager
import global.genesis.gen.dao.FileStorage
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

const val BUCKET = "bucket-name"
const val FOLDER_PREFIX = "/project-name/"
const val FILE_NAME = "hello.txt"
const val FILE_NAME_EXCLUDING_EXTENSION = "hello"
const val FILE_EXTENSION = ".txt"
const val FILE_PATH = "$FOLDER_PREFIX$FILE_NAME_EXCLUDING_EXTENSION$FILE_EXTENSION"
const val UPLOAD_ID = "1"
const val TAG = "SOME_TAG"
val FILE_CONTENTS_FOR_SINGLE_UPLOAD = stringOfLength(12)
val FILE_CONTENTS_FOR_MULTI_PART_UPLOAD = stringOfLength(FILE_CONTENTS_FOR_SINGLE_UPLOAD.length + 1)
val MAX_SINGLE_UPLOAD_SIZE = inputStream(FILE_CONTENTS_FOR_SINGLE_UPLOAD).available()
val UPLOAD_PARTS = listOf(PartETag(1, TAG), PartETag(2, TAG))
val COMPLETE_REQUEST = CompleteMultipartUploadRequest(BUCKET, FILE_PATH, UPLOAD_ID, UPLOAD_PARTS)

class AwsFileStorageManagerTest : AbstractDatabaseTest() {

    override fun createMockDictionary(): GenesisDictionary = prodDictionary()

    override fun systemDefinition(): Map<String, Any> {
        return mapOf(
            "STORAGE_STRATEGY" to "AWS",
            "S3_STORAGE_MODE" to "LOCAL",
            "AWS_HOST" to "http://some-host:8080",
            "S3_BUCKET_NAME" to "bucket-name",
            "S3_FOLDER_PREFIX" to "/project-name/"
        )
    }

    private lateinit var mockS3: AmazonS3
    private lateinit var fileStorageManager: FileStorageManager<InputStream>

    @BeforeEach
    fun setUp() {
        mockS3 = mock {}
        fileStorageManager = AwsFileStorageManager(
            asyncEntityDb,
            systemDefinitionService,
            mockS3,
            systemDefinitionService.get("S3_BUCKET_NAME").orElse(""),
            systemDefinitionService.get("S3_FOLDER_PREFIX").orElse(""),
            MAX_SINGLE_UPLOAD_SIZE
        ).also { it.initialise() }
    }

    @Test
    fun `test saving a file`() = runBlocking {
        mockS3ObjectLookUp(firstFilePathExists = false)
        mockS3SuccessfullySavingObject()

        val response = fileStorageManager.saveFile(FILE_NAME, inputStream(FILE_CONTENTS_FOR_SINGLE_UPLOAD), "Josh")

        assertEquals("REMOTE_STORAGE", response.storageManager)
        assertEquals(FILE_NAME, response.fileName)
        assertEquals(FILE_PATH, response.locationDetails)

        val captor = argumentCaptor<PutObjectRequest>()
        verify(mockS3).putObject(captor.capture())
        assertEquals(BUCKET, captor.firstValue.bucketName)
        assertEquals(FILE_PATH, captor.firstValue.key)
        assertEquals(FILE_EXTENSION, captor.firstValue.metadata.contentType)
        assertTrue(readInputStream(captor.firstValue.inputStream).contains(FILE_CONTENTS_FOR_SINGLE_UPLOAD))
    }

    @Test
    fun `test saving a file - multi part`() = runBlocking {
        mockS3ObjectLookUp(firstFilePathExists = false)
        mockSuccessfulS3MultipartUpload()

        val response = fileStorageManager.saveFile(FILE_NAME, inputStream(FILE_CONTENTS_FOR_MULTI_PART_UPLOAD), "Josh")

        assertEquals("REMOTE_STORAGE", response.storageManager)
        assertEquals(FILE_NAME, response.fileName)
        assertEquals(FILE_PATH, response.locationDetails)

        val initMultipartUploadCaptor = argumentCaptor<InitiateMultipartUploadRequest>()
        verify(mockS3).initiateMultipartUpload(initMultipartUploadCaptor.capture())
        assertEquals(BUCKET, initMultipartUploadCaptor.firstValue.bucketName)
        assertEquals(FILE_PATH, initMultipartUploadCaptor.firstValue.key)

        val uploadPartCaptor = argumentCaptor<UploadPartRequest>()
        verify(mockS3, times(2)).uploadPart(uploadPartCaptor.capture())
        assertEquals(BUCKET, uploadPartCaptor.firstValue.bucketName)
        assertEquals(FILE_PATH, uploadPartCaptor.firstValue.key)
        assertTrue(readInputStream(uploadPartCaptor.firstValue.inputStream).contains(FILE_CONTENTS_FOR_SINGLE_UPLOAD))

        val completeMultipartUploadCaptor = argumentCaptor<CompleteMultipartUploadRequest>()
        verify(mockS3).completeMultipartUpload(completeMultipartUploadCaptor.capture())
        assertEquals(BUCKET, completeMultipartUploadCaptor.firstValue.bucketName)
        assertEquals(FILE_PATH, completeMultipartUploadCaptor.firstValue.key)
        assertEquals(UPLOAD_ID, completeMultipartUploadCaptor.firstValue.uploadId)
        assertEquals(COMPLETE_REQUEST.partETags[0].eTag, completeMultipartUploadCaptor.firstValue.partETags[0].eTag)
        assertEquals(COMPLETE_REQUEST.partETags[0].partNumber, completeMultipartUploadCaptor.firstValue.partETags[0].partNumber)
        assertEquals(COMPLETE_REQUEST.partETags[1].eTag, completeMultipartUploadCaptor.firstValue.partETags[1].eTag)
        assertEquals(COMPLETE_REQUEST.partETags[1].partNumber, completeMultipartUploadCaptor.firstValue.partETags[1].partNumber)
    }

    @Test
    fun `test saving a file that already exists in bucket`() = runBlocking {
        mockS3ObjectLookUp(firstFilePathExists = true)
        mockS3SuccessfullySavingObject()

        val response = fileStorageManager.saveFile(FILE_NAME, inputStream(FILE_CONTENTS_FOR_SINGLE_UPLOAD), "Josh")
        val expectedFileName = "$FILE_NAME_EXCLUDING_EXTENSION(1)$FILE_EXTENSION"
        val expectedFilePath = "${FOLDER_PREFIX}$expectedFileName"

        assertEquals("REMOTE_STORAGE", response.storageManager)
        assertEquals(expectedFileName, response.fileName)
        assertEquals(expectedFilePath, response.locationDetails)

        val captor = argumentCaptor<PutObjectRequest>()
        verify(mockS3).putObject(captor.capture())
        assertEquals(BUCKET, captor.firstValue.bucketName)
        assertEquals(expectedFilePath, captor.firstValue.key)
        assertEquals(FILE_EXTENSION, captor.firstValue.metadata.contentType)
        assertTrue(readInputStream(captor.firstValue.inputStream).contains(FILE_CONTENTS_FOR_SINGLE_UPLOAD))
    }

    @Test
    fun `test saving a file that already exists in bucket - multi part`() = runBlocking {
        mockS3ObjectLookUp(firstFilePathExists = true)
        mockSuccessfulS3MultipartUpload()

        val response = fileStorageManager.saveFile(FILE_NAME, inputStream(FILE_CONTENTS_FOR_MULTI_PART_UPLOAD), "Josh")
        val expectedFileName = "$FILE_NAME_EXCLUDING_EXTENSION(1)$FILE_EXTENSION"
        val expectedFilePath = "${FOLDER_PREFIX}$expectedFileName"

        assertEquals("REMOTE_STORAGE", response.storageManager)
        assertEquals(expectedFileName, response.fileName)
        assertEquals(expectedFilePath, response.locationDetails)

        val createMultipartUploadCaptor = argumentCaptor<InitiateMultipartUploadRequest>()
        verify(mockS3).initiateMultipartUpload(createMultipartUploadCaptor.capture())
        assertEquals(BUCKET, createMultipartUploadCaptor.firstValue.bucketName)
        assertEquals(expectedFilePath, createMultipartUploadCaptor.firstValue.key)

        val uploadPartCaptor = argumentCaptor<UploadPartRequest>()
        verify(mockS3, times(2)).uploadPart(uploadPartCaptor.capture())
        assertEquals(BUCKET, uploadPartCaptor.firstValue.bucketName)
        assertEquals(expectedFilePath, uploadPartCaptor.firstValue.key)
        assertTrue(readInputStream(uploadPartCaptor.firstValue.inputStream).contains(FILE_CONTENTS_FOR_SINGLE_UPLOAD))

        val completeMultipartUploadCaptor = argumentCaptor<CompleteMultipartUploadRequest>()
        verify(mockS3).completeMultipartUpload(completeMultipartUploadCaptor.capture())
        assertEquals(BUCKET, completeMultipartUploadCaptor.firstValue.bucketName)
        assertEquals(expectedFilePath, completeMultipartUploadCaptor.firstValue.key)
        assertEquals(UPLOAD_ID, completeMultipartUploadCaptor.firstValue.uploadId)
        assertEquals(COMPLETE_REQUEST.partETags[0].eTag, completeMultipartUploadCaptor.firstValue.partETags[0].eTag)
        assertEquals(COMPLETE_REQUEST.partETags[0].partNumber, completeMultipartUploadCaptor.firstValue.partETags[0].partNumber)
        assertEquals(COMPLETE_REQUEST.partETags[1].eTag, completeMultipartUploadCaptor.firstValue.partETags[1].eTag)
        assertEquals(COMPLETE_REQUEST.partETags[1].partNumber, completeMultipartUploadCaptor.firstValue.partETags[1].partNumber)
    }

    @Test
    fun `test getting an aws server error when saving a file`() = runBlocking {
        mockS3ObjectLookUpAwsServerError()
        var passed = false

        try {
            fileStorageManager.saveFile(FILE_NAME, inputStream(FILE_CONTENTS_FOR_SINGLE_UPLOAD), "Josh")
        } catch (e: Exception) {
            passed = true
        }
        assertTrue(passed)
    }

    @Test
    fun `test getting an aws server error when saving a file - multi part`() = runBlocking {
        mockS3MultipartUploadAwsServerError()
        var passed = false

        try {
            fileStorageManager.saveFile(FILE_NAME, inputStream(FILE_CONTENTS_FOR_MULTI_PART_UPLOAD), "Josh")
        } catch (e: Exception) {
            passed = true
        }

        val captor = argumentCaptor<AbortMultipartUploadRequest>()
        assertTrue(passed)
        verify(mockS3).abortMultipartUpload(captor.capture())
        assertEquals(BUCKET, captor.firstValue.bucketName)
        assertEquals(FILE_PATH, captor.firstValue.key)
        assertEquals(UPLOAD_ID, captor.firstValue.uploadId)
    }

    @Test
    fun `test retrieving a file`() = runBlocking {
        val record = insertFileStorageRecord()
        mockS3GetObject()

        val response = fileStorageManager.loadFile(record.fileStorageId)!!

        assertEquals(record.fileStorageId, response.fileStorageId)
        assertEquals(FILE_CONTENTS_FOR_SINGLE_UPLOAD, String(response.inputStream.readAllBytes()))

        val getObjectCaptor = argumentCaptor<GetObjectRequest>()
        verify(mockS3).getObject(getObjectCaptor.capture())
        assertEquals(BUCKET, getObjectCaptor.firstValue.bucketName)
        assertEquals(FILE_PATH, getObjectCaptor.firstValue.key)
    }

    @Test
    fun `test retrieving a file that does not exist in bucket`() = runBlocking {
        val record = insertFileStorageRecord()
        mockS3GetObjectThatDoesNotExist()

        var passed = false
        try {
            fileStorageManager.loadFile(record.fileStorageId)!!
        } catch (e: Exception) {
            passed = true
        }
        assertTrue(passed)
    }

    @Test
    fun `test getting an aws server error when retrieving a file`() = runBlocking {
        val record = insertFileStorageRecord()
        mockS3GetObjectAwsServerError()

        var passed = false
        try {
            fileStorageManager.loadFile(record.fileStorageId)!!
        } catch (e: Exception) {
            passed = true
        }
        assertTrue(passed)
    }

    @Test
    fun `test updating a file that exists`() = runBlocking {
        val recordId = insertFileStorageRecord().fileStorageId
        mockS3SuccessfullySavingObject()

        val response = fileStorageManager.modifyFile(recordId, inputStream(FILE_CONTENTS_FOR_SINGLE_UPLOAD), "Josh")

        assertEquals(recordId, response.fileStorageId)
        assertEquals("REMOTE_STORAGE", response.storageManager)
        assertEquals(FILE_NAME, response.fileName)
        assertEquals(FILE_PATH, response.locationDetails)

        val captor = argumentCaptor<PutObjectRequest>()
        verify(mockS3).putObject(captor.capture())
        assertEquals(BUCKET, captor.firstValue.bucketName)
        assertEquals(FILE_PATH, captor.firstValue.key)
        assertEquals(FILE_EXTENSION, captor.firstValue.metadata.contentType)
        assertTrue(readInputStream(captor.firstValue.inputStream).contains(FILE_CONTENTS_FOR_SINGLE_UPLOAD))
    }

    @Test
    fun `test updating a file that does not exist`(): Unit = runBlocking {
        var passed = false
        try {
            fileStorageManager.modifyFile("nonExistingId", inputStream(FILE_CONTENTS_FOR_SINGLE_UPLOAD), "Josh")
        } catch (e: FileNotFoundException) {
            passed = true
        }

        assertTrue(passed)

        val captor = argumentCaptor<PutObjectRequest>()
        verify(mockS3, times(0)).putObject(captor.capture())
    }

    @Test
    fun `test updating a file that does not exist - multi part`() = runBlocking {
        val recordId = insertFileStorageRecord().fileStorageId
        mockSuccessfulS3MultipartUpload()

        val response = fileStorageManager.modifyFile(recordId, inputStream(FILE_CONTENTS_FOR_MULTI_PART_UPLOAD), "Josh")

        assertEquals(recordId, response.fileStorageId)
        assertEquals("REMOTE_STORAGE", response.storageManager)
        assertEquals(FILE_NAME, response.fileName)
        assertEquals(FILE_PATH, response.locationDetails)

        val createMultipartUploadCaptor = argumentCaptor<InitiateMultipartUploadRequest>()
        verify(mockS3).initiateMultipartUpload(createMultipartUploadCaptor.capture())
        assertEquals(BUCKET, createMultipartUploadCaptor.firstValue.bucketName)
        assertEquals(FILE_PATH, createMultipartUploadCaptor.firstValue.key)

        val uploadPartCaptor = argumentCaptor<UploadPartRequest>()
        verify(mockS3, times(2)).uploadPart(uploadPartCaptor.capture())
        assertEquals(BUCKET, uploadPartCaptor.firstValue.bucketName)
        assertEquals(FILE_PATH, uploadPartCaptor.firstValue.key)
        assertTrue(readInputStream(uploadPartCaptor.firstValue.inputStream).contains(FILE_CONTENTS_FOR_SINGLE_UPLOAD))

        val completeMultipartUploadCaptor = argumentCaptor<CompleteMultipartUploadRequest>()
        verify(mockS3).completeMultipartUpload(completeMultipartUploadCaptor.capture())
        assertEquals(BUCKET, completeMultipartUploadCaptor.firstValue.bucketName)
        assertEquals(FILE_PATH, completeMultipartUploadCaptor.firstValue.key)
        assertEquals(UPLOAD_ID, completeMultipartUploadCaptor.firstValue.uploadId)
        assertEquals(COMPLETE_REQUEST.partETags[0].eTag, completeMultipartUploadCaptor.firstValue.partETags[0].eTag)
        assertEquals(COMPLETE_REQUEST.partETags[0].partNumber, completeMultipartUploadCaptor.firstValue.partETags[0].partNumber)
        assertEquals(COMPLETE_REQUEST.partETags[1].eTag, completeMultipartUploadCaptor.firstValue.partETags[1].eTag)
        assertEquals(COMPLETE_REQUEST.partETags[1].partNumber, completeMultipartUploadCaptor.firstValue.partETags[1].partNumber)
    }

    @Test
    fun `test getting an aws server error when updating a file`() = runBlocking {
        val recordId = insertFileStorageRecord().fileStorageId
        mockS3SavingObjectAwsServerError()

        assertThrows<Exception> {
            fileStorageManager.modifyFile(recordId, inputStream(FILE_CONTENTS_FOR_SINGLE_UPLOAD), "Josh")
        }
    }

    @Test
    fun `test getting an aws server error when updating a file - multi part`() = runBlocking {
        val recordId = insertFileStorageRecord().fileStorageId
        mockS3MultipartUploadAwsServerError()

        assertThrows<Exception> {
            fileStorageManager.modifyFile(recordId, inputStream(FILE_CONTENTS_FOR_MULTI_PART_UPLOAD), "Josh")
        }

        val captor = argumentCaptor<AbortMultipartUploadRequest>()
        verify(mockS3).abortMultipartUpload(captor.capture())
        assertEquals(BUCKET, captor.firstValue.bucketName)
        assertEquals(FILE_PATH, captor.firstValue.key)
        assertEquals(UPLOAD_ID, captor.firstValue.uploadId)
    }

    @Test
    fun `test deleting a file`() = runBlocking {
        val recordId = insertFileStorageRecord().fileStorageId
        mockS3SuccessfullyDeletingObject()

        fileStorageManager.deleteFile(recordId)

        val deleteCaptor = argumentCaptor<DeleteObjectsRequest>()
        verify(mockS3).deleteObjects(deleteCaptor.capture())
        assertEquals(BUCKET, deleteCaptor.firstValue.bucketName)
        assertEquals(FILE_PATH, deleteCaptor.firstValue.keys[0].key)
    }

    @Test
    fun `test getting an aws server error when deleting a file`() = runBlocking {
        val recordId = insertFileStorageRecord().fileStorageId
        mockS3DeletingObjectAwsServerError()

        assertThrows<RuntimeException> {
            fileStorageManager.deleteFile(recordId)
        }

        val deleteCaptor = argumentCaptor<DeleteObjectsRequest>()
        verify(mockS3).deleteObjects(deleteCaptor.capture())
        assertEquals(BUCKET, deleteCaptor.firstValue.bucketName)
        assertEquals(FILE_PATH, deleteCaptor.firstValue.keys[0].key)
    }

    private fun insertFileStorageRecord() = runBlocking {
        val fileStorage = FileStorage {
            storageManager = "REMOTE_STORAGE"
            fileName = FILE_NAME
            fileSize = 1
            modifiedAt = DateTime.now()
            modifiedBy = "Josh"
            createdBy = "Josh"
            createdAt = DateTime.now()
            locationDetails = FILE_PATH
        }
        return@runBlocking asyncEntityDb.insert(fileStorage).record
    }

    private fun mockS3ObjectLookUp(firstFilePathExists: Boolean) {
        if (firstFilePathExists) {
            whenever(mockS3.doesObjectExist(any(), any()))
                .thenReturn(true).thenReturn(false)
        } else {
            whenever(mockS3.doesObjectExist(any(), any()))
                .thenReturn(false)
        }
    }

    private fun mockS3ObjectLookUpAwsServerError() = runBlocking {
        whenever(mockS3.doesObjectExist(any(), any())).thenThrow(SdkBaseException::class.java)
    }

    private fun mockS3SuccessfullySavingObject() = runBlocking {
        doReturn(PutObjectResult()).whenever(mockS3).putObject(any())
    }

    private fun mockSuccessfulS3MultipartUpload() = runBlocking {
        mockS3InitialisationOfMultipartUpload()
        mockUploadingPartOfS3Object()
        mockS3CompletionOfMultipartUpload()
    }

    private fun mockS3MultipartUploadAwsServerError() = runBlocking {
        mockS3InitialisationOfMultipartUpload()
        whenever(mockS3.uploadPart(any())).thenThrow(SdkBaseException::class.java)
    }

    private fun mockS3InitialisationOfMultipartUpload() = runBlocking {
        val result = InitiateMultipartUploadResult()
        result.uploadId = UPLOAD_ID
        doReturn(result).whenever(mockS3).initiateMultipartUpload(any())
    }

    private fun mockS3SuccessfullyDeletingObject() = runBlocking {
        val mockDeletedObject: DeletedObject = mock { on { key } doReturn FILE_PATH }
        doReturn(DeleteObjectsResult(listOf(mockDeletedObject))).whenever(mockS3).deleteObjects(any())
    }

    private fun mockS3DeletingObjectAwsServerError() = runBlocking {
        whenever(mockS3.deleteObjects(any())).thenThrow(SdkBaseException::class.java)
    }

    private fun mockS3SavingObjectAwsServerError() = runBlocking {
        whenever(mockS3.putObject(any())).thenThrow(SdkBaseException::class.java)
    }

    private fun mockUploadingPartOfS3Object() = runBlocking {
        val result = UploadPartResult()
        result.eTag = UPLOAD_PARTS[0].eTag
        result.partNumber = UPLOAD_PARTS[0].partNumber
        val result2 = UploadPartResult()
        result2.eTag = UPLOAD_PARTS[1].eTag
        result2.partNumber = UPLOAD_PARTS[1].partNumber
        doReturn(result).doReturn(result2).whenever(mockS3).uploadPart(any())
    }

    private fun mockS3CompletionOfMultipartUpload() = runBlocking {
        val result = CompleteMultipartUploadResult()
        result.key = FILE_PATH
        doReturn(result).whenever(mockS3).completeMultipartUpload(any())
    }

    private fun mockS3GetObject() = runBlocking {
        val result = S3Object()
        result.setObjectContent(ByteArrayInputStream(FILE_CONTENTS_FOR_SINGLE_UPLOAD.toByteArray()))
        doReturn(result).whenever(mockS3).getObject(any())
    }

    private fun mockS3GetObjectThatDoesNotExist() = runBlocking {
        doReturn(null).whenever(mockS3).getObject(any())
    }

    private fun mockS3GetObjectAwsServerError() = runBlocking {
        whenever(mockS3.getObject(any())).thenThrow(SdkBaseException::class.java)
    }

    private fun readInputStream(inputStream: InputStream) = String(inputStream.readAllBytes())
}

fun stringOfLength(x: Int): String {
    val builder = StringBuilder()
    for (i in 1..x) {
        builder.append(i.toString())
    }
    return builder.toString()
}

private fun inputStream(s: String) = ByteArrayInputStream(s.toByteArray())
