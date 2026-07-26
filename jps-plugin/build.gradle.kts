plugins {
  id("org.jetbrains.intellij.platform.module")
}

// The JPS build process, runs on either the project or module java SDK if provided.
// but in most cases a user would have selected Haxe there, and the fallback is then
// the latest configured java version in the SDK list. if non are found it will use
// bundled JBR (in IJ 2026 case that would be Java 25)
//
// we could insist that every user installs and configure a Java 25 SDK, but it's
// probably better to just target a lower java version so that most existing java SDKs
// will work with the plugin (we should probably add to the wiki  what JPS byte code version errors means)
tasks.withType<JavaCompile>().configureEach {
  options.release.set(11)
}

repositories {
  mavenCentral()

  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  implementation(project(":common"))
  intellijPlatform {
    intellijIdea(providers.gradleProperty("platformVersion"))
    bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })
    bundledModules(providers.gradleProperty("platformBundledModules").map { it.split(',') })
  }
}