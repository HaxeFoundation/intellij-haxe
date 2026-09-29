import java.time.Duration
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformExtension
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.intellij.platform.gradle.tasks.GenerateLexerTask
import org.jetbrains.intellij.platform.gradle.tasks.GenerateParserTask
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
    id("java") // Java support

    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.intelliJPlatformGrammarKit) // generate parser and lexer
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
    alias(libs.plugins.testLogger) // console output for tests
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(25)
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

sourceSets {
    main {
        java {
            srcDir("src/main/gen")
        }
    }
}


// Configure project's dependencies
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        mavenCentral()
    }
}


dependencies {

    var platformVersion = providers.gradleProperty("platformVersion").get();
    var platformType = providers.gradleProperty("platformType").get();


    val ideaBaseDir = "${project.rootDir}/idea"
    val ideaTargetDir = "${ideaBaseDir}/idea${platformType}-${platformVersion}"


    implementation(libs.commonmark)
    implementation(libs.commonmarkAutolink)
    implementation(libs.commonmarkGfmTables)

    implementation(libs.jacksonDatabind)
    implementation(libs.commonsText)

    val flexShared = "${ideaTargetDir}/config/plugins/flex/lib/flex-shared.jar"
    val flexSupport = "${ideaTargetDir}/config/plugins/flex/lib/FlexSupport.jar"

    compileOnly(files(flexShared))
    compileOnly(files(flexSupport))

    compileOnly(files("${ideaTargetDir}/lib/openapi.jar"))
    compileOnly(files("${ideaTargetDir}/lib/util.jar"))

    testCompileOnly(files(flexShared))
    testCompileOnly(files(flexSupport))

    testCompileOnly(files("${ideaTargetDir}/lib/openapi.jar"))
    testCompileOnly(files("${ideaTargetDir}/lib/util.jar"))

    compileOnly(libs.lombok)
    testCompileOnly(libs.lombok)
    annotationProcessor (libs.lombok)
    testAnnotationProcessor (libs.lombok)

    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitPlatformLauncher)
    // junit4 is a need of the platform test framework itself, not of our tests: the
    // JUnit3-style fixture classes (UsefulTestCase, ParsingTestCase, ...) our test
    // engines wrap extend junit.framework.TestCase, which lives in the junit4 jar,
    // so javac needs it on the classpath to resolve the engine supertypes
    testImplementation(libs.junit)

    intellijPlatform {
        pluginVerifier()
        intellijIdea(providers.gradleProperty("platformVersion"))

        jflex ("1.9.2")
        grammarKit("2023.3.1")

        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })
        bundledModules(providers.gradleProperty("platformBundledModules").map { it.split(',') })


        // Platform stays even with JUnit5 in place: the fixture classes the tests build
        // on (UsefulTestCase, ParsingTestCase, CodeInsightTestFixture) live there
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.JUnit5)
        testFramework(TestFrameworkType.Bundled)
        testFramework(TestFrameworkType.Plugin.Java)

        pluginModule(implementation(project(":jps-plugin")))
        pluginModule(implementation(project(":common")))
        pluginModule(implementation(project(":debuggers:hashlink-debug-adapter")))

        pluginComposedModule(implementation(project(":debuggers:hxcpp-debugger-protocol-legacy")))
        pluginComposedModule(implementation(project(":common")))
        // composed (merged into the main jar) so the DAP client/protocol classes
        // are on the plugin's runtime classpath - lib/modules jars are not loaded
        pluginComposedModule(implementation(project(":debuggers:dap-protocol")))
        pluginComposedModule(implementation(project(":debuggers:hashlink-debug-adapter")))
        pluginComposedModule(implementation(project(":debuggers:vshaxe-hxcpp-debugger-adapter")))
        pluginComposedModule(implementation(project(":debuggers:eval-debugger")))
        pluginComposedModule(implementation(project(":debuggers:browser-debugger")))
        pluginComposedModule(implementation(project(":display-protocol")))
        pluginComposedModule(implementation(project(":profilers:core")))

    }

}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName").get()
        group = providers.gradleProperty("pluginGroup").get()

        ideaVersion.sinceBuild.set(providers.gradleProperty("pluginSinceBuild"))
        ideaVersion.untilBuild.set(providers.gradleProperty("pluginUntilBuild"))
    }


    pluginVerification(fun IntelliJPlatformExtension.PluginVerification.() {
        freeArgs = listOf("-mute", "TemplateWordInPluginId,ForbiddenPluginIdPrefix")
        failureLevel = listOf(
//            VerifyPluginTask.FailureLevel.MISSING_DEPENDENCIES
        )
        ides {
            recommended()
        }
    })
}



// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    keepUnreleasedSection.set(false)
    headerParserRegex.set("(\\d+\\.\\d+(\\.\\d+)*)(.*)") // old version names does not conform to standard

}


tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    test {
        useJUnitPlatform()
        // Tests usually takes 3-6 minutes, so 15 min should be an acceptable timeout (in case of Debugger deadlocks)
        timeout.set(Duration.ofMinutes(15))

        // platform switch: fixture comparisons OVERWRITE their expected files
        // instead of failing - the sanctioned way to regenerate test data after
        // an intended formatter/generator behavior change. Review the git diff!
        systemProperty("idea.tests.overwrite.data", System.getProperty("idea.tests.overwrite.data", "false"))

        // Compiler integration tests (`haxe --wait`) used in IDE services (catalog, resolve, completion).
        // Tests are opt-in as they depend on haxe being installed and might be sensitive to what version is installed.
        val liveCompilerTests = providers.gradleProperty("liveCompilerTests").getOrElse("false").toBoolean()
        if (!liveCompilerTests) {
            logger.lifecycle("SKIPPING live compiler integration tests (opt in with -PliveCompilerTests=true)")
            filter.excludeTestsMatching("com.intellij.plugins.haxe.v2.display.HaxeLiveCompilerIntegrationTest")
        }
    }

    buildPlugin {
        dependsOn(generateParser, generateLexer)
    }

    printProductsReleases {
        channels = listOf(ProductRelease.Channel.EAP)
        types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
        untilBuild = provider { null }

        doLast {
            val latestEap = productsReleases.get().max()
        }
    }

    patchPluginXml {
        version = providers.gradleProperty("pluginVersion").get();
        sinceBuild.set(providers.gradleProperty("pluginSinceBuild"))
        untilBuild.set(providers.gradleProperty("pluginUntilBuild"))


        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(providers.fileContents(layout.projectDirectory.file("DESCRIPTION.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in DESCRIPTION.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        })

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes.set(providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                        (getOrNull(pluginVersion) ?: getUnreleased())
                                .withHeader(true)
                                .withEmptySections(false),
                        Changelog.OutputType.HTML,
                )
            }
        })
    }


    signPlugin {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
//        channels = properties("pluginVersion").map { listOf(it.split('-').getOrElse(1) { "default" }.split('.')) }
    }

    clean {
        dependsOn("cleanGenerated")
    }

    compileJava {
        dependsOn("generateParser")
        dependsOn("generateLexer")
    }

    compileKotlin {
        dependsOn("generateParser")
        dependsOn("generateLexer")
    }

    processResources {
        dependsOn("generateParser")
        dependsOn("generateLexer")
    }


    generateParser {
        dependsOn("generateHaxeParser")
        dependsOn("generateMetadataParser")
        dependsOn("generateHxmlParser")
        enabled = false
    }
    generateLexer {
        dependsOn("generateHaxeLexer")
        dependsOn("generateMetadataLexer")
        dependsOn("generateHxmlLexer")
        enabled = false
    }

    processResources {
        from("src") {
            include("**/*.properties")
        }
    }


    //Makes sure the latest Hashlink debugger and LimeProjectParser is added to the sandbox
    withType<PrepareSandboxTask> {

        dependsOn(":debuggers:hashlink-debug-adapter:buildDebugAdapter")
        from(project(":debuggers:hashlink-debug-adapter").layout.buildDirectory.file("hl/hl-debug-adapter.hl")) {
            into(pluginName.map { "$it/adapter" })
        }

        // Note: the lime project parser/evaluator is a jar that contains compiled java and Haxe sources
        dependsOn(":tools:LimeProjectParser:buildParser")
        from(project(":tools:LimeProjectParser").layout.buildDirectory.file("libs/LimeProjectParser.jar")) {
            into(pluginName.map { "$it/tools" })
        }

    }


    buildPlugin {
        val oldName = archiveBaseName.get() + "-" + archiveVersion.get() + ".zip"
        val newName = "intellij-haxe-" + providers.gradleProperty("platformVersion").get() + ".zip"

        outputs.upToDateWhen {
            file("${project.rootDir}/" + newName).exists()
        }
        doLast {
            copy {
                from("${project.rootDir}/build/distributions/").include(oldName)
                into("${project.rootDir}/")
                rename({ newName })
            }
        }
    }


}


