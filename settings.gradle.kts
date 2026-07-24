rootProject.name = "Haxe-plugin"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":jps-plugin")
include(":common")

// the debugger family lives under debuggers/ (the :debuggers node is an
// empty grouping project with no build logic of its own)
include(":debuggers:dap-protocol")
include(":debuggers:hashlink-debug-adapter")
include(":debuggers:vshaxe-hxcpp-debugger-adapter")
include(":debuggers:intellij-hxcpp-debugger")
include(":debuggers:hxcpp-debugger-protocol-legacy")
include(":debuggers:eval-debugger")
include(":debuggers:browser-debugger")
include(":debuggers:compat-matrix")

