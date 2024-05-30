dependencies {
    implementation("global.genesis:genesis-pal-dataserver")
    implementation("global.genesis:genesis-pal-eventhandler")
    implementation("global.genesis:genesis-pal-streamerclient")
    implementation("global.genesis:genesis-pal-streamer")
    implementation("global.genesis:genesis-pal-requestserver")
    implementation("global.genesis:genesis-pal-camel")
    implementation("global.genesis:genesis-dbtest")
    implementation("global.genesis:genesis-testsupport")

    implementation(project(path = ":file-server-test-dictionary-cache", configuration = "codeGen"))
}

tasks {
    copyDependencies {
        enabled = false
    }
    artifactoryPublish {
        enabled = false
    }
}

description = "file-server-test-config"
