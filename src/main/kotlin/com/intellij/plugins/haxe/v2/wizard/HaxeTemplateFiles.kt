package com.intellij.plugins.haxe.v2.wizard

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.plugins.haxe.HaxeWizardBundle

/**
 * Renders the project-generator files from the platform file templates in
 * {@code fileTemplates/j2ee/Haxe Project *.ft} (the "Haxe project" group under
 * Settings | File and Code Templates | Other — user-customizable). Code owns
 * the CONDITIONAL assembly (which lines a target needs, escaping); the
 * templates own the file shapes.
 */
object HaxeTemplateFiles {

  const val SOURCE_DIR: String = HaxeModuleBuilder.SOURCE_DIR

  /**
   * The hxml targets the plain-project template offers, with the output flag
   * and default output each target's manual page prescribes. HashLink is TWO
   * entries because the output EXTENSION selects the compilation mode: a
   * {@code .hl} file is VM bytecode, a {@code .c} file switches the
   * generator to HL/C sources. The legacy source-generating targets carry
   * the support library the compiler requires for them.
   */
  enum class HxmlTargetOption(private val labelKey: String, private val outputFlag: String, val defaultOutput: String,
                              val lib: String? = null) {
    HASHLINK_VM("haxe.wizard.hxml.target.hashlink.vm", "--hl", "out/app.hl"),
    HASHLINK_C("haxe.wizard.hxml.target.hashlink.c", "--hl", "out/c/main.c"),
    JAVASCRIPT("haxe.wizard.hxml.target.javascript", "--js", "out/app.js"),
    NEKO("haxe.wizard.hxml.target.neko", "--neko", "out/app.n"),
    CPP("haxe.wizard.hxml.target.cpp", "--cpp", "out/cpp"),
    JVM("haxe.wizard.hxml.target.jvm", "--jvm", "out/app.jar"),
    JAVA_LEGACY("haxe.wizard.hxml.target.java", "--java", "out/java", "hxjava"),
    CSHARP("haxe.wizard.hxml.target.csharp", "--cs", "out/cs", "hxcs"),
    PHP("haxe.wizard.hxml.target.php", "--php", "out/php"),
    PYTHON("haxe.wizard.hxml.target.python", "--python", "out/app.py"),
    LUA("haxe.wizard.hxml.target.lua", "--lua", "out/app.lua"),
    FLASH("haxe.wizard.hxml.target.flash", "--swf", "out/app.swf"),
    INTERP("haxe.wizard.hxml.target.interp", "--interp", "");

    /** Whether the target writes anything — the interpreter runs the program instead. */
    val hasOutput: Boolean get() = defaultOutput.isNotEmpty()

    fun outputLine(output: String): String = if (hasOutput) "$outputFlag $output" else outputFlag

    override fun toString(): String = HaxeWizardBundle.message(labelKey)
  }

  /**
   * Everything the HXML template form collects. {@code swfHeader} arrives
   * preassembled ({@code width:height:fps:color}) — the form owns the four
   * fields, the assembly here only places the line.
   */
  data class HxmlSpec(
    val target: HxmlTargetOption,
    val mainClass: String = "Main",
    val output: String = target.defaultOutput,
    val dce: String = "std",
    val jsSourceMap: Boolean = false,
    val swfVersion: String = "",
    val swfHeader: String = "")

  fun starterMainHx(project: Project, className: String): String =
    render(project, "Haxe Project Main", mapOf("MAIN_CLASS" to className))

  fun hxml(project: Project, spec: HxmlSpec): String {
    val libLine = spec.target.lib?.let { "-lib $it\n" } ?: ""
    val extras = buildString {
      if (spec.target == HxmlTargetOption.JAVASCRIPT && spec.jsSourceMap) append("\n-D source-map")
      if (spec.target == HxmlTargetOption.FLASH) {
        if (spec.swfVersion.isNotBlank()) append("\n--swf-version ${spec.swfVersion}")
        if (spec.swfHeader.isNotBlank()) append("\n--swf-header ${spec.swfHeader}")
      }
    }
    return render(project, "Haxe Project Build", mapOf(
      "MAIN_CLASS" to spec.mainClass,
      "LIB_LINE" to libLine,
      "DCE" to spec.dce,
      "OUTPUT_LINE" to spec.target.outputLine(spec.output),
      "EXTRA_LINES" to extras))
  }

