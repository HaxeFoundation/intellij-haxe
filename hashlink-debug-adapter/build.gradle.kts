plugins {
    id("org.jetbrains.intellij.platform.module")
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
    }

    implementation("tools.jackson.core:jackson-databind:3.1.0")

    compileOnly("org.projectlombok:lombok:1.18.44")
    testCompileOnly("org.projectlombok:lombok:1.18.44")
    annotationProcessor("org.projectlombok:lombok:1.18.44")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.44")

    testImplementation(libs.junit)
}

val buildHashlinkAdapter = providers.gradleProperty("buildHashlinkAdapter").getOrElse("true").toBoolean()
val adapterHl = layout.buildDirectory.file("hl/hl-debug-adapter.hl")

// probed lazily at execution time so a haxe-less machine can still configure and build the rest of the plugin
val haxeAvailable: Boolean by lazy {
    try {
        ProcessBuilder("haxe", "--version")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start().waitFor() == 0
    } catch (e: Exception) {
        false
    }
}

tasks.register<Exec>("buildDebugAdapter") {
    group = "hashlink"
    description = "Compiles the DAP debug adapter to HashLink bytecode (build/hl/hl-debug-adapter.hl)"
    onlyIf {
        if (!buildHashlinkAdapter) {
            logger.warn("SKIPPING HashLink debug adapter build (buildHashlinkAdapter=false); the plugin distribution will not contain hl-debug-adapter.hl")
            return@onlyIf false
        }
        if (!haxeAvailable) {
            logger.warn("SKIPPING HashLink debug adapter build (haxe compiler not found on PATH); the plugin distribution will not contain hl-debug-adapter.hl")
            return@onlyIf false
        }
        true
    }
    workingDir = projectDir
    commandLine = listOf("haxe", "build.hxml")
    inputs.dir("src/main/haxe")
    inputs.file("build.hxml")
    outputs.file(adapterHl)
}

tasks.register<Exec>("testHaxeAdapter") {
    group = "hashlink"
    description = "Runs the Haxe-side adapter tests with the Haxe interpreter (no HashLink runtime required)"
    onlyIf {
        (buildHashlinkAdapter && haxeAvailable).also {
            if (!it) logger.warn("SKIPPING Haxe adapter tests (haxe compiler not found on PATH or buildHashlinkAdapter=false)")
        }
    }
    workingDir = projectDir
    commandLine = listOf("haxe", "test.hxml")
    inputs.dir("src/main/haxe")
    inputs.dir("src/test/haxe")
    inputs.file("test.hxml")
    outputs.upToDateWhen { false }
}

tasks.named("check") {
    dependsOn("testHaxeAdapter")
}

tasks.named<Test>("test") {
    dependsOn("buildDebugAdapter")
    // integration tests locate the built adapter and (optionally) the HashLink executable through these
    systemProperty("dap.adapter.hl", adapterHl.get().asFile.absolutePath)
    providers.gradleProperty("hashlinkBin").orNull?.let {
        systemProperty("hashlink.executable", it)
    }
}
