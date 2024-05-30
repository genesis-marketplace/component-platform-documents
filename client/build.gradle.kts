import com.github.gradle.node.npm.task.NpmTask

plugins {
    base
    id("com.github.node-gradle.node") version "7.0.2"
}

tasks {

    // Setup custom clean task to be run when "clean" task runs.
    val npmClean = register("npmClean", NpmTask::class) {
        args.set(listOf("run", "clean"))
        delete(".bootstrapDone")
    }

    val npmBootstrap = register("npmBootstrap", NpmTask::class) {
        val workingDir = layout.projectDirectory.asFile
        args.set(listOf("run", "bootstrap"))
        outputs.upToDateWhen { File(workingDir, ".bootstrapDone").exists() }
        doLast { File(workingDir, ".bootstrapDone").createNewFile() }
    }

    val npmAssemble = register("npmAssemble", NpmTask::class) {
        args.set(listOf("run", "build"))
        inputs.dir("src")
        outputs.dir("dist")
        dependsOn(npmBootstrap)
    }

    val test = register("test", NpmTask::class) {
        dependsOn(npmBootstrap) // in case assemble uses its cache
        dependsOn(assemble)
        args.set(listOf("run", "test"))
        inputs.files(fileTree("src"))
        inputs.file("package.json")

        val testsExecutedMarkerName: String = "${projectDir}/.tests.executed"
        // Below some potentially useful config snippets if we want to be efficient with test executions.
        
        // allows easy triggering re-tests
        doLast {
            File(testsExecutedMarkerName).appendText("delete this file to force re-execution JavaScript tests")
        }
        outputs.file(testsExecutedMarkerName)
    }

    assemble {
        dependsOn(npmAssemble)
    }

    clean {
        // Depend on the custom npmClean task, the default gradle one deletes the "build" folder by default
        // and the project build won't work without it.
        dependsOn(npmClean)
    }

    build {
        dependsOn(test)
    }

    register("sonar") {
        doLast {
            logger.warn("sonar task must be executed from the runner for typescript projects.")
        }
    }
}