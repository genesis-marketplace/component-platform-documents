
dataServer {
    query(FILE_STORAGE) {
        permissioning {
            permissionCodes("FileStorageView")
        }

        config {
            compression = true
        }

        fields {
            FILE_STORAGE_ID
            STORAGE_MANAGER
            FILE_NAME
            MODIFIED_AT
            MODIFIED_BY
            CREATED_BY
            CREATED_AT
            FILE_SIZE
        }
    }
}
