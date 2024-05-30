import org.apache.tools.ant.filters.FixCrLfFilter

ext.set("localDaogenVersion", "FILE_SERVER")

plugins {
    kotlin("jvm")
    `maven-publish`
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    distribution
    id("com.jfrog.artifactory")
    id("global.genesis.build")
    id("org.sonarqube") version "4.4.1.3373"
    id("org.gradle.test-retry") version "1.5.8"
    id("org.jetbrains.kotlinx.kover") version "0.7.6"
    id("com.bnorm.power.kotlin-power-assert") version "0.13.0"
}

val isCiServer = System.getenv().containsKey("CI")
val os = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem()

sonarqube {
    properties {
        property("sonar.projectKey", "genesislcap_genesis-file-server")
        property("sonar.projectName", "pbc-documents-server")
        property("sonar.organization", "genesislcap")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sourceEncoding", "UTF-8")
    }
}

project(":file-server-app") {
    sonarqube {
        properties {
            property("sonar.sources", "src/main")
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.gradle.maven-publish")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "com.jfrog.artifactory")
    apply(plugin = "org.sonarqube")
    apply(plugin = "org.gradle.test-retry")

    tasks {
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
                jvmTarget = "17"
            }
        }
        test {
            maxHeapSize = "2g"
            useJUnitPlatform()

            retry {
                maxRetries.set(5)
            }

            val testProperties = listOf(
                "DbLayer",
                "MqLayer",
                "DbHost",
                "DbUsername",
                "DbPassword",
                "AliasSource",
                "ClusterMode",
                "DictionarySource",
                "DbNamespace",
                "DbMode",
                "DbThreadsMax",
                "DbThreadsMin",
                "DbThreadKeepAliveSeconds",
                "DbSqlConnectionPoolSize",
                "DbQuotedIdentifiers"
            )
            // Add exports and opens so ChronicleQueue can continue working in JDK 17.
            // More info in: https://chronicle.software/chronicle-support-java-17/
            jvmArgs = jvmArgs!! + listOf(
                "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
                "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED",
                "--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac=ALL-UNNAMED",
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.nio=ALL-UNNAMED" // this one is opened for LMDB
            )
            val properties = System.getProperties()
            for (property in testProperties) {
                val value = properties.getProperty(property)
                    ?: ext.properties[property]?.toString()

                if (value != null) {
                    inputs.property(property, value)
                    systemProperty(property, value)
                }
            }
            if (os.isMacOsX) {
                // Needed to guarantee FDB java bindings will work as expected in MacOS
                environment("DYLD_LIBRARY_PATH", "/usr/local/lib")
            }
            // UK Locale changed in recent Java versions and the abbreviation for September is now Sept instead of Sep.
            // This cases our DumpTableFormattedTest.test dump table formatted to fail. Setting to COMPAT mode allows
            // same behaviour as Java 8. We should deal with this at some point.
            // More info here: https://bugs.openjdk.org/browse/JDK-8256837
            // And here: https://bugs.openjdk.org/browse/JDK-8273437
            systemProperty("java.locale.providers", "COMPAT")
            if (!isCiServer) {
                systemProperty("kotlinx.coroutines.debug", "")
            }
        }

        jar {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
    ktlint {
        filter {
            exclude { element -> element.file.path.contains("generated") || element.file.path.contains("internal-modules") }
        }
    }
    dependencies {
        implementation(platform("global.genesis:genesis-bom:${properties["genesisVersion"]}"))
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
        constraints {
            implementation("com.amazonaws:aws-java-sdk-s3:1.12.701")
            implementation("org.thymeleaf:thymeleaf:3.1.2.RELEASE")
            implementation("com.openhtmltopdf:openhtmltopdf-core:1.0.10")
            implementation("com.openhtmltopdf:openhtmltopdf-pdfbox:1.0.10")
            implementation("org.jsoup:jsoup:1.17.2")
            testImplementation("org.apache.pdfbox:pdfbox:3.0.2")
            implementation("com.microsoft.graph:microsoft-graph:5.80.0")
            implementation("com.azure:azure-identity:1.12.0")
        }
    }
}

distributions {
    main {
        contents {
            // Octal conversion for file permissions
            val libPermissions = "600".toInt(8)
            val scriptPermissions = "700".toInt(8)
            into("file-server/bin") {
                project.subprojects.forEach {
                    from("${it.buildDir}/libs")
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    exclude("file-server-test-*.jar")
                    exclude("file-server-config*.jar")
                    include("file-server-*.jar")
                }
            }
            into("file-server/lib") {
                from("${project.rootProject.buildDir}/dependencies")
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                exclude("genesis-*.jar")
                include("*.jar")

                fileMode = libPermissions
            }
            includeCfg(this)
            includeScripts(this, scriptPermissions)
            includeData(this)
            // Removes intermediate folder called with the same name as the zip archive.
            into("/")
        }
    }
}

val packageConfigFiles: TaskProvider<Jar> = tasks.register<Jar>("packConfigFiles")

tasks {
    distZip {
        for (subproject in subprojects) {
            dependsOn(subproject.tasks.named("assemble"))
        }
        archiveBaseName.set("genesisproduct-file-server")
        archiveClassifier.set("bin")
        archiveExtension.set("zip")
    }

    packageConfigFiles {
        dependsOn("createProductDetails", "createManifest")
        archiveClassifier.set("minimal")
        archiveExtension.set("zip")
        includeCfg(this)
        includeData(this)
        includeScripts(this, "700".toInt(8))
        exclude("**/*.java", "**/*.jar")
    }

    assemble {
        for (subproject in subprojects) {
            dependsOn(subproject.tasks.named("assemble"))
        }
    }
    build {
        for (subproject in subprojects) {
            dependsOn(subproject.tasks.named("build"))
        }
    }
    clean {
        for (subproject in subprojects) {
            dependsOn(subproject.tasks.named("clean"))
        }
    }
    test {
        for (subproject in subprojects) {
            dependsOn(subproject.tasks.named("test"))
        }
    }
    publishToMavenLocal {
        for (subproject in subprojects) {
            dependsOn(subproject.tasks.named("publishToMavenLocal"))
        }
    }
    ktlintFormat {
        for (subproject in subprojects) {
            dependsOn(subproject.tasks.named("ktlintFormat"))
        }
    }
}

allprojects {
    group = "global.genesis"

    apply(plugin = "org.jetbrains.kotlinx.kover")
    apply(plugin = "com.bnorm.power.kotlin-power-assert")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    repositories {
        // if (ext.has("USE_MVN_LOCAL")) {
        mavenLocal {
            // VERY IMPORTANT!!! EXCLUDE AGRONA AS IT IS A POM DEPENDENCY AND DOES NOT PLAY NICELY WITH MAVEN LOCAL!
            content {
                excludeGroup("org.agrona")
                // }
            }
        }
        mavenCentral()
        maven {
            url = uri("https://genesisglobal.jfrog.io/genesisglobal/dev-repo")
            credentials {
                username = properties["genesisArtifactoryUser"].toString()
                password = properties["genesisArtifactoryPassword"].toString()
            }
        }
        val repoKey = buildRepoKey()
        maven {
            url = uri("https://genesisglobal.jfrog.io/genesisglobal/$repoKey")
            credentials {
                username = properties["genesisArtifactoryUser"].toString()
                password = properties["genesisArtifactoryPassword"].toString()
            }
        }
    }

    publishing {
        publications.create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

artifactory {
    setContextUrl("https://genesisglobal.jfrog.io/genesisglobal")

    publish {
        repository {
            setRepoKey(buildRepoKey())
            setUsername(property("genesisArtifactoryUser").toString())
            setPassword(property("genesisArtifactoryPassword").toString())
        }
        defaults {
            publications("ALL_PUBLICATIONS")
            setPublishArtifacts(true)
            setPublishPom(true)
            isPublishBuildInfo = false
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("file-serverServerDistribution") {
            artifactId = "file-server-distribution"
            artifact(tasks.distZip.get())
        }
        create<MavenPublication>("file-serverMinimalDistribution") {
            artifactId = "file-server-distribution"
            artifact(packageConfigFiles.get())
        }
    }
}

koverReport {
    filters {
        excludes {
            packages("global.genesis.gen.*")
        }
    }
    defaults {
        html {
            onCheck = true
            setReportDir(layout.buildDirectory.dir("kover-reports/html-result"))
        }
        xml {
            onCheck = true
            setReportFile(layout.buildDirectory.file("kover-reports/result.xml"))
        }
    }
}

fun includeScripts(copySpec: CopySpec, scriptPermissions: Int) {
    copySpec.into("file-server/scripts") {
        from("${project.rootProject.projectDir}/file-server-app/src/main/genesis/scripts")
        filter(
            org.apache.tools.ant.filters.FixCrLfFilter::class,
            "eol" to FixCrLfFilter.CrLf.newInstance("lf")
        )
        fileMode = scriptPermissions
    }
}

fun includeCfg(copySpec: CopySpec) {
    copySpec.into("file-server/cfg") {
        from("${project.rootProject.projectDir}/file-server-app/src/main/genesis/cfg")
        filter(
            org.apache.tools.ant.filters.FixCrLfFilter::class,
            "eol" to FixCrLfFilter.CrLf.newInstance("lf")
        )
    }
}

fun includeData(copySpec: CopySpec) {
    copySpec.into("file-server/data") {
        from("${project.rootProject.projectDir}/file-server-app/src/main/genesis/data")
        filter(
            org.apache.tools.ant.filters.FixCrLfFilter::class,
            "eol" to FixCrLfFilter.CrLf.newInstance("lf")
        )
    }
}

fun buildRepoKey(): String {
    val buildTag = buildTagFor(project.version.toString())

    val repoKey = if (shouldDeployToClientRepo(buildTag)) {
        "libs-$buildTag-client"
    } else {
        "libs-$buildTag-local"
    }

    return repoKey
}

fun buildTagFor(version: String): String =
    when (version.substringAfterLast('-')) {
        "SNAPSHOT" -> "snapshot"
        in Regex("""M\d+[a-z]*$""") -> "milestone"
        else -> "release"
    }

fun shouldDeployToClientRepo(buildTag: String) = properties["deployToClientRepo"] == "true" && buildTag != "snapshot"

operator fun Regex.contains(s: String) = matches(s)
