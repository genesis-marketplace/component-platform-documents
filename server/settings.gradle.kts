
rootProject.name = "genesisproduct-file-server"

// this is duplicated at the task level
pluginManagement {
    val genesisVersion: String by settings
    plugins {
        id("global.genesis.build") version genesisVersion
        id("global.genesis.settings") version genesisVersion
    }

    repositories {
        // if(extra.has("USE_MVN_LOCAL")) {
        mavenLocal {
            // VERY IMPORTANT!!! EXCLUDE AGRONA AS IT IS A POM DEPENDENCY AND DOES NOT PLAY NICELY WITH MAVEN LOCAL!
            content {
                excludeGroup("org.agrona")
                // }
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://genesisglobal.jfrog.io/genesisglobal/dev-repo")
            credentials {
                username = extra.properties["genesisArtifactoryUser"].toString()
                password = extra.properties["genesisArtifactoryPassword"].toString()
            }
        }
    }
}

plugins {
    id("global.genesis.settings")
}

genesis {
    projectType = PBC

    plugins {
        genesisDeploy.enabled = false
        genesisDistribution.enabled = false
    }
}

include("file-server-api")
include("file-server-app")
include("file-server-test-config")
include("file-server-test-dictionary-cache")
include("file-server-test-dictionary-cache:genesis-generated-sysdef")
include("file-server-test-dictionary-cache:genesis-generated-fields")
include("file-server-test-dictionary-cache:genesis-generated-dao")
include("file-server-test-dictionary-cache:genesis-generated-hft")
include("file-server-test-dictionary-cache:genesis-generated-view")
