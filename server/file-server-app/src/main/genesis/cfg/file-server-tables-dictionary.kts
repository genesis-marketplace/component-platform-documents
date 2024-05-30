tables {
    table("FILE_STORAGE", 10001, audit = details(10002, "FS")) {
        sequence(FILE_STORAGE_ID, "FF")
        mappingStub()
        STORAGE_MANAGER not null
        FILE_NAME not null
        FILE_SIZE not null
        MODIFIED_AT not null
        MODIFIED_BY not null
        CREATED_BY not null
        CREATED_AT not null
        LOCATION_DETAILS not null
        IS_TEMPLATE

        primaryKey {
            FILE_STORAGE_ID
        }
        indices {
            nonUnique {
                STORAGE_MANAGER
                FILE_NAME
            }
        }
    }

    table("TEMPLATE_ASSET", 10003, audit = details(10004, "TA")) {
        TEMPLATE_ID
        ASSET_ID
        primaryKey {
            TEMPLATE_ID
            ASSET_ID
        }
        indices {
            nonUnique {
                TEMPLATE_ID
            }
        }
    }
}
