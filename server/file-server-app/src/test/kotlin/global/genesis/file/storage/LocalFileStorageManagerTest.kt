package global.genesis.file.storage

import global.genesis.db.rx.entity.transactionaware.withUserContext
import global.genesis.db.util.AbstractDatabaseTest
import global.genesis.dictionary.GenesisDictionary
import global.genesis.gen.config.tables.FILE_STORAGE
import global.genesis.gen.dao.Trade
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.test.assertFailsWith

class LocalFileStorageManagerTest : AbstractDatabaseTest() {

    override fun createMockDictionary(): GenesisDictionary = prodDictionary()

    private val tempFolder = Files.createTempDirectory("localStorage")

    @AfterEach
    fun tearDown() {
        if (!Files.isDirectory(tempFolder)) return
        Files.walkFileTree(
            tempFolder,
            object : SimpleFileVisitor<Path>() {
                override fun postVisitDirectory(
                    dir: Path,
                    exc: IOException?
                ): FileVisitResult {
                    Files.delete(dir)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes
                ): FileVisitResult {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }
            }
        )
    }

    private fun localFileStorage(): FileStorageManager<InputStream> = LocalFileStorageManager(
        storagePath = tempFolder,
        db = asyncEntityDb
    ).also { it.initialise() }

    @Test
    fun `test storing file`() = runBlocking {
        val fileStorage = localFileStorage()

        val storage = fileStorage.saveFile("hello.txt", inputStream("Hello World"), "Peter")
        val loadFile = fileStorage.loadFile(storage.fileStorageId)?.useStream { String(it.readAllBytes()) }

        assert(storage.fileName == "hello.txt")
        assert(storage.locationDetails == "hello.txt")
        assert(storage.createdBy == "Peter")
        assert(storage.modifiedBy == "Peter")
        assert(storage.createdAt == storage.modifiedAt)
        assert(loadFile == "Hello World")
    }

    @Test
    fun `test storing file - user aware`() = runBlocking {
        val fileStorage = localFileStorage().userAware()
        val (storage, loadFile) = withUserContext("Peter", "test", null) {
            val storage = fileStorage.saveFile("hello.txt", inputStream("Hello World"))
            val loadFile = fileStorage.loadFile(storage.fileStorageId)?.useStream { String(it.readAllBytes()) }

            storage to loadFile
        }

        assert(storage.fileName == "hello.txt")
        assert(storage.locationDetails == "hello.txt")
        assert(storage.createdBy == "Peter")
        assert(storage.modifiedBy == "Peter")
        assert(storage.createdAt == storage.modifiedAt)
        assert(loadFile == "Hello World")
    }

    @Test
    fun `test storing file linked to trade`() = runBlocking {
        val fileStorage = localFileStorage()
        val link = Trade.ById("123")
        val storage = fileStorage.saveFile("hello.txt", inputStream("Hello World"), "Peter", link)

        val files = fileStorage.loadFiles(link)
            .map { storedFile -> storedFile.useStream { String(it.readAllBytes()) } }
            .toList()

        assert(files.size == 1)

        assert(storage.fileName == "hello.txt")
        assert(storage.locationDetails == "hello.txt")
        assert(storage.createdBy == "Peter")
        assert(storage.modifiedBy == "Peter")
        assert(storage.createdAt == storage.modifiedAt)
        assert(files.first() == "Hello World")
    }

    @Test
    fun `test storage file getters`() = runBlocking {
        val fileStorage = localFileStorage()

        val storage = fileStorage.saveFile("hello.txt", inputStream("Hello World"), "Peter")
        val loadFile = fileStorage.loadFile(storage.fileStorageId)

        assert(loadFile?.fileStorageId == storage.fileStorageId)
        assert(loadFile?.storageManager == storage.storageManager)
        assert(loadFile?.fileName == storage.fileName)
        assert(loadFile?.modifiedAt == storage.modifiedAt)
        assert(loadFile?.modifiedBy == storage.modifiedBy)
        assert(loadFile?.createdBy == storage.createdBy)
        assert(loadFile?.createdAt == storage.createdAt)
        assert(loadFile?.locationDetails == storage.locationDetails)
    }

    @Test
    fun `test storing deleted file`() = runBlocking<Unit> {
        val fileStorage = localFileStorage()

        val storage = fileStorage.saveFile("hello.txt", inputStream("Hello World"), "Peter")
        tearDown()

        assertFailsWith<FileNotFoundException> { fileStorage.loadFile(storage.fileStorageId) }
    }

    @Test
    fun `test loading unknown file`() = runBlocking {
        val fileStorage = localFileStorage()
        val loadFile = fileStorage.loadFile("abc")

        assert(loadFile == null)
    }

    @Test
    fun `test storing two files with the same name`() = runBlocking {
        val fileStorage = localFileStorage()

        val storage1 = fileStorage.saveFile("hello.txt", inputStream("Hello World"), "Peter")
        val storage2 = fileStorage.saveFile("hello.txt", inputStream("Hello World!"), "Peter")

        assert(storage1.fileStorageId != storage2.fileStorageId)
        assert(storage1.fileName != storage2.fileName)
        assert(storage1.locationDetails != storage2.locationDetails)

        val loadFile1 = fileStorage.loadFile(storage1.fileStorageId)?.useStream { String(it.readAllBytes()) }
        val loadFile2 = fileStorage.loadFile(storage2.fileStorageId)?.useStream { String(it.readAllBytes()) }

        assert(loadFile1 == "Hello World")
        assert(loadFile2 == "Hello World!")
    }

    @Test
    fun `test storing two files with the same name - load files by name`() = runBlocking {
        val fileStorage = localFileStorage()

        val storage1 = fileStorage.saveFile("hello.txt", inputStream("Hello World"), "Peter")
        val storage2 = fileStorage.saveFile("hello.txt", inputStream("Hello World!"), "Peter")

        assert(storage1.fileStorageId != storage2.fileStorageId)
        assert(storage1.locationDetails != storage2.locationDetails)

        val storageIds = asyncEntityDb.getBulk(FILE_STORAGE)
            .map { it.fileStorageId }
            .toList()

        assert(storage1.fileStorageId in storageIds)
        assert(storage2.fileStorageId in storageIds)
    }

    @Test
    fun `test modify file`() = runBlocking {
        val fileStorage = localFileStorage()

        val storage = fileStorage.saveFile("hello.txt", inputStream("Hello World"), "Peter")
        fileStorage.modifyFile(storage.fileStorageId, inputStream("Good Morning"), "Harry")

        val loadFile = fileStorage.loadFile(storage.fileStorageId)?.useStream { String(it.readAllBytes()) }

        val record = asyncEntityDb.get(storage.byId())!!

        assert(storage.fileName == "hello.txt")
        assert(storage.locationDetails == "hello.txt")
        assert(record.createdBy == "Peter")
        assert(record.modifiedBy == "Harry")
        assert(record.modifiedAt > record.createdAt)

        assert(loadFile == "Good Morning")
    }

    @Test
    fun `test delete file`() = runBlocking {
        val fileStorage = localFileStorage()

        val storage = fileStorage.saveFile("hello.txt", inputStream("Hello World"), "Peter")
        fileStorage.deleteFile(storage.fileStorageId)

        val record = asyncEntityDb.get(storage.byId())

        assert(record == null)
    }

    private fun inputStream(s: String) = ByteArrayInputStream(s.toByteArray())
}
