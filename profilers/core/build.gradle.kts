plugins {
    id("org.jetbrains.intellij.platform.module")
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

// Neutral profiler snapshot model plus one translator per capture format.
// Consumed by the IU profiler bridge in the main plugin; must stay free of
// IDE and target-specific dependencies so the translators remain plain
// fixture-testable functions.
dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
    }

    // tracy streams LZ4 with a cross-frame dictionary window; commons-compress's
    // block reader + prefill() is the one Java implementation that decodes it.
    // Session-file chunks deliberately use the JDK's own Deflater instead:
    // this library's compressor hits a pathological slow path on real zone
    // data (seconds per chunk), and deflate also compresses better
    implementation(libs.commonsCompress)

    // the V8 .cpuprofile translator reads JSON
    implementation(libs.jacksonDatabind)

    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
