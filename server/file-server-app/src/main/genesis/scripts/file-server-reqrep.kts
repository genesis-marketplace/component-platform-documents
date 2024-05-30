requestReplies {
    requestReply(FILE_STORAGE) {
        permissioning {
            permissionCodes("FileStorageView")
        }

        request {
            STORAGE_MANAGER
            FILE_NAME
        }

        reply {
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
