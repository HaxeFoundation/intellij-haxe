// Debugger for haxe's EVAL target (--interp scripts and macros): a Java
// translator that will present DAP to the IDE stack while speaking the
// compiler's built-in eval-debugger JSON-RPC protocol (framing and message
// shapes documented in EvalFraming/EvalProtocol). No shipped artifact and no
// extra runtime: the debug server lives inside haxe itself (-D eval-debugger).

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

    implementation(project(":debuggers:dap-protocol"))

    implementation(libs.jacksonDatabind)

    compileOnly(libs.lombok)
    testCompileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation(libs.junit)
}

// Debugger tests are OPT-IN (-PdebuggerTests=true): most plugin work does not
// touch the debuggers; the compat-matrix tool passes the flag itself (same
// pattern as the other debugger modules). The live tests also self-skip when
// haxe is not on PATH.
val debuggerTests = providers.gradleProperty("debuggerTests").getOrElse("false").toBoolean()

tasks.named<Test>("test") {
    onlyIf {
        if (!debuggerTests) {
            logger.lifecycle("SKIPPING eval debugger tests (opt in with -PdebuggerTests=true)")
        }
        debuggerTests
    }
    // live tests locate the interp fixtures through this
    systemProperty("eval.fixture.src.dir", File(projectDir, "test-fixtures").absolutePath)
}
