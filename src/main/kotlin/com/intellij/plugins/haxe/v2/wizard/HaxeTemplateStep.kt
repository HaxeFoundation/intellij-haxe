package com.intellij.plugins.haxe.v2.wizard

import com.intellij.ide.wizard.AbstractNewProjectWizardMultiStepBase
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.GitNewProjectWizardData.Companion.gitData
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.baseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.project.Project
import com.intellij.plugins.haxe.HaxeWizardBundle
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetOptions
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns

/**
 * The template switcher: Empty | HXML | Lime | OpenFL | NME | Haxelib —
 * each child step owns its fields and its generation. Only the SELECTED
 * template's {@code setupProject} runs (multi-step base contract). The
 * haxelib-repository choice sits here because it applies to every template.
 */
class HaxeTemplateStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {

  /** Dropdown entry for the repository choice; local = create a project .haxelib. */
  private data class RepoChoice(val local: Boolean, val label: String) {
    override fun toString(): String = label
  }

  private val repoChoices = listOf(
    RepoChoice(false, HaxeWizardBundle.message("haxe.wizard.haxelib.repo.global")),
    RepoChoice(true, HaxeWizardBundle.message("haxe.wizard.haxelib.repo.local")))

  private val switcher = Switcher(this)
  private val repoProperty: GraphProperty<RepoChoice> = propertyGraph.property(repoChoices.first())

  override fun setupUI(builder: Panel) {
    switcher.setupUI(builder)
    builder.row(HaxeWizardBundle.message("haxe.wizard.haxelib.repo.label")) {
      comboBox(repoChoices).bindItem(repoProperty)
    }
  }

  override fun setupProject(project: Project) {
    switcher.setupProject(project)
    if (repoProperty.get().local) {
      HaxeTemplateScaffold.createLocalHaxelibRepo(project, this, gitData?.git == true)
    }
  }

  private class Switcher(parent: NewProjectWizardStep) : AbstractNewProjectWizardMultiStepBase(parent) {
    override val label: String get() = HaxeWizardBundle.message("haxe.wizard.template.label")

    override fun initSteps(): Map<String, NewProjectWizardStep> = linkedMapOf(
      HaxeWizardBundle.message("haxe.wizard.template.empty") to EmptyTemplateStep(this),
      "HXML" to HxmlTemplateStep(this),
      "Lime" to LimeFamilyTemplateStep(this, LimeFlavor.LIME),
      "OpenFL" to LimeFamilyTemplateStep(this, LimeFlavor.OPENFL),
      "NME" to LimeFamilyTemplateStep(this, LimeFlavor.NME),
      "Haxelib" to HaxelibTemplateStep(this))
  }
}

/** Just the module with its src root — the closest thing to Empty Project that is still a Haxe module. */
private class EmptyTemplateStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
  override fun setupProject(project: Project) {
    HaxeTemplateScaffold.createModule(project, this)
  }
}

/**
 * Plain haxe project: target choice with the fields each target's manual
 * page prescribes — main class and output for all (the output default
 * follows the target until edited; the interpreter target has none), dead
 * code elimination for all, a source-map toggle for JavaScript, version and
 * stage header for Flash. Generates the starter class (named after the main
 * class) and a build.hxml registered as active.
 */
private class HxmlTemplateStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {

  private val dceModes = listOf("std", "full", "no")

  private val targetProperty: GraphProperty<HaxeTemplateFiles.HxmlTargetOption> =
    propertyGraph.property(HaxeTemplateFiles.HxmlTargetOption.HASHLINK_VM)
  private val mainClassProperty: GraphProperty<String> = propertyGraph.property("Main")
  private val outputProperty: GraphProperty<String> = propertyGraph.lazyProperty { targetProperty.get().defaultOutput }
  private val dceProperty: GraphProperty<String> = propertyGraph.property("std")
  private val jsSourceMapProperty: GraphProperty<Boolean> = propertyGraph.property(false)
  private val swfVersionProperty: GraphProperty<String> = propertyGraph.property("")
  private val swfWidthProperty: GraphProperty<String> = propertyGraph.property(DEFAULT_SWF_WIDTH.toString())
  private val swfHeightProperty: GraphProperty<String> = propertyGraph.property(DEFAULT_SWF_HEIGHT.toString())
  private val swfFpsProperty: GraphProperty<String> = propertyGraph.property(DEFAULT_SWF_FPS.toString())
  private val swfColorProperty: GraphProperty<String> = propertyGraph.property(DEFAULT_SWF_COLOR)

  init {
    // the output default follows the selected target until edited manually
    outputProperty.dependsOn(targetProperty) { targetProperty.get().defaultOutput }
  }

