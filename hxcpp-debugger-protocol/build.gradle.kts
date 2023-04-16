import java.io.ByteArrayOutputStream

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

val hxcppSrcFolder = "src/main/haxe"
val hxcppDebuggerGit = properties("hxcppDebuggerGit").get()
val hxcppDebuggerBranch = properties("hxcppDebuggerBranch").get()

val hxjavaVersion = properties("hxjavaVersion").get()
val haxeLibChangeVersion = properties("haxeLibChangeVersion").get()
val generateHxcppDebugger = properties("generateHxcppDebugger").getOrElse("false").toBoolean()

val hxcppGeneratedFolder = if (generateHxcppDebugger) "src/gen/src" else "src/fallback/java"

sourceSets["main"].java.srcDirs(hxcppGeneratedFolder)

//// helping intellij detecting source directories
idea.module {
    generatedSourceDirs.add(File("src/gen/src"))
    sourceDirs.add(File("src/main/fallback"))
    sourceDirs.add(File("src/main/java"))
}

tasks {
    compileJava {
        dependsOn("generateDebuggerJavaSource")
    }
    clean {
        dependsOn("cleanGenerated")
    }
}


tasks.create<Exec>("installHxJava") {
    group = "hxcpp"
    onlyIf({ generateHxcppDebugger })
    commandLine = listOf("haxelib", "install", "hxjava", hxjavaVersion, haxeLibChangeVersion)
//    if (!logger.isDebugEnabled()) standardOutput = ByteArrayOutputStream() // avoid to much output (loads of empty lines on install)

}

tasks.create<Exec>("installHxcppDebugger") {
    group = "hxcpp"
    onlyIf({ generateHxcppDebugger })
    commandLine = listOf("haxelib", "git", "hxcpp-debugger", hxcppDebuggerGit, hxcppDebuggerBranch, haxeLibChangeVersion)
    if (!logger.isDebugEnabled()) standardOutput = ByteArrayOutputStream() // avoid to much output (loads of empty lines on install)
}

tasks.create<Exec>("generateDebuggerJavaSource") {
    group = "hxcpp"
    onlyIf({ generateHxcppDebugger })
    dependsOn("installHxJava")
    dependsOn("installHxcppDebugger")

    inputs.file(File("${hxcppSrcFolder}/JavaProtocol.hx"))
    outputs.upToDateWhen { File("gen/src").exists() }

    workingDir = File("src/main/haxe/")
    commandLine = listOf("haxe", "-cp", "..", "-java", "../../gen/", "-main", "JavaProtocol",
            "-lib", "hxcpp-debugger:git:$hxcppDebuggerGit#$hxcppDebuggerBranch",
            "-lib", "hxjava:$hxjavaVersion")
}


tasks.create<Delete>("cleanGenerated") {
    group = "hxcpp"
    delete.add("src/gen/")
}
