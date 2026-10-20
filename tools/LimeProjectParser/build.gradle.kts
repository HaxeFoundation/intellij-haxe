plugins {
    base
}

fun properties(key: String) = providers.gradleProperty(key)

val hxjavaVersion = properties("hxjavaVersion").get()
val haxeLibChangeVersion = properties("haxeLibChangeVersion").get()

// probed lazily at execution time so a haxe-less machine can still configure
// and build the rest of the plugin
val haxeAvailable: Boolean by lazy {
    try {
        ProcessBuilder("haxe", "--version")
            .redirectErrorStream(true)
            .start()
            .waitFor() == 0
    } catch (e: Exception) {
        false
    }
}

val parserJar = layout.buildDirectory.file("libs/LimeProjectParser.jar")

// `haxelib path lib:version` is version-exact and never touches the machine's
// selected ("current") version - the probe that lets an installed lib be left alone
val hxjavaInstalled: Boolean by lazy {
    try {
        ProcessBuilder("haxelib", "path", "hxjava:$hxjavaVersion")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start().waitFor() == 0
    } catch (e: Exception) {
        false
    }
}

tasks.register<Exec>("installHxJava") {
    group = "build"
    description = "Provisions the pinned hxjava haxelib when missing; an installed one is left untouched"
    // `haxelib install` switches the current-version selection as a side effect,
    // so it must only ever run when the pinned version is absent (buildParser
    // pins the compile itself with `-lib hxjava:<version>` on the command line)
    onlyIf { haxeAvailable && !hxjavaInstalled }
    commandLine = listOf("haxelib", "install", "hxjava", hxjavaVersion, "--quiet", haxeLibChangeVersion)
}

tasks.register<Exec>("buildParser") {
    group = "build"
    description = "Compiles the lime project.xml evaluator to a JVM jar"
    dependsOn("installHxJava")
    onlyIf { haxeAvailable }
    workingDir = projectDir
    commandLine = listOf("haxe", "build.hxml", "-lib", "hxjava:$hxjavaVersion")
    inputs.dir("src/main/haxe")
    // build.hxml embeds src/main/resources (HxpRunner.hx) into the jar via
    // -resource; without this input an edit there leaves the task UP-TO-DATE
    // and ships a stale jar
    inputs.dir("src/main/resources")
    inputs.file("build.hxml")
    outputs.file(parserJar)
}

tasks.register<Exec>("testParser") {
    group = "verification"
    description = "Runs the evaluator's haxe-side tests via --interp"
    onlyIf { haxeAvailable }
    workingDir = projectDir
    commandLine = listOf("haxe", "test.hxml")
    inputs.dir("src/main/haxe")
    // test.hxml embeds src/main/resources the same way buildParser does
    inputs.dir("src/main/resources")
    inputs.dir("src/test/haxe")
    inputs.file("test.hxml")
    // --interp leaves no artifact; declare a marker so up-to-date checks work
    outputs.file(layout.buildDirectory.file("test-marker.txt"))
    doLast {
        outputs.files.singleFile.writeText("passed")
    }
}

tasks.register<Exec>("integrationTestParser") {
    group = "verification"
    description = "Evaluates project.xml AND project.hxp fixtures end to end (self-skips without lime/hxp)"
    onlyIf { haxeAvailable }
    workingDir = projectDir
    commandLine = listOf("haxe", "integration-test.hxml")
    inputs.dir("src/main/haxe")
    inputs.dir("src/main/resources")
    inputs.dir("src/test/haxe")
    inputs.file("integration-test.hxml")
    outputs.file(layout.buildDirectory.file("integration-test-marker.txt"))
    doLast {
        outputs.files.singleFile.writeText("passed")
    }
}

tasks.named("check") {
    dependsOn("testParser", "integrationTestParser")
}

tasks.named("assemble") {
    dependsOn("buildParser")
}
