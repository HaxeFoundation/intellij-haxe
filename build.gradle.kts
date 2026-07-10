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
    jvmToolchain(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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


    implementation("org.commonmark:commonmark:0.21.0")
    implementation("org.commonmark:commonmark-ext-autolink:0.21.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.21.0")

    implementation("tools.jackson.core:jackson-databind:3.1.0")
    implementation("org.apache.commons:commons-text:1.14.0")

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

    compileOnly("org.projectlombok:lombok:1.18.44")
    testCompileOnly("org.projectlombok:lombok:1.18.44")
    annotationProcessor ("org.projectlombok:lombok:1.18.44")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.44")

    // TODO upgrade to junit5 (testFramework(TestFrameworkType.JUnit5))
    testImplementation(libs.junit)
    // https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1663#issuecomment-2182516044
    testImplementation(libs.opentest4j)

    intellijPlatform {
        pluginVerifier()
        intellijIdea(providers.gradleProperty("platformVersion"))

        jflex ("1.9.2")
        grammarKit("2023.3.1")

        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })
        bundledModules(providers.gradleProperty("platformBundledModules").map { it.split(',') })

        // TODO upgrade to JUnit5
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Bundled)
        testFramework(TestFrameworkType.Plugin.Java)

        pluginModule(implementation(project(":jps-plugin")))
        pluginModule(implementation(project(":common")))
        pluginModule(implementation(project(":hashlink-debug-adapter")))

        pluginComposedModule(implementation(project(":hxcpp-debugger-protocol")))
        pluginComposedModule(implementation(project(":common")))

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


    // ship the DAP debug adapter bytecode inside the plugin directory (not a jar):
    // the hl executable needs a real file path to run it
    withType<PrepareSandboxTask> {
        dependsOn(":hashlink-debug-adapter:buildDebugAdapter")
        from(project(":hashlink-debug-adapter").layout.buildDirectory.file("hl/hl-debug-adapter.hl")) {
            into(pluginName.map { "$it/adapter" })
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
}
tasks.register<GenerateLexerTask>("generateHaxeLexer") {
    group = "lexers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/lang/lexer/haxe.flex"))
    targetRootOutputDir.set(File("src/main/gen/com/intellij/plugins/haxe/lang/lexer"))
    purgeOldFiles = false
}

tasks.register<GenerateParserTask>("generateMetadataParser") {
    group = "parsers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/metadata/parser/metadata.bnf"))
    targetRootOutputDir.set(File("src/main/gen"))
    purgeOldFiles = false

}

tasks.register<GenerateLexerTask>("generateMetadataLexer") {
    group = "lexers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/metadata/lexer/metadata.flex"))
    targetRootOutputDir.set(File("src/main/gen/com/intellij/plugins/haxe/metadata/lexer/"))
    purgeOldFiles = false
}


tasks.register<GenerateParserTask>("generateHxmlParser") {
    group = "parsers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/buildsystem/hxml/parser/hxml.bnf"))
    targetRootOutputDir.set(File("src/main/gen"))
    purgeOldFiles = false
}

tasks.register<GenerateLexerTask>("generateHxmlLexer") {
    group = "lexers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/buildsystem/hxml/lexer/hxml.flex"))
    targetRootOutputDir.set(File("src/main/gen/com/intellij/plugins/haxe/hxml/lexer"))
    purgeOldFiles = false
}

