// Debugger for haxe's JS target running IN A BROWSER (Firefox now, Chromium
// next): drives the pinned vscode DAP debug adapters (external, node-run,
// SHA-256-verified downloads — see the supply-chain rules in the project
// docs) through the shared :debuggers:dap-protocol client. This module holds
// the adapter acquisition/launch plumbing, the content http server, and the
// live wire probes; the IDE-side backend/run configuration live in the main
// plugin source like the other debuggers'.

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

    testImplementation(libs.junit)
}

// Debugger validation belongs to the dedicated windows CI job; the regular
// build/release jobs pass -PdebuggerTests=false (same pattern as the other
// debugger modules). The live probes also self-skip when their runtimes
// (portable node, the pinned adapter, firefox, haxe) are not provisioned.
val debuggerTests = providers.gradleProperty("debuggerTests").getOrElse("false").toBoolean()

tasks.named<Test>("test") {
    onlyIf {
        if (!debuggerTests) {
            logger.lifecycle("SKIPPING browser debugger tests (opt in with -PdebuggerTests=true)")
        }
        debuggerTests
    }
    // the user-provisioned node runtime + pinned adapters (git-ignored /node/)
    systemProperty("web.debug.node.root", File(rootDir, "node").absolutePath)
    // the compat-matrix web lanes point each cell at a PROVISIONED node
    providers.gradleProperty("webDebugNodeExe").orNull?.let {
        systemProperty("web.debug.node.exe", it)
    }
}
