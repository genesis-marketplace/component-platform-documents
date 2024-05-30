@file:ScriptModules("file-server-app")

webHandlers {
    val fileStorage = fileStorageProvider()

    config {
        requiresAuth = false
        logLevel = INFO
    }

    fileStorageDownloadEndpoint(storage = fileStorage) {
        permissioning {
            permissionCodes("FileStorageDownload")
        }
    }

    fileStorageUploadEndpoint(storage = fileStorage) {
        permissioning {
            permissionCodes("FileStorageUpload")
        }
    }

    endpoint(GET, "all-files") {
        permissioning {
            permissionCodes("FileStorageAdminAction")
        }
        handleRequest {
            db.getBulk(FILE_STORAGE)
        }
    }

    endpoint(DELETE, "delete") {
        permissioning {
            permissionCodes("FileStorageAdminAction")
        }
        val fileStorageId by queryParameter("fileStorageId")
        handleRequest {
            require(fileStorageId.isNotBlank()) { "Mandatory query parameter is missing: 'fileStorageId'" }
            fileStorage.deleteFile(fileStorageId)
            FileStorage.ById(fileStorageId)
        }
    }
}
