fields {

    field(name = "STORAGE_LOCATION_ID", type = STRING)
    field(name = "FULLY_QUALIFIED_NAME", type = STRING)
    field(name = "LOCATION_NAME", type = STRING)
    field(name = "STORAGE_MANAGER", type = STRING)

    field(name = "FILE_STORAGE_ID", type = STRING)
    field(name = "FILE_NAME", type = STRING, maxSize = 256)
    field(name = "FILE_SIZE", type = LONG)
    field(name = "MODIFIED_AT", type = DATETIME)
    field(name = "LOCATION_NAME", type = STRING)
    field(name = "MODIFIED_BY", type = STRING)
    field(name = "CREATED_BY", type = STRING)
    field(name = "LOCATION_DETAILS", type = STRING, maxSize = 1024)

    field(name = "TEMPLATE_ID", type = STRING)
    field(name = "ASSET_ID", type = STRING)
}
