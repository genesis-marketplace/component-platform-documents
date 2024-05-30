dependencies {
    compileOnly(genesis("script-dependencies"))

    implementation(project(path = ":file-server-api"))
    implementation("org.thymeleaf:thymeleaf")
    implementation("com.openhtmltopdf:openhtmltopdf-core")
    implementation("com.openhtmltopdf:openhtmltopdf-pdfbox")
    implementation("org.jsoup:jsoup")
    implementation("com.amazonaws:aws-java-sdk-s3")
    implementation("com.microsoft.graph:microsoft-graph")
    implementation("com.azure:azure-identity")

    implementation("global.genesis:genesis-commons")
    implementation("global.genesis:genesis-db")
    implementation("global.genesis:genesis-clustersupport")
    implementation("global.genesis:genesis-pal-dataserver")
    implementation("global.genesis:genesis-pal-requestserver")

    api("global.genesis:genesis-db-server")
    api("global.genesis:genesis-environment")

    testImplementation("global.genesis:genesis-testsupport")
    testImplementation("org.apache.pdfbox:pdfbox")
    testImplementation(genesis("testsupport"))
    testImplementation("io.ktor:ktor-client-core")
    testImplementation("io.ktor:ktor-client-cio")
    testImplementation("io.ktor:ktor-client-content-negotiation")
    testImplementation("io.ktor:ktor-serialization-jackson")
    testImplementation("global.genesis:genesis-router")
    compileOnly(project(path = ":file-server-dictionary-cache", configuration = "codeGen"))
    testImplementation(project(path = ":file-server-test-dictionary-cache", configuration = "codeGen"))
}

description = "file-server-app"

sourceSets {
    main {
        resources {
            srcDirs("src/main/resources", "src/main/genesis")
        }
    }
}