  override fun setupUI(builder: Panel) {
    builder.row(HaxeWizardBundle.message("haxe.wizard.target.label")) {
      comboBox(HaxeTemplateFiles.HxmlTargetOption.entries).bindItem(targetProperty)
    }
    builder.row(HaxeWizardBundle.message("haxe.wizard.main.class.label")) {
      textField().bindText(mainClassProperty).columns(20)
    }
    builder.row(HaxeWizardBundle.message("haxe.wizard.dce.label")) {
      comboBox(dceModes).bindItem(dceProperty)
    }
    builder.row(HaxeWizardBundle.message("haxe.wizard.output.label")) {
      textField().bindText(outputProperty).columns(28)
    }.visibleIf(targetProperty.transform { it.hasOutput })
    builder.row {
      checkBox(HaxeWizardBundle.message("haxe.wizard.js.sourcemap.label")).bindSelected(jsSourceMapProperty)
    }.visibleIf(targetProperty.transform { it == HaxeTemplateFiles.HxmlTargetOption.JAVASCRIPT })
    builder.row(HaxeWizardBundle.message("haxe.wizard.swf.header.label")) {
      textField().bindText(swfWidthProperty).columns(5)
      label("x")
      textField().bindText(swfHeightProperty).columns(5)
      label(HaxeWizardBundle.message("haxe.wizard.fps.label"))
      textField().bindText(swfFpsProperty).columns(4)
      label(HaxeWizardBundle.message("haxe.wizard.swf.color.label"))
      textField().bindText(swfColorProperty).columns(7)
      label(HaxeWizardBundle.message("haxe.wizard.swf.version.label"))
      textField().bindText(swfVersionProperty).columns(4)
    }.visibleIf(targetProperty.transform { it == HaxeTemplateFiles.HxmlTargetOption.FLASH })
  }

  override fun setupProject(project: Project) {
    val contentRoot = HaxeTemplateScaffold.createModule(project, this) ?: return
    val target = targetProperty.get()
    val mainClass = mainClassProperty.get().ifBlank { "Main" }
    val isFlash = target == HaxeTemplateFiles.HxmlTargetOption.FLASH
    val spec = HaxeTemplateFiles.HxmlSpec(
      target = target,
      mainClass = mainClass,
      output = outputProperty.get().ifBlank { target.defaultOutput },
      dce = dceProperty.get(),
      jsSourceMap = jsSourceMapProperty.get(),
      swfVersion = swfVersion(),
      swfHeader = if (isFlash) swfHeader() else "")

    val files = mapOf(
      "${HaxeTemplateFiles.SOURCE_DIR}/$mainClass.hx" to HaxeTemplateFiles.starterMainHx(project, mainClass),
      "build.hxml" to HaxeTemplateFiles.hxml(project, spec))
    HaxeTemplateScaffold.writeAndRegister(project, contentRoot, files, buildFileName = "build.hxml")
  }

  /**
   * The {@code width:height:fps:color} stage header from the free-text fields;
   * non-numeric input would render an hxml the compiler rejects, so each
   * field falls back to its default.
   */
  private fun swfHeader(): String {
    val width = swfWidthProperty.get().toIntOrNull() ?: DEFAULT_SWF_WIDTH
    val height = swfHeightProperty.get().toIntOrNull() ?: DEFAULT_SWF_HEIGHT
    val fps = swfFpsProperty.get().toIntOrNull() ?: DEFAULT_SWF_FPS
    val colorText = swfColorProperty.get().trim()
    // the stage color is an RGB hex sextet (ffffff)
    val color = if (colorText.matches(Regex("[0-9a-fA-F]{6}"))) colorText else DEFAULT_SWF_COLOR
    return "$width:$height:$fps:$color"
  }

  /** --swf-version takes a plain number (14 or 11.2); anything else drops the line. */
  private fun swfVersion(): String {
    // one or two dot-separated numbers
    return swfVersionProperty.get().trim().takeIf { it.matches(Regex("\\d+(\\.\\d+)?")) } ?: ""
  }

  private companion object {
    const val DEFAULT_SWF_WIDTH = 960
    const val DEFAULT_SWF_HEIGHT = 640
    const val DEFAULT_SWF_FPS = 60
    const val DEFAULT_SWF_COLOR = "ffffff"
  }
}

private enum class LimeFlavor(val haxelib: String, val buildFileName: String) {
  LIME("lime", "project.xml"),
  OPENFL("openfl", "project.xml"),
  NME("nme", "project.nmml")
}

/**
 * Lime / OpenFL / NME application project: target plus the common
 * project-file properties, defaults matching the frameworks' own templates.
 * The application name follows the project name until edited.
 */
