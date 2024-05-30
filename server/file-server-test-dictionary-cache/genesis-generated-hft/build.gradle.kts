description = "genesis-generated-hft"

codeGen {
    configModuleFilter = setOf("file-server-test-config", "file-server-app")
    useCleanerTask.set(((properties["useCleanerTask"] ?: "true") == "true"))
}