tasks.register<Delete>("cleanGenerated") {
    group = "grammarkit"
    delete = setOf("src/main/gen/")
}

tasks.register<GenerateParserTask>("generateHaxeParser") {
    group = "parsers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/lang/parser/haxe.bnf"))
    targetRootOutputDir.set(File("src/main/gen"))
    purgeOldFiles = false

    outputs.file("src/main/gen/com/intellij/plugins/haxe/lang/parser/HaxeParser.java")
    outputs.file("src/main/gen/com/intellij/plugins/haxe/lang/lexer/HaxeTokenTypes.java")
    outputs.dir("src/main/gen/com/intellij/plugins/haxe/lang/psi")

}
tasks.register<GenerateLexerTask>("generateHaxeLexer") {
    group = "lexers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/lang/lexer/haxe.flex"))
    outputs.file("src/main/gen/com/intellij/plugins/haxe/lang/lexer/_HaxeLexer.java")
    targetRootOutputDir.set(File("src/main/gen"))
    purgeOldFiles = false
}

tasks.register<GenerateParserTask>("generateMetadataParser") {
    group = "parsers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/metadata/parser/metadata.bnf"))
    targetRootOutputDir.set(File("src/main/gen"))
    purgeOldFiles = false

    outputs.file("src/main/gen/com/intellij/plugins/haxe/metadata/parser/HaxeMetadataParser.java")
    outputs.file("src/main/gen/com/intellij/plugins/haxe/metadata/lexer/HaxeMetadataTokenTypes.java")
    outputs.dir("src/main/gen/com/intellij/plugins/haxe/metadata/psi")

}

tasks.register<GenerateLexerTask>("generateMetadataLexer") {
    group = "lexers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/metadata/lexer/metadata.flex"))
    outputs.file("src/main/gen/com/intellij/plugins/haxe/metadata/lexer/MetadataLexer.java")
    targetRootOutputDir.set(File("src/main/gen"))
    purgeOldFiles = false
}


tasks.register<GenerateParserTask>("generateHxmlParser") {
    group = "parsers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/buildsystem/hxml/parser/hxml.bnf"))
    targetRootOutputDir.set(File("src/main/gen"))
    purgeOldFiles = false

    outputs.file("src/main/gen/com/intellij/plugins/haxe/hxml/HXMLParser.java")
    outputs.dir("src/main/gen/com/intellij/plugins/haxe/hxml/psi")
}

tasks.register<GenerateLexerTask>("generateHxmlLexer") {
    group = "lexers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/buildsystem/hxml/lexer/hxml.flex"))
    outputs.file("src/main/gen/com/intellij/plugins/haxe/hxml/lexer/HXMLLexer.java")
    targetRootOutputDir.set(File("src/main/gen"))
    purgeOldFiles = false
}

// One button for every haxelib upload zip (see the per-module buildHaxelibZip
// tasks); the zips land in the project root, git-ignored.
tasks.register("buildHaxelibZips") {
    group = "haxelib"
    description = "Builds every haxelib upload zip into the project root"
    dependsOn(":debuggers:dap-protocol:buildHaxelibZip",
              ":debuggers:intellij-hxcpp-debugger:buildHaxelibZip")
}
