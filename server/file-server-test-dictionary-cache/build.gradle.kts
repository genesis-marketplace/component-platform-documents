description = "file-server-test-dictionary-cache"

tasks {
    artifactoryPublish {
        enabled = false
    }
}

subprojects {
    tasks {
        artifactoryPublish {
            enabled = false
        }
    }
}
