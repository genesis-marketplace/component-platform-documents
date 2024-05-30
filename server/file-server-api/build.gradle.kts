dependencies {
    api("global.genesis:genesis-environment")
    testImplementation(genesis("testsupport"))
    compileOnly(project(path = ":file-server-dictionary-cache", configuration = "codeGen"))
    testImplementation(project(path = ":file-server-test-dictionary-cache", configuration = "codeGen"))
}

description = "file-server-api"