  /** project.xml for lime or openfl ("haxelib" decides which framework the file pulls in). */
  fun limeProjectXml(project: Project, haxelib: String, title: String, pkg: String, width: Int, height: Int, fps: Int): String =
    render(project, "Haxe Project Lime", appVariables(title, pkg, width, height, fps) + ("HAXELIB" to haxelib))

  /** project.nmml for NME. */
  fun nmmlProjectXml(project: Project, title: String, pkg: String, width: Int, height: Int, fps: Int): String =
    render(project, "Haxe Project NME", appVariables(title, pkg, width, height, fps))

  /** The application variables the lime-family project templates share. */
  private fun appVariables(title: String, pkg: String, width: Int, height: Int, fps: Int): Map<String, String> = mapOf(
    "TITLE" to xml(title),
    "PACKAGE" to xml(pkg),
    "FILE" to xml(fileNameOf(title)),
    "WIDTH" to width.toString(),
    "HEIGHT" to height.toString(),
    "FPS" to fps.toString())

  fun limeMainHx(project: Project): String = render(project, "Haxe Project Lime Main", emptyMap())

  fun openflMainHx(project: Project): String = render(project, "Haxe Project OpenFL Main", emptyMap())

  fun nmeMainHx(project: Project): String = render(project, "Haxe Project NME Main", emptyMap())

  /**
   * haxelib.json carrying every field the bundled schema REQUIRES (name,
   * license, releasenote, contributors, version) plus the useful optionals;
   * classPath is fixed to the template's source folder.
   */
  fun haxelibJson(project: Project,
                  name: String,
                  license: String,
                  version: String,
                  description: String,
                  url: String,
                  tags: List<String>,
                  contributors: List<String>,
                  releasenote: String): String =
    render(project, "Haxe Project Haxelib", mapOf(
      "NAME" to json(name),
      "URL" to json(url),
      "LICENSE" to json(license),
      "TAGS" to tags.joinToString(", ") { "\"${json(it)}\"" },
      "DESCRIPTION" to json(description),
      "VERSION" to json(version),
      "RELEASENOTE" to json(releasenote),
      "CONTRIBUTORS" to contributors.joinToString(", ") { "\"${json(it)}\"" }))

  /**
   * The library's development hxml: gives the IDE a build context (compiler
   * completion, diagnostics, the compilation server) without producing any
   * artifact. Libraries have no -main, so the starter class is listed as an
   * explicit compile root — a bare module name in hxml — which is what makes
   * the compiler actually type the library.
   */
  fun haxelibDevHxml(project: Project, rootClassName: String): String =
    render(project, "Haxe Project Dev", mapOf("ROOT_CLASS" to rootClassName))

  /** A starter class named after the library (capitalized, non-identifier chars stripped). */
  fun haxelibStarterClass(project: Project, libName: String): Pair<String, String> {
    val className = haxelibClassNameOf(libName)
    return className to render(project, "Haxe Project Haxelib Class", mapOf("CLASS_NAME" to className))
  }

  fun haxelibClassNameOf(libName: String): String = libName
    .filter { it.isLetterOrDigit() || it == '_' }
    .ifEmpty { "Lib" }
    .replaceFirstChar { it.uppercaseChar() }

  /**
   * Plain {@code ${VAR}} substitution over the template's raw text — the
   * project templates use no Velocity features, and skipping the engine
   * keeps hxml {@code #} comment lines literal (Velocity would parse them).
   */
  private fun render(project: Project, templateName: String, variables: Map<String, String>): String {
    var text = FileTemplateManager.getInstance(project).getJ2eeTemplate(templateName).text
    text = text.replace("\${SOURCE_DIR}", SOURCE_DIR)
    for ((key, value) in variables) {
      text = text.replace("\${$key}", value)
    }
    return text
  }

  private fun fileNameOf(title: String): String =
    title.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.ifEmpty { "App" }

  private fun xml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

  private fun json(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
}
