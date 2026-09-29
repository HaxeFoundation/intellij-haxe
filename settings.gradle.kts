rootProject.name = "Haxe-plugin"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":jps-plugin")
include(":common")

//dap protocol use in most debugger integrations
include(":debuggers:dap-protocol")

// debuggers
include(":debuggers:eval-debugger")
include(":debuggers:browser-debugger")
include(":debuggers:hashlink-debug-adapter")
include(":debuggers:intellij-hxcpp-debugger")
include(":debuggers:vshaxe-hxcpp-debugger-adapter")
include(":debuggers:hxcpp-debugger-protocol-legacy")

// Test matrix only used to verify that all debuggers work as expected across different haxe version and runtimes.
include(":debuggers:compat-matrix")

// profilers: neutral snapshot model + per-format translators (no IDE dependencies)
include(":profilers:core")

// haxe JSON-RPC display protocol (allow the IDE to communicate with Haxe Compiler)
include(":display-protocol")

// Custom tool for evaluating Lime/openFL projects including HXP
include(":tools:LimeProjectParser")