private class LimeFamilyTemplateStep(parent: NewProjectWizardStep, private val flavor: LimeFlavor)
  : AbstractNewProjectWizardStep(parent) {

  /** id = the name the target-selection store expects; label = the presentable name. */
  private data class TargetChoice(val id: String, val label: String) {
    override fun toString(): String = label
  }

  // each build system's CONFIGURED target list (Settings | Haxe | Frameworks)
  // is the single source - the tool window's target selector offers the same
  private val targets: List<TargetChoice> = HaxeTargetOptions.choicesFor(buildFileType())
    .map { TargetChoice(it.id, it.displayName) }

  private fun buildFileType(): HaxeBuildFileType = when (flavor) {
    LimeFlavor.NME -> HaxeBuildFileType.NMML
    LimeFlavor.OPENFL -> HaxeBuildFileType.OPENFL
    LimeFlavor.LIME -> HaxeBuildFileType.LIME
  }

  private val targetProperty: GraphProperty<TargetChoice> = propertyGraph.property(defaultTarget())
  private val titleProperty: GraphProperty<String> = propertyGraph.lazyProperty { baseData?.name ?: "App" }
  private val packageProperty: GraphProperty<String> = propertyGraph.lazyProperty { defaultPackage() }
  private val widthProperty: GraphProperty<String> = propertyGraph.property(DEFAULT_WIDTH.toString())
  private val heightProperty: GraphProperty<String> = propertyGraph.property(DEFAULT_HEIGHT.toString())
  private val fpsProperty: GraphProperty<String> = propertyGraph.property(DEFAULT_FPS.toString())

  /** The framework's declared default target — reordering the configured list must not change it. */
  private fun defaultTarget(): TargetChoice {
    val default = HaxeTargetOptions.defaultChoice(buildFileType())
    return targets.find { it.id == default.id } ?: targets.first()
  }

  init {
    // follow the project name until the user overrides these fields
    baseData?.nameProperty?.let { nameProperty ->
      titleProperty.dependsOn(nameProperty) { nameProperty.get() }
      packageProperty.dependsOn(nameProperty) { defaultPackage() }
    }
  }

  private fun defaultPackage(): String {
    val name = (baseData?.name ?: "app").lowercase().filter { it.isLetterOrDigit() }.ifEmpty { "app" }
    return "com.example.$name"
  }

  override fun setupUI(builder: Panel) {
    builder.row(HaxeWizardBundle.message("haxe.wizard.target.label")) {
      comboBox(targets).bindItem(targetProperty)
    }
    builder.row(HaxeWizardBundle.message("haxe.wizard.app.title.label")) {
      textField().bindText(titleProperty).columns(24)
    }
    builder.row(HaxeWizardBundle.message("haxe.wizard.app.package.label")) {
      textField().bindText(packageProperty).columns(24)
    }
    builder.row(HaxeWizardBundle.message("haxe.wizard.window.label")) {
      textField().bindText(widthProperty).columns(5)
      label("x")
      textField().bindText(heightProperty).columns(5)
      label(HaxeWizardBundle.message("haxe.wizard.fps.label"))
      textField().bindText(fpsProperty).columns(4)
    }
  }

  override fun setupProject(project: Project) {
    val contentRoot = HaxeTemplateScaffold.createModule(project, this) ?: return
    val width = widthProperty.get().toIntOrNull() ?: DEFAULT_WIDTH
    val height = heightProperty.get().toIntOrNull() ?: DEFAULT_HEIGHT
    val fps = fpsProperty.get().toIntOrNull() ?: DEFAULT_FPS
    val title = titleProperty.get()
    val pkg = packageProperty.get()

    val projectFile = when (flavor) {
      LimeFlavor.NME -> HaxeTemplateFiles.nmmlProjectXml(project, title, pkg, width, height, fps)
      else -> HaxeTemplateFiles.limeProjectXml(project, flavor.haxelib, title, pkg, width, height, fps)
    }
    val mainHx = when (flavor) {
      LimeFlavor.LIME -> HaxeTemplateFiles.limeMainHx(project)
      LimeFlavor.OPENFL -> HaxeTemplateFiles.openflMainHx(project)
      LimeFlavor.NME -> HaxeTemplateFiles.nmeMainHx(project)
    }
    val files = mapOf(
      flavor.buildFileName to projectFile,
      "${HaxeTemplateFiles.SOURCE_DIR}/Main.hx" to mainHx)
    HaxeTemplateScaffold.writeAndRegister(project, contentRoot, files,
                                          buildFileName = flavor.buildFileName,
                                          targetId = targetProperty.get().id)
  }

  private companion object {
    const val DEFAULT_WIDTH = 1280
    const val DEFAULT_HEIGHT = 720
    const val DEFAULT_FPS = 60
  }
}
