systemDefinition {
    global {
        item("LOCAL_STORAGE_FOLDER", "LOCAL_STORAGE")
        item("STORAGE_STRATEGY", "LOCAL")
        item("S3_STORAGE_MODE", "DEV")
        item("S3_BUCKET_NAME", "")
        item("S3_FOLDER_PREFIX", "")

        // LOCAL
        item("AWS_HOST", "")

        // DEV
        item("AWS_REGION", "")
        item("AWS_ACCESS_KEY", "")
        item("AWS_SECRET_ACCESS_KEY", "")

        // SHAREPOINT
        item("SHAREPOINT_SITE_URL", "")
        item("SHAREPOINT_FOLDER", "Shared Documents")
        item("ACCESS_TOKEN", "")
    }
}