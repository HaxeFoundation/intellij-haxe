import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    // Gradle IntelliJ Plugin
    id("org.jetbrains.intellij.platform") version "2.1.0"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "2.0.0"
    // Gradle Qodana Plugin
    id("org.jetbrains.qodana") version "0.1.13"
    // Gradle Kover Plugin
    id("org.jetbrains.kotlinx.kover") version "0.6.1"
    // generate parser and lexer
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
    // console output for tests
    id("com.adarshr.test-logger") version "3.2.0"
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()
sourceSets["main"].java.srcDirs("src/main/gen")

var platformVersion = properties("platformVersion").get();
var platformType = properties("platformType").get();

val ideaBaseDir = "${project.rootDir}/idea"
val ideaTargetDir = "${ideaBaseDir}/idea${platformType}-${platformVersion}"

dependencies {
    implementation("org.commonmark:commonmark:0.21.0")
    implementation("org.commonmark:commonmark-ext-autolink:0.21.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.21.0")

    implementation(project(":common"))
    implementation(project(":jps-plugin"))
    implementation(project(":hxcpp-debugger-protocol"))

    val flexShared = "${ideaTargetDir}/config/plugins/flex/lib/flex-shared.jar"
    val flexSupport = "${ideaTargetDir}/config/plugins/flex/lib/FlexSupport.jar"

    compileOnly(files(flexShared))
    compileOnly(files(flexSupport))

    compileOnly(files("${ideaTargetDir}/lib/openapi.jar"))
    compileOnly(files("${ideaTargetDir}/lib/util.jar"))

    testCompileOnly(project(":jps-plugin"))
    testCompileOnly(project(":common"))
    testCompileOnly(project(":hxcpp-debugger-protocol"))

    testCompileOnly(files(flexShared))
    testCompileOnly(files(flexSupport))

    testCompileOnly(files("${ideaTargetDir}/lib/openapi.jar"))
    testCompileOnly(files("${ideaTargetDir}/lib/util.jar"))

    compileOnly("org.projectlombok:lombok:1.18.34")
    testCompileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor ("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.34")

    // TODO upgrade to junit5 (testFramework(TestFrameworkType.JUnit5))
    testImplementation("junit:junit:4.13.2")
    // https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1663#issuecomment-2182516044
    testImplementation("org.opentest4j:opentest4j:1.3.0")

    intellijPlatform {
        pluginVerifier()
        instrumentationTools()
        create(platformType, platformVersion)

        plugins(properties("platformPlugins").map { it.split(',') })
        bundledPlugins(properties("platformBundledPlugins").map { it.split(',') })

        // TODO upgrade to JUnit5
        //testFramework(TestFrameworkType.JUnit5)
        testFramework(TestFrameworkType.Bundled)

    }

}
allprojects {
    apply(plugin = "java")
    apply(plugin = "idea")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

subprojects {
    apply(plugin = "org.jetbrains.intellij.platform.module")

    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
            mavenCentral()
        }
    }

    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.34")
        testCompileOnly("org.projectlombok:lombok:1.18.34")
        annotationProcessor ("org.projectlombok:lombok:1.18.34")
        testAnnotationProcessor ("org.projectlombok:lombok:1.18.34")

        intellijPlatform {
            instrumentationTools()

            val type = providers.gradleProperty("platformType")
            val version = providers.gradleProperty("platformVersion")
            create(type, version)

            plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })
            bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

            testFramework(TestFrameworkType.Bundled)
        }
    }
}

apply(plugin = "org.jetbrains.intellij.platform")


// Configure project's dependencies
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        mavenCentral()
    }
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellijPlatform  {
    pluginConfiguration   {
    name = properties("pluginName").get()
    group = properties("pluginGroup").get()

    ideaVersion.sinceBuild.set(properties("pluginSinceBuild"))
    ideaVersion.untilBuild.set(properties("pluginUntilBuild"))

    }
    verifyPlugin {
        freeArgs = listOf("-mute", "TemplateWordInPluginId,ForbiddenPluginIdPrefix")
        failureLevel = listOf(
//            VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            VerifyPluginTask.FailureLevel.MISSING_DEPENDENCIES
        )
        ides {
            //TODO  problem verifying 2024.2 beta, emojipicker not found, + timeout ?
//            recommended()
            select {
                sinceBuild.set("240")
                untilBuild.set("241.*")
//                sinceBuild.set(properties("pluginSinceBuild"))
//                untilBuild.set(properties("pluginUntilBuild"))
            }
        }
    }
//    instrumentCode = false
}



grammarKit {
    jflexRelease.set("1.9.1")
}



// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    keepUnreleasedSection.set(false)
    headerParserRegex.set("(\\d+\\.\\d+(\\.\\d+)*)(.*)") // old version names does not conform to standard

}


tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
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
        version =properties("pluginVersion").get();
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))


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
        changeNotes.set(properties("pluginVersion").map { pluginVersion ->
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
        certificateChain.set(environment("CERTIFICATE_CHAIN"))
        privateKey.set(environment("PRIVATE_KEY"))
        password.set(environment("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(environment("PUBLISH_TOKEN"))
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


    buildPlugin {
        val oldName = archiveBaseName.get() + "-" + archiveVersion.get() + ".zip"
        val newName = "intellij-haxe-" + properties("platformVersion").get() + ".zip"

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
    targetRootOutputDir.set(File("src/main/gen"))
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/lang/parser/haxe.bnf"))
    pathToParser.set("com/intellij/plugins/haxe/lang/parser/HaxeParser.java")
    pathToPsiRoot.set("com/intellij/plugins/haxe/lang")
}
tasks.register<GenerateLexerTask>("generateHaxeLexer") {
    group = "lexers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/lang/lexer/haxe.flex"))
    targetOutputDir.set(File("src/main/gen/com/intellij/plugins/haxe/lang/lexer"))
}

tasks.register<GenerateParserTask>("generateMetadataParser") {
    group = "parsers"
    targetRootOutputDir.set(File("src/main/gen"))
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/metadata/parser/metadata.bnf"))
    pathToParser.set("com/intellij/plugins/haxe/metadata/lexer/MetadataLexer.java")
    pathToPsiRoot.set("com/intellij/plugins/haxe/lang")

}

tasks.register<GenerateLexerTask>("generateMetadataLexer") {
    group = "lexers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/metadata/lexer/metadata.flex"))
    targetOutputDir.set(File("src/main/gen/com/intellij/plugins/haxe/metadata/lexer/"))
}


tasks.register<GenerateParserTask>("generateHxmlParser") {
    group = "parsers"
    targetRootOutputDir.set(File("src/main/gen"))
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/buildsystem/hxml/parser/hxml.bnf"))
    pathToParser.set("com/intellij/plugins/haxe/hxml/parser/HXMLParser.java")
    pathToPsiRoot.set("com/intellij/plugins/haxe/lang")
}

tasks.register<GenerateLexerTask>("generateHxmlLexer") {
    group = "lexers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/buildsystem/hxml/lexer/hxml.flex"))
    targetOutputDir.set(File("src/main/gen/com/intellij/plugins/haxe/hxml/lexer"))
}

