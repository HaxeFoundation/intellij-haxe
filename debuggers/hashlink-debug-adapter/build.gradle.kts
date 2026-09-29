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

    // generic DAP client/transport/messages live in :dap-protocol (shared with the HXCPP debugger)
    implementation(project(":debuggers:dap-protocol"))

    implementation(libs.jacksonDatabind)

    compileOnly(libs.lombok)
    testCompileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

val buildHashlinkAdapter = providers.gradleProperty("buildHashlinkAdapter").getOrElse("true").toBoolean()
val adapterHl = layout.buildDirectory.file("hl/hl-debug-adapter.hl")
val fixtureHl = layout.buildDirectory.file("hl/test-fixture.hl")
val threadsFixtureHl = layout.buildDirectory.file("hl/threads-fixture.hl")
val spinFixtureHl = layout.buildDirectory.file("hl/spin-fixture.hl")
val uncaughtFixtureHl = layout.buildDirectory.file("hl/uncaught-fixture.hl")
val vmFixtureHl = layout.buildDirectory.file("hl/vm-fixture.hl")
val stacktraceFixtureHl = layout.buildDirectory.file("hl/stacktrace-fixture.hl")
val typedThrowFixtureHl = layout.buildDirectory.file("hl/typedthrow-fixture.hl")
// haxelib used to read the .hl bytecode debug tables. The version pin lives
// in build.hxml's `-lib format:<version>` line (test.hxml mirrors it); the
// installer below provisions exactly that version.
val formatHaxelibVersion: String by lazy {
    // the versioned lib reference: `-lib format:3.7.0`
    Regex("-lib format:(\\S+)").find(File(projectDir, "build.hxml").readText())?.groupValues?.get(1)
        ?: error("build.hxml must pin the format haxelib as `-lib format:<version>`")
}

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

// `haxelib path lib:version` is version-exact and never touches the machine's
// selected ("current") version - the probe that lets an installed lib be left alone
val formatHaxelibInstalled: Boolean by lazy {
    try {
        ProcessBuilder("haxelib", "path", "format:$formatHaxelibVersion")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start().waitFor() == 0
    } catch (e: Exception) {
        false
    }
}

