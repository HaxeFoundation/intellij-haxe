// Our own in-debuggee HXCPP debug server (haxelib: intellij-hxcpp-debug-server):
// interpreter-run unit tests for the Haxe server, gradle-built native fixtures,
// and the Java-client integration suite driving the REAL DapClient stack
// against those fixtures. See docs/README.md for the server gotchas.

import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations

plugins {
    id("org.jetbrains.intellij.platform.module")
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

// No production Java: this module's product is the Haxe haxelib. The Java
// sources are the integration tests only.
dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
    }

    testImplementation(project(":debuggers:dap-protocol"))
    testImplementation(libs.junit)
}

// ---------------------------------------------------------------------------
// Toolchain probing + CI gating: fixtures need haxe + hxcpp + a C++ toolchain,
// so everything skips gracefully without them (same pattern as the vshaxe
// module). Debugger tests are OPT-IN (-PdebuggerTests=true; the compat-matrix
// tool passes it itself) - without it this module's tests and fixture builds
// are skipped entirely.
// ---------------------------------------------------------------------------
val debuggerTests = providers.gradleProperty("debuggerTests").getOrElse("false").toBoolean()
val exeSuffix = if (System.getProperty("os.name").startsWith("Windows")) ".exe" else ""

// probed lazily at execution time so a haxe-less machine can still configure
// and build the rest of the plugin
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

// ---------------------------------------------------------------------------
// hxcpp version pinning ( makes sure we can build for  haxe 4 & 5 ).
// v4.3.114 is  backward compatible with haxe 4.1-4.3 (api level 430)
// and compatible with haxe 5 preview.
// ---------------------------------------------------------------------------
val hxcppGitUrl = "https://github.com/HaxeFoundation/hxcpp"
val hxcppPinnedTag = "v4.3.114"

// Gradle 9 forbids Project.exec {} inside a task action; run external processes
// through the injected ExecOperations service instead (captured at config time).
val execOperations = serviceOf<ExecOperations>()

// haxelib may print WARNING lines around the path (haxe 5's haxelib emits a
// "Repository requires reformatting" notice), so take the line that actually
// IS an existing directory rather than the raw output.
fun hxcppLibPath(): File? = try {
    val process = ProcessBuilder("haxelib", "libpath", "hxcpp").redirectErrorStream(true).start()
    val lines = process.inputStream.bufferedReader().readLines()
    process.waitFor()
    if (process.exitValue() != 0) null
    else lines.map { it.trim() }.firstOrNull { it.isNotEmpty() && File(it).isDirectory }?.let { File(it) }
} catch (e: Exception) {
    null
}


fun hxcppIsPinned(): Boolean {
    val path = hxcppLibPath() ?: return false
    return try {
        val process = ProcessBuilder("git", "-C", path.absolutePath, "describe", "--tags")
            .redirectErrorStream(true).start()
        val described = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        process.exitValue() == 0 && described == hxcppPinnedTag
    } catch (e: Exception) {
        false
    }
}

// the DAP message typedefs come from the shared :debuggers:dap-protocol
// module (haxelib "intellij-dap-protocol"); registration is idempotent
tasks.register<Exec>("registerDapProtocolHaxelib") {
    group = "hxcpp"
    description = "Points haxelib at the in-repo intellij-dap-protocol sources (haxelib dev)"
    onlyIf { haxeAvailable }
    commandLine = listOf("haxelib", "dev", "intellij-dap-protocol",
                         rootProject.file("debuggers/dap-protocol").absolutePath)
}

// the server-under-test itself: fixtures compile it in via
// `-lib intellij-hxcpp-debug-server`, so the dev registration IS the setup
tasks.register<Exec>("registerServerHaxelib") {
    group = "hxcpp"
    description = "Registers hxcpp-debug-server/ as the haxelib dev path for 'intellij-hxcpp-debug-server'"
    onlyIf { haxeAvailable }
    // this directory must NOT be named `haxelib`: on linux, haxelib's recursive
    // dependency resolution re-invokes "haxelib" cwd-relative from the module
    // root, and a `haxelib` subdirectory there shadows the binary (execve of a
    // directory -> EACCES, so every fixture build fails). Windows is unaffected.
    commandLine = listOf("haxelib", "dev", "intellij-hxcpp-debug-server", File(projectDir, "hxcpp-debug-server").absolutePath)
}

// hscript powers watch/hover/condition evaluation (M5); it is a released
// haxelib, so pull it in when absent rather than assuming a primed machine
tasks.register<Exec>("installHscript") {
    group = "hxcpp"
    description = "Installs the hscript haxelib if it is not already present"
    onlyIf { haxeAvailable }
    commandLine = listOf("haxelib", "install", "hscript", "--always", "--quiet")
    isIgnoreExitValue = true
}

