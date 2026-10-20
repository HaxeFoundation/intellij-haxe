plugins {
    id("org.jetbrains.intellij.platform.module")
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

// Haxe JSON-RPC display protocol: DTOs mirroring std haxe.display.*, the
// JSON codec and client, and the null-terminated socket transport to a
// `haxe --wait <port>` server. Must stay free of IDE and project-model
// dependencies; the main plugin adds the IDE glue on top. Wire facts live in
// README.md.
dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
    }

    implementation(libs.jacksonDatabind)
    implementation(libs.jacksonDataformatXml)

    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

// The live-server tests drive a real `haxe --wait` process: opt-in like the
// debugger suites, since the machine may lack haxe (or carry an unexpected
// version). The DTO/transport unit tests always run.
val displayTests = providers.gradleProperty("displayTests").getOrElse("false").toBoolean()

tasks.named<Test>("test") {
    useJUnitPlatform()
    if (!displayTests) {
        logger.lifecycle("SKIPPING live display server tests (opt in with -PdisplayTests=true)")
        filter.excludeTestsMatching("com.intellij.plugins.haxe.display.client.LiveDisplayServerTest")
        filter.excludeTestsMatching("com.intellij.plugins.haxe.display.haxe3.LiveHaxe3DisplayServerTest")
    }
    // which compiler the live tests drive; defaults to the PATH haxe
    systemProperty("display.test.haxe", providers.gradleProperty("displayTestHaxe").getOrElse("haxe"))
    // the haxe 3 executable the haxe 3 live tests drive; without one they skip themselves
    systemProperty("display.test.haxe3", providers.gradleProperty("displayTestHaxe3").getOrElse(""))
}
