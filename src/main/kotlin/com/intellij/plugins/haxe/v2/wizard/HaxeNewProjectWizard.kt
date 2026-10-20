package com.intellij.plugins.haxe.v2.wizard

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardChainStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep.Companion.ADD_SAMPLE_CODE_PROPERTY_NAME
import com.intellij.ide.wizard.language.LanguageGeneratorNewProjectWizard
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.project.Project
import com.intellij.plugins.haxe.HaxeWizardBundle
import com.intellij.ui.UIBundle
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected
import icons.HaxeIcons
import javax.swing.Icon

/**
 * "Haxe" entry in the New Project / New Module wizard (v2). Creates a plain
 * general-type module with a src root; with "Add sample code" checked (the
 * default) also a starter Main.hx and a build.hxml that is registered as the
 * module's active build file.
 */
class HaxeNewProjectWizard : LanguageGeneratorNewProjectWizard {

  override val name: String = "Haxe"

  override val icon: Icon
    get() = HaxeIcons.HAXE_LOGO

  override val ordinal: Int = 600

  override fun createStep(parent: NewProjectWizardStep): NewProjectWizardStep =
    NewProjectWizardChainStep(HaxeProjectSdkStep(parent)).nextStep(::Step)

  private class Step(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {

    // the platform-wide persisted "Add sample code" toggle other wizards share
    private val addSampleCodeProperty = propertyGraph.property(true)
      .bindBooleanStorage(ADD_SAMPLE_CODE_PROPERTY_NAME)

    override fun setupUI(builder: Panel) {
      builder.row {
        checkBox(UIBundle.message("label.project.wizard.new.project.add.sample.code"))
          .bindSelected(addSampleCodeProperty)
          .comment(HaxeWizardBundle.message("haxe.wizard.add.sample.comment"))
      }
    }

    override fun setupProject(project: Project) {
      val contentRoot = HaxeTemplateScaffold.createModule(project, this) ?: return
      if (addSampleCodeProperty.get()) {
        val files = mapOf(
          "${HaxeModuleBuilder.SOURCE_DIR}/Main.hx" to HaxeTemplateFiles.starterMainHx(project, "Main"),
          BUILD_FILE_NAME to BUILD_HXML)
        HaxeTemplateScaffold.writeAndRegister(project, contentRoot, files, buildFileName = BUILD_FILE_NAME)
      }
    }
  }

  private companion object {
    const val BUILD_FILE_NAME = "build.hxml"

    val BUILD_HXML = """
      -cp ${HaxeModuleBuilder.SOURCE_DIR}
      -main Main
      --interp
    """.trimIndent() + "\n"
  }
}