tasks.register<Exec>("installFormatHaxelib") {
    group = "hashlink"
    description = "Provisions the pinned 'format' haxelib when missing; an installed one is left untouched"
    // `haxelib install` switches the current-version selection as a side effect,
    // so it must only ever run when the pinned version is absent - the hxml pins
    // make the compiles themselves selection-independent
    onlyIf { buildHashlinkAdapter && haxeAvailable && !formatHaxelibInstalled }
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

tasks.register<Exec>("buildThreadsFixture") {
    group = "hashlink"
    description = "Compiles the multi-threaded debuggee fixture (build/hl/threads-fixture.hl)"
    onlyIf { buildHashlinkAdapter && haxeAvailable }
    dependsOn("installFormatHaxelib")
    workingDir = File(projectDir, "test-fixtures")
    commandLine = listOf("haxe", "threads.hxml")
    inputs.dir("test-fixtures/src")
    inputs.file("test-fixtures/threads.hxml")
    outputs.file(threadsFixtureHl)
}

tasks.register<Exec>("buildSpinFixture") {
    group = "hashlink"
    description = "Compiles the busy-loop pause debuggee fixture (build/hl/spin-fixture.hl)"
    onlyIf { buildHashlinkAdapter && haxeAvailable }
    dependsOn("installFormatHaxelib")
    workingDir = File(projectDir, "test-fixtures")
    commandLine = listOf("haxe", "spin.hxml")
    inputs.dir("test-fixtures/src")
    inputs.file("test-fixtures/spin.hxml")
    outputs.file(spinFixtureHl)
}

tasks.register<Exec>("buildUncaughtFixture") {
    group = "hashlink"
    description = "Compiles the caught-then-uncaught exception fixture (build/hl/uncaught-fixture.hl)"
    onlyIf { buildHashlinkAdapter && haxeAvailable }
    dependsOn("installFormatHaxelib")
    workingDir = File(projectDir, "test-fixtures")
    commandLine = listOf("haxe", "uncaught.hxml")
    inputs.dir("test-fixtures/src")
    inputs.file("test-fixtures/uncaught.hxml")
    outputs.file(uncaughtFixtureHl)
}

tasks.register<Exec>("buildVmFixture") {
    group = "hashlink"
    description = "Compiles the VM-raised (null access) exception fixture (build/hl/vm-fixture.hl)"
    onlyIf { buildHashlinkAdapter && haxeAvailable }
    dependsOn("installFormatHaxelib")
    workingDir = File(projectDir, "test-fixtures")
    commandLine = listOf("haxe", "vm.hxml")
    inputs.dir("test-fixtures/src")
    inputs.file("test-fixtures/vm.hxml")
    outputs.file(vmFixtureHl)
}

tasks.register<Exec>("buildStackTraceFixture") {
    group = "hashlink"
    description = "Compiles the haxe.Exception __nativeStack fixture (build/hl/stacktrace-fixture.hl)"
    onlyIf { buildHashlinkAdapter && haxeAvailable }
    dependsOn("installFormatHaxelib")
    workingDir = File(projectDir, "test-fixtures")
    commandLine = listOf("haxe", "stacktrace.hxml")
    inputs.dir("test-fixtures/src")
    inputs.file("test-fixtures/stacktrace.hxml")
    outputs.file(stacktraceFixtureHl)
}

tasks.register<Exec>("buildTypedThrowFixture") {
    group = "hashlink"
    description = "Compiles the type-filtered exception fixture (build/hl/typedthrow-fixture.hl)"
    onlyIf { buildHashlinkAdapter && haxeAvailable }
    dependsOn("installFormatHaxelib")
    workingDir = File(projectDir, "test-fixtures")
    commandLine = listOf("haxe", "typedthrow.hxml")
    inputs.dir("test-fixtures/src")
    inputs.file("test-fixtures/typedthrow.hxml")
    outputs.file(typedThrowFixtureHl)
}

// The DAP message typedefs live in the shared :debuggers:dap-protocol module
// (haxelib "intellij-dap-protocol", consumed via `haxelib dev`); this makes
// the registration idempotent for every build that compiles against them.
tasks.register<Exec>("registerDapProtocolHaxelib") {
    group = "hashlink"
    description = "Points haxelib at the in-repo intellij-dap-protocol sources (haxelib dev)"
    onlyIf { haxeAvailable }
    commandLine = listOf("haxelib", "dev", "intellij-dap-protocol",
                         rootProject.file("debuggers/dap-protocol").absolutePath)
}

tasks.register<Exec>("buildDebugAdapter") {
    group = "hashlink"
    description = "Compiles the DAP debug adapter to HashLink bytecode (build/hl/hl-debug-adapter.hl)"
    dependsOn("installFormatHaxelib", "registerDapProtocolHaxelib")
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
    inputs.dir(rootProject.file("debuggers/dap-protocol/src/main/haxe"))
    inputs.file("build.hxml")
    outputs.file(adapterHl)
}

// Debugger tests are OPT-IN (-PdebuggerTests=true): most plugin work does not
// touch the debuggers; the compat-matrix tool passes the flag itself. The
// adapter bytecode that ships in the plugin (buildDebugAdapter) is NOT
// affected by this flag.
val debuggerTests = providers.gradleProperty("debuggerTests").getOrElse("false").toBoolean()

tasks.register<Exec>("testHaxeAdapter") {
    group = "hashlink"
    description = "Runs the Haxe-side adapter tests with the Haxe interpreter (no HashLink runtime required)"
    dependsOn("installFormatHaxelib", "registerDapProtocolHaxelib", "buildTestFixture")
    onlyIf {
        (debuggerTests && buildHashlinkAdapter && haxeAvailable).also {
            if (!it) logger.warn("SKIPPING Haxe adapter tests (opt in with -PdebuggerTests=true; also needs haxe on PATH and buildHashlinkAdapter=true)")
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
    useJUnitPlatform()
    // @Timeout kills a hung test from a watcher thread (JUnit4 timeout semantics);
    // the default mode would merely wait for the test to come back on its own
    systemProperty("junit.jupiter.execution.timeout.thread.mode.default", "SEPARATE_THREAD")
    onlyIf {
        if (!debuggerTests) {
            logger.lifecycle("SKIPPING debugger tests (opt in with -PdebuggerTests=true)")
        }
        debuggerTests
    }
    dependsOn("buildDebugAdapter", "buildTestFixture", "buildThreadsFixture", "buildSpinFixture", "buildUncaughtFixture", "buildVmFixture", "buildStackTraceFixture", "buildTypedThrowFixture")
    // the binaries under test ARE test inputs: without this, a Haxe-only
    // change reuses a cached (FROM-CACHE/UP-TO-DATE) test result and the
    // rebuilt adapter is never actually exercised
    inputs.files(adapterHl, fixtureHl, threadsFixtureHl, spinFixtureHl,
                 uncaughtFixtureHl, vmFixtureHl, stacktraceFixtureHl, typedThrowFixtureHl)
    // integration tests locate the built adapter, the debuggee fixtures and
    // (optionally) the HashLink executable through these
    systemProperty("dap.adapter.hl", adapterHl.get().asFile.absolutePath)
    systemProperty("dap.fixture.hl", fixtureHl.get().asFile.absolutePath)
    systemProperty("dap.fixture.threads.hl", threadsFixtureHl.get().asFile.absolutePath)
    systemProperty("dap.fixture.spin.hl", spinFixtureHl.get().asFile.absolutePath)
    systemProperty("dap.fixture.uncaught.hl", uncaughtFixtureHl.get().asFile.absolutePath)
    systemProperty("dap.fixture.vm.hl", vmFixtureHl.get().asFile.absolutePath)
    systemProperty("dap.fixture.stacktrace.hl", stacktraceFixtureHl.get().asFile.absolutePath)
    systemProperty("dap.fixture.typedthrow.hl", typedThrowFixtureHl.get().asFile.absolutePath)
    systemProperty("dap.fixture.src.dir", File(projectDir, "test-fixtures/src").absolutePath)
    providers.gradleProperty("hashlinkBin").orNull?.let {
        systemProperty("hashlink.executable", it)
    }
    // -PdapTestForks=N runs N test CLASSES in parallel fork JVMs. Safe in
    // principle (each test spawns its own adapter+debuggee on dynamic ports,
    // and fixture compilation is a separate task that never overlaps a test
    // run). Default 4; pass -PdapTestForks=1 for a fully sequential run on a
    // small machine (e.g. a low-core CI runner).
    maxParallelForks = (providers.gradleProperty("dapTestForks").orNull?.toIntOrNull() ?: 4)
        .coerceAtLeast(1)
}
