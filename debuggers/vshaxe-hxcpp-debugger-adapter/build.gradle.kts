plugins {
    id("org.jetbrains.intellij.platform.module")
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

// The new HXCPP debugger: a jsonrpc wire-protocol client for the
// hxcpp-debug-server running inside the debuggee, plus an in-process DAP
// adapter translating between the IDE-facing DAP surface and that protocol.
// See docs/README.md for the wire-protocol gotchas.
dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
    }

    implementation(project(":debuggers:dap-protocol"))
    implementation(libs.jacksonDatabind)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.junit)
}

// ---------------------------------------------------------------------------
// Integration-test fixture: a windows exe with hxcpp-debug-server compiled in.
// Needs haxe + hxcpp + a C++ toolchain; everything skips gracefully without
// them (same pattern as hashlink-debug-adapter). The port is fixed and
// non-default so tests never collide with a real debug session on 6972.
// ---------------------------------------------------------------------------
val hxcppFixturePort = 6973
val hxcppDebugServerVersion = "1.2.4" // pinned for reproducible fixture builds

// Debugger validation belongs to the dedicated windows CI job: fixtures need
// haxe + the hxcpp toolchain + a C++ compiler, and the tests target windows.
// Debugger tests are OPT-IN (-PdebuggerTests=true); without the flag this disables
// this module's tests and fixture builds entirely (compilation still runs).
val debuggerTests = providers.gradleProperty("debuggerTests").getOrElse("false").toBoolean()
val exeSuffix = if (System.getProperty("os.name").startsWith("Windows")) ".exe" else ""

// name -> (hxml, main class); each compiles to build/hxcpp/<name>/<Main>-debug(.exe)
val hxcppFixtures = mapOf(
    "fixture" to Pair("fixture.hxml", "Main"),
    "spin" to Pair("spin.hxml", "Spin"),
    "uncaught" to Pair("uncaught.hxml", "Uncaught"),
)

fun fixtureExe(name: String) =
    layout.buildDirectory.file("hxcpp/$name/${hxcppFixtures.getValue(name).second}-debug$exeSuffix")

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

tasks.register<Exec>("installHxcppDebugServerHaxelib") {
    group = "hxcpp"
    description = "Installs the pinned 'hxcpp-debug-server' haxelib compiled into the test fixture"
    onlyIf { debuggerTests && haxeAvailable }
    commandLine = listOf("haxelib", "install", "hxcpp-debug-server", hxcppDebugServerVersion, "--quiet", "--always")
}

hxcppFixtures.forEach { (name, spec) ->
    tasks.register<Exec>("buildHxcpp${name.replaceFirstChar { it.uppercase() }}Fixture") {
        group = "hxcpp"
        description = "Compiles the '$name' debuggee fixture to a native exe (build/hxcpp/$name)"
        onlyIf {
            if (!debuggerTests) {
                logger.lifecycle("SKIPPING hxcpp '$name' fixture build (opt in with -PdebuggerTests=true)")
            } else if (!haxeAvailable) {
                logger.warn("SKIPPING hxcpp '$name' fixture build (haxe compiler not found on PATH); integration tests will be skipped")
            }
            debuggerTests && haxeAvailable
        }
        dependsOn("installHxcppDebugServerHaxelib")
        // run from the MODULE root, not test-fixtures: haxe builds generated-file
        // paths from the cwd without normalizing, and a "test-fixtures/../" segment
        // pushes the longest generated names past Windows' 260-char MAX_PATH
        workingDir = projectDir
        commandLine = listOf("haxe", "test-fixtures/${spec.first}")
        inputs.dir("test-fixtures/src")
        inputs.file("test-fixtures/${spec.first}")
        outputs.file(fixtureExe(name))
    }
}

tasks.named<Test>("test") {
    onlyIf {
        if (!debuggerTests) {
            logger.lifecycle("SKIPPING debugger tests (opt in with -PdebuggerTests=true)")
        }
        debuggerTests
    }
    hxcppFixtures.keys.forEach { name ->
        dependsOn("buildHxcpp${name.replaceFirstChar { it.uppercase() }}Fixture")
        // integration tests locate each built fixture through these
        systemProperty("hxcpp.fixture.$name.exe", fixtureExe(name).get().asFile.absolutePath)
    }
    // kept for the existing tests' property name
    systemProperty("hxcpp.fixture.exe", fixtureExe("fixture").get().asFile.absolutePath)
    systemProperty("hxcpp.fixture.port", hxcppFixturePort)
    systemProperty("hxcpp.fixture.src.dir", File(projectDir, "test-fixtures/src").absolutePath)
}
