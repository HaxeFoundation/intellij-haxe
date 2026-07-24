plugins {
    id("org.jetbrains.intellij.platform.module")
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

// Generic Debug Adapter Protocol (DAP) client, transport and message types.
// Shared by the HashLink and HXCPP debuggers; must stay free of IDE and
// debugger-specific dependencies.
dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
    }

    implementation(libs.jacksonDatabind)

    compileOnly(libs.lombok)
    testCompileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation(libs.junit)
}

// Haxelib upload zip: the library sits at the ZIP ROOT with its sources under
// src/ (classPath "src"), so the repo's src/main/haxe tree is remapped and
// haxelib.json's classPath rewritten to match. Lands in the project root
// (git-ignored).
val haxelibVersion = Regex("\"version\"\\s*:\\s*\"([^\"]+)\"")
    .find(file("haxelib.json").readText())!!.groupValues[1]

tasks.register<Zip>("buildHaxelibZip") {
    group = "haxelib"
    description = "Builds the intellij-dap-protocol haxelib upload zip into the project root"
    archiveFileName = "intellij-dap-protocol-$haxelibVersion.zip"
    destinationDirectory = rootProject.layout.projectDirectory
    from("haxelib.json") {
        filter { line: String ->
            line.replace(Regex("\"classPath\"\\s*:\\s*\"[^\"]*\""), "\"classPath\": \"src\"")
        }
    }
    from("src/main/haxe") {
        into("src")
    }
}

// Debugger tests are OPT-IN (-PdebuggerTests=true): most plugin work does not
// touch the debuggers, and the compat-matrix tool passes the flag itself.
// Compilation still runs in every build.
tasks.named<Test>("test") {
    onlyIf {
        val enabled = providers.gradleProperty("debuggerTests").getOrElse("false").toBoolean()
        if (!enabled) {
            logger.lifecycle("SKIPPING debugger tests (opt in with -PdebuggerTests=true)")
        }
        enabled
    }
}
