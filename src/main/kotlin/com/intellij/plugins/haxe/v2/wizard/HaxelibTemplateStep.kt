package com.intellij.plugins.haxe.v2.wizard

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.baseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.project.Project
import com.intellij.plugins.haxe.HaxeWizardBundle
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns

/**
 * Haxelib library project: the manifest form covers every field the bundled
 * haxelib.json schema REQUIRES (name, license, version, releasenote,
 * contributors — all defaulted so the generated manifest is always valid)
 * plus the useful optionals; classPath is fixed to the src folder. The
 * library name follows the project name until edited.
 */
internal class HaxelibTemplateStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {

  // the license values the haxelib.json schema accepts
  private val licenses = listOf("MIT", "Apache", "BSD", "GPL", "LGPL", "Public")

  private val nameProperty: GraphProperty<String> = propertyGraph.lazyProperty { baseData?.name ?: "mylib" }
  private val licenseProperty: GraphProperty<String> = propertyGraph.property("MIT")
  private val versionProperty: GraphProperty<String> = propertyGraph.property("0.0.1")
  private val descriptionProperty: GraphProperty<String> = propertyGraph.property("")
  private val urlProperty: GraphProperty<String> = propertyGraph.property("")
  private val tagsProperty: GraphProperty<String> = propertyGraph.property("")
  private val contributorsProperty: GraphProperty<String> = propertyGraph.property("")
  private val releasenoteProperty: GraphProperty<String> =
    propertyGraph.property(HaxeWizardBundle.message("haxe.wizard.haxelib.releasenote.default"))

  init {
    baseData?.nameProperty?.let { projectName ->
      nameProperty.dependsOn(projectName) { projectName.get() }
    }
  }

  override fun setupUI(builder: Panel) {
    builder.row(HaxeWizardBundle.message("haxe.wizard.haxelib.name.label")) {
      textField().bindText(nameProperty).columns(24)
    }
    builder.row(HaxeWizardBundle.message("haxe.wizard.haxelib.license.label")) {
      comboBox(licenses).bindItem(licenseProperty)
      label(HaxeWizardBundle.message("haxe.wizard.haxelib.version.label"))
      textField().bindText(versionProperty).columns(8)
    }
    builder.row(HaxeWizardBundle.message("haxe.wizard.haxelib.description.label")) {
      textField().bindText(descriptionProperty).columns(36)
    }
    builder.row(HaxeWizardBundle.message("haxe.wizard.haxelib.url.label")) {
      textField().bindText(urlProperty).columns(36)
    }
    builder.row(HaxeWizardBundle.message("haxe.wizard.haxelib.tags.label")) {
      textField().bindText(tagsProperty).columns(24)
        .comment(HaxeWizardBundle.message("haxe.wizard.haxelib.list.comment"))
    }
    builder.row(HaxeWizardBundle.message("haxe.wizard.haxelib.contributors.label")) {
      textField().bindText(contributorsProperty).columns(24)
        .comment(HaxeWizardBundle.message("haxe.wizard.haxelib.list.comment"))
    }
    builder.row(HaxeWizardBundle.message("haxe.wizard.haxelib.releasenote.label")) {
      textField().bindText(releasenoteProperty).columns(36)
    }
  }

  override fun setupProject(project: Project) {
    val contentRoot = HaxeTemplateScaffold.createModule(project, this) ?: return
    val libName = nameProperty.get().ifBlank { "mylib" }
    val (className, classContent) = HaxeTemplateFiles.haxelibStarterClass(project, libName)
    val manifest = HaxeTemplateFiles.haxelibJson(
      project = project,
      name = libName,
      license = licenseProperty.get(),
      version = versionProperty.get().ifBlank { "0.0.1" },
      description = descriptionProperty.get(),
      url = urlProperty.get(),
      tags = splitList(tagsProperty.get()),
      contributors = splitList(contributorsProperty.get()),
      releasenote = releasenoteProperty.get())

    val files = mapOf(
      "haxelib.json" to manifest,
      "${HaxeTemplateFiles.SOURCE_DIR}/$className.hx" to classContent,
      "dev.hxml" to HaxeTemplateFiles.haxelibDevHxml(project, className))
    // dev.hxml as the active build file gives the library compiler
    // completion and diagnostics from the start
    HaxeTemplateScaffold.writeAndRegister(project, contentRoot, files, buildFileName = "dev.hxml")
  }

  private fun splitList(raw: String): List<String> =
    raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
}
