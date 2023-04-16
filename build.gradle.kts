import org.gradle.jvm.tasks.Jar
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "1.8.10"
    // Gradle IntelliJ Plugin
    id("org.jetbrains.intellij") version "1.13.3"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "2.0.0"
    // Gradle Qodana Plugin
    id("org.jetbrains.qodana") version "0.1.13"
    // Gradle Kover Plugin
    id("org.jetbrains.kotlinx.kover") version "0.6.1"
    // lombok
    id("io.freefair.lombok") version "8.0.1"
    // generate parser and lexer
    id("org.jetbrains.grammarkit") version "2022.3.1"
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()
sourceSets["main"].java.srcDirs("src/main/gen")

var platformVersion = properties("platformVersion").get();

val ideaBaseDir = "${project.rootDir}/idea-IU"
val ideaTargetDir = "${ideaBaseDir}/ideaIU-${platformVersion}"


dependencies {
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

}


allprojects {
    apply(plugin = "java")
    apply(plugin = "idea")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.intellij")

    // Configure project's dependencies
    repositories {
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
    intellij {
        pluginName.set(properties("pluginName"))
        version.set(properties("platformVersion"))
        type.set(properties("platformType"))

        ideaDependencyCachePath.set("${project.rootDir}/idea-IU")
        sandboxDir.set("${project.rootDir}/build/idea-sandbox")

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
        plugins.set(properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) })
    }

}

subprojects {
    tasks {
        runIde { isEnabled = false }
        patchPluginXml { isEnabled = false }
        verifyPlugin { isEnabled = false }
        prepareSandbox { isEnabled = false }
        buildSearchableOptions { isEnabled = false }
    }
}


// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    headerParserRegex.set("\\d+\\.\\d+(\\.\\d+)*(.*)") // old version names does not conform to standard
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}


tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    buildPlugin {
        dependsOn(generateParser, generateLexer)
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        pluginXmlFiles.set(listOf(
                file("src/main/resources/META-INF/plugin.xml"),
                file("src/main/resources/META-INF/debugger-support.xml"),
                file("src/main/resources/META-INF/flex-debugger-support.xml")
        ))

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

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
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
        channels.set(properties("pluginVersion").map { listOf(it.split('-').getOrElse(1) { "default" }.split('.').first()) })
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

    jar {
//        finalizedBy("copyJar")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        archiveBaseName.set("intellij-haxe")
        includeEmptyDirs = false;

        include("**/*").includeEmptyDirs = false
        // include submodules
        from(project(":jps-plugin").sourceSets["main"].output).include("**/*").includeEmptyDirs = false
        from(project(":common").sourceSets["main"].output).include("**/*").includeEmptyDirs = false
        from(project(":hxcpp-debugger-protocol").sourceSets["main"].output).include("**/*").includeEmptyDirs = false

        val type = properties("platformType").get();
        val version = properties("platformVersion").get();
        val pluginDir = "${project.rootDir}/idea-${type}/idea${type}-${version}/lib/forms_rt.jar"
        from(zipTree(pluginDir)).include("com/intellij/uiDesigner/core/*.class")

    }
    instrumentedJar {
        finalizedBy("copyJar")
    }
}


tasks.create<Copy>("copyJar") {
        val jarName = "intellij-haxe-" + properties("platformVersion").get() + ".jar"
        val jarTask = tasks.getByName("instrumentedJar") as Jar
        from("${project.rootDir}/build/libs/").include(jarTask.archiveFileName.get())
        into("${project.rootDir}")
        rename({ jarName })
}


tasks.create<Delete>("cleanGenerated") {
    group = "grammarkit"
    delete = setOf("src/main/gen/")
}

tasks.create<GenerateParserTask>("generateHaxeParser") {
    group = "parsers"
    targetRoot.set("src/main/gen")
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/lang/parser/haxe.bnf"))
    pathToParser.set("com/intellij/plugins/haxe/lang/parser/HaxeParser.java")
    pathToPsiRoot.set("com/intellij/plugins/haxe/lang")
}
tasks.create<GenerateLexerTask>("generateHaxeLexer") {
    group = "lexers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/lang/lexer/haxe.flex"))
    targetDir.set("src/main/gen/com/intellij/plugins/haxe/lang/lexer")
    targetClass.set("HaxeLexer")
}

tasks.create<GenerateParserTask>("generateMetadataParser") {
    group = "parsers"
    targetRoot.set("src/main/gen")
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/metadata/parser/metadata.bnf"))
    pathToParser.set("com/intellij/plugins/haxe/metadata/lexer/MetadataLexer.java")
    pathToPsiRoot.set("com/intellij/plugins/haxe/lang")

}

tasks.create<GenerateLexerTask>("generateMetadataLexer") {
    group = "lexers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/metadata/lexer/metadata.flex"))
    targetDir.set("src/main/gen/com/intellij/plugins/haxe/metadata/lexer/")
    targetClass.set("MetadataLexer")
}


tasks.create<GenerateParserTask>("generateHxmlParser") {
    group = "parsers"
    targetRoot.set("src/main/gen")
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/hxml/parser/hxml.bnf"))
    pathToParser.set("com/intellij/plugins/haxe/hxml/parser/HXMLParser.java")
    pathToPsiRoot.set("com/intellij/plugins/haxe/lang")
}

tasks.create<GenerateLexerTask>("generateHxmlLexer") {
    group = "lexers"
    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/hxml/lexer/hxml.flex"))
    targetDir.set("src/main/gen/com/intellij/plugins/haxe/hxml/lexer")
    targetClass.set("HXMLLexer")
}

