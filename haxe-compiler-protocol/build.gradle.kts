
fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

val protocolSrcFolder = "src/main/haxe"

val hxjavaVersion = properties("hxjavaVersion").get()

tasks.register<Exec>("generateCompilerProtocolJavaSource") {
    group = "haxe-compiler"
//    dependsOn("installHxJava")

    inputs.file(File("${protocolSrcFolder}/HaxeCompilerProtocol.hx"))
    outputs.upToDateWhen { File("gen/src").exists() }

    workingDir = File("src/main/haxe/")
    commandLine = listOf("haxe", "-cp", "..", "--jvm", "../../bin/", "-main", "HaxeCompilerProtocol",
        "-lib", "hxjava:$hxjavaVersion")
}

