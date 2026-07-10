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
val fixtureHl = layout.buildDirectory.file("hl/test-fixture.hl")
// haxelib used to read the .hl bytecode debug tables; pinned for reproducible builds
val formatHaxelibVersion = "3.7.0"

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

tasks.register<Exec>("installFormatHaxelib") {
    group = "hashlink"
    description = "Installs the pinned 'format' haxelib used to read .hl debug info"
    onlyIf { buildHashlinkAdapter && haxeAvailable }
    commandLine = listOf("haxelib", "install", "format", formatHaxelibVersion, "--quiet", "--always")
}

tasks.register<Exec>("buildTestFixture") {
    group = "hashlink"
    description = "Compiles the debuggee test fixture with debug info (build/hl/test-fixture.hl)"
    onlyIf { buildHashlinkAdapter && haxeAvailable }
    dependsOn("installFormatHaxelib")
    workingDir = File(projectDir, "test-fixtures")
    commandLine = listOf("haxe", "fixture.hxml")
    inputs.dir("test-fixtures/src")
    inputs.file("test-fixtures/fixture.hxml")
    outputs.file(fixtureHl)
}

tasks.register<Exec>("buildDebugAdapter") {
    group = "hashlink"
    description = "Compiles the DAP debug adapter to HashLink bytecode (build/hl/hl-debug-adapter.hl)"
    dependsOn("installFormatHaxelib")
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
    dependsOn("installFormatHaxelib", "buildTestFixture")
    onlyIf {
        (buildHashlinkAdapter && haxeAvailable).also {
            if (!it) logger.warn("SKIPPING Haxe adapter tests (haxe compiler not found on PATH or buildHashlinkAdapter=false)")
        }
    }
    workingDir = projectDir
    commandLine = listOf("haxe", "test.hxml")
    environment("DAP_FIXTURE_HL", fixtureHl.get().asFile.absolutePath)
    inputs.dir("src/main/haxe")
    inputs.dir("src/test/haxe")
    inputs.file("test.hxml")
    outputs.upToDateWhen { false }
}

tasks.named("check") {
    dependsOn("testHaxeAdapter")
}

tasks.named<Test>("test") {
    dependsOn("buildDebugAdapter", "buildTestFixture")
    // integration tests locate the built adapter, the debuggee fixture and
    // (optionally) the HashLink executable through these
    systemProperty("dap.adapter.hl", adapterHl.get().asFile.absolutePath)
    systemProperty("dap.fixture.hl", fixtureHl.get().asFile.absolutePath)
    systemProperty("dap.fixture.src", File(projectDir, "test-fixtures/src/Main.hx").absolutePath)
    providers.gradleProperty("hashlinkBin").orNull?.let {
        systemProperty("hashlink.executable", it)
    }
}
