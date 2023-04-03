/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
//    id("org.jetbrains.intellij") version "1.13.3"
    id("org.jetbrains.intellij") version "1.10.1"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "2.0.0"
    // Gradle Qodana Plugin
    id("org.jetbrains.qodana") version "0.1.13"
    // Gradle Kover Plugin
    id("org.jetbrains.kotlinx.kover") version "0.6.1"
    // lombok
    id("io.freefair.lombok") version "6.3.0"
    // generate parser and lexer
//    id("org.jetbrains.grammarkit") version "2022.3.1"
//    id("org.jetbrains.grammarkit") version "2022.3"
    id("org.jetbrains.grammarkit") version "2021.2.2"
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

sourceSets["main"].java.srcDirs("src/main/gen")

dependencies{
    implementation(project(":common"))
    implementation(project(":hxcpp-debugger-protocol"))
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

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
        plugins.set(properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) })
    }

}



// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}


tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    buildPlugin{
        dependsOn(generateParser, generateLexer)
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))


        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes.set(properties("pluginVersion").map {
            pluginVersion ->
            with(changelog) {
                renderItem(
                        (getOrNull(pluginVersion) ?: getUnreleased())
                                .withHeader(false)
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


    generateParser{
      dependsOn("generateHaxeParser")
      dependsOn("generateMetadataParser")
      dependsOn("generateHxmlParser")
        enabled = false
    }
    generateLexer{
        dependsOn("generateHaxeLexer")
        dependsOn("generateMetadataLexer")
        dependsOn("generateHxmlLexer")
        enabled = false
    }

    processResources {
        from ("src") {
            include("**/*.properties")
        }
    }

}

tasks.create<Delete>("cleanGenerated") {
    group = "grammarkit"
    delete = setOf("src/main/gen/")
}

tasks.create<GenerateParserTask>("generateHaxeParser") {
    group = "parsers"
    targetRoot.set("src/main/gen")
//    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/lang/parser/haxe.bnf"))
    source.set("src/main/java/com/intellij/plugins/haxe/lang/parser/haxe.bnf")
    pathToParser.set("com/intellij/plugins/haxe/lang/parser/HaxeParser.java")
    pathToPsiRoot.set("com/intellij/plugins/haxe/lang")
}
tasks.create<GenerateLexerTask>("generateHaxeLexer") {
    group = "lexers"
//    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/lang/lexer/HaxeLexer.flex"))
    source.set("src/main/java/com/intellij/plugins/haxe/lang/lexer/HaxeLexer.flex")
    targetDir.set("src/main/gen/com/intellij/plugins/haxe/lang/lexer")
    targetClass.set("HaxeLexer")
}

tasks.create<GenerateParserTask>("generateMetadataParser") {
    group = "parsers"
    targetRoot.set("src/main/gen")
//    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/metadata/parser/metadata.bnf"))
    source.set("src/main/java/com/intellij/plugins/haxe/metadata/parser/metadata.bnf")
    pathToParser.set("com/intellij/plugins/haxe/metadata/lexer/MetadataLexer.java")
    pathToPsiRoot.set("com/intellij/plugins/haxe/lang")

}

tasks.create<GenerateLexerTask>("generateMetadataLexer") {
    group = "lexers"
//    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/metadata/lexer/metadata.flex"))
    source.set("src/main/java/com/intellij/plugins/haxe/metadata/lexer/metadata.flex")
    targetDir.set("src/main/gen/com/intellij/plugins/haxe/metadata/lexer/")
    targetClass.set("MetadataLexer")
}


tasks.create<GenerateParserTask>("generateHxmlParser") {
    group = "parsers"
    targetRoot.set("src/main/gen")
//    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/hxml/parser/hxml.bnf"))
    source.set("src/main/java/com/intellij/plugins/haxe/hxml/parser/hxml.bnf")
    pathToParser.set("com/intellij/plugins/haxe/hxml/parser/HXMLParser.java")
    pathToPsiRoot.set("com/intellij/plugins/haxe/lang")
}

tasks.create<GenerateLexerTask>("generateHxmlLexer") {
    group = "lexers"
//    sourceFile.set(File("src/main/java/com/intellij/plugins/haxe/hxml/lexer/hxml.flex"))
    source.set("src/main/java/com/intellij/plugins/haxe/hxml/lexer/hxml.flex")
    targetDir.set("src/main/gen/com/intellij/plugins/haxe/hxml/lexer")
    targetClass.set("HXMLLexer")
}

