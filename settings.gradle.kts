rootProject.name = "Haxe-plugin"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":hxcpp-debugger-protocol")
include(":jps-plugin")
include(":common")