// Pin hxcpp to the version that can build haxe 5 (see the block above the
// hxcpp version constants). Guarded to run only when the current hxcpp is not
// already the pinned tag, so it is a one-time setup on a fresh machine and a
// no-op afterwards. Scoped to debugger-test builds: a regular plugin build
// (no -PdebuggerTests=true) never touches the global toolchain.
tasks.register("installHxcpp") {
    group = "hxcpp"
    description = "Pins hxcpp to $hxcppPinnedTag (needs api level 500 for haxe 5; backward compatible with 4.1-4.3)"
    onlyIf { debuggerTests && haxeAvailable && !hxcppIsPinned() }
    doLast {
        logger.lifecycle("Pinning hxcpp to $hxcppPinnedTag (api level 500 for haxe 5 support)")
        // --always: haxelib asks "Overwrite branch [y/n/a]?" when a git install
        // already exists, and a non-interactive build would default to "n"
        execOperations.exec { commandLine("haxelib", "git", "hxcpp", hxcppGitUrl, hxcppPinnedTag, "--always") }
        // a git checkout ships hxcpp as source: rebuild its command-line tool
        val toolDir = File(hxcppLibPath() ?: error("hxcpp libpath unresolved after install"), "tools/hxcpp")
        execOperations.exec {
            workingDir = toolDir
            commandLine("haxe", "compile.hxml")
        }
    }
}

tasks.register<Exec>("testHaxeServer") {
    group = "verification"
    description = "Runs the intellij-hxcpp-debug-server unit tests under the Haxe interpreter"
    dependsOn("registerDapProtocolHaxelib", "installHscript")
    workingDir = projectDir
    commandLine = listOf("haxe", "test.hxml")
    onlyIf {
        if (!haxeAvailable) logger.lifecycle("SKIPPING intellij-hxcpp-debug-server unit tests (haxe compiler not found on PATH)")
        haxeAvailable
    }
}

// ---------------------------------------------------------------------------
// Native fixtures: name -> (hxml, main class); each compiles to
// build/hxcpp/<name>/<Main>-debug(.exe) with the WORK-IN-PROGRESS server
// compiled in (haxelib dev), so the integration tests always test this tree.
// ---------------------------------------------------------------------------
val hxcppFixtures = mapOf(
    "fixture" to Pair("fixture.hxml", "Main"),
    "fixture-ex" to Pair("fixture-ex.hxml", "MainEx"),
)

fun fixtureExe(name: String) =
    layout.buildDirectory.file("hxcpp/$name/${hxcppFixtures.getValue(name).second}-debug$exeSuffix")

fun fixtureTaskName(name: String) =
    "buildHxcpp${name.split("-").joinToString("") { part -> part.replaceFirstChar { it.uppercase() } }}Fixture"

hxcppFixtures.forEach { (name, spec) ->
    tasks.register<Exec>(fixtureTaskName(name)) {
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
        dependsOn("registerDapProtocolHaxelib", "registerServerHaxelib", "installHscript", "installHxcpp")
        // run from the MODULE root, not test-fixtures: haxe builds generated-file
        // paths from the cwd without normalizing, and a "test-fixtures/../" segment
        // pushes the longest generated names past Windows' 260-char MAX_PATH
        workingDir = projectDir
        commandLine = listOf("haxe", "test-fixtures/${spec.first}")
        inputs.dir("test-fixtures/src")
        inputs.file("test-fixtures/${spec.first}")
        // the fixture embeds the server sources: a haxelib change must rebuild
        inputs.dir("hxcpp-debug-server")
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
        dependsOn(fixtureTaskName(name))
        // integration tests locate each built fixture through these
        systemProperty("hxcpp.server.fixture.$name.exe", fixtureExe(name).get().asFile.absolutePath)
        // the fixture (with the server compiled in) IS a test input: without
        // this, a haxelib-only change reuses a cached test result and the new
        // server is never actually exercised
        inputs.file(fixtureExe(name))
    }
    systemProperty("hxcpp.server.fixture.src.dir", File(projectDir, "test-fixtures/src").absolutePath)
}

tasks.named("check") {
    dependsOn("testHaxeServer")
}

// Haxelib upload zip: the library sits at the ZIP ROOT, and the source tree
// already IS the release layout (haxelib.json with classPath "src", sources
// under src/, extraParams.hxml beside them) — the zip is a straight copy.
// Lands in the project root (git-ignored).
val serverHaxelibVersion = Regex("\"version\"\\s*:\\s*\"([^\"]+)\"")
    .find(file("hxcpp-debug-server/haxelib.json").readText())!!.groupValues[1]

tasks.register<Zip>("buildHaxelibZip") {
    group = "haxelib"
    description = "Builds the intellij-hxcpp-debug-server haxelib upload zip into the project root"
    archiveFileName = "intellij-hxcpp-debug-server-$serverHaxelibVersion.zip"
    destinationDirectory = rootProject.layout.projectDirectory
    from("hxcpp-debug-server")
}
