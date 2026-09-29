package com.intellij.plugins.haxe.v2.wizard

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.GeneratorNewProjectWizard
import com.intellij.ide.wizard.GeneratorNewProjectWizardBuilderAdapter
import com.intellij.ide.wizard.GitNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseStep
import com.intellij.ide.wizard.NewProjectWizardChainStep.Companion.nextStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.RootNewProjectWizardStep
import com.intellij.plugins.haxe.HaxeWizardBundle
import icons.HaxeIcons
import javax.swing.Icon

/**
 * The standalone "Haxe Template" entry in the New Project dialog's generator
 * list — named to stay distinct from the "Haxe" language option inside the
 * generic New Project entry: one page with SDK selection and a project
 * TEMPLATE switcher (Empty, HXML, Lime, OpenFL, NME, Haxelib), each template
 * generating configured files and wiring the v2 stores. The language-dropdown
 * entry ({@code HaxeNewProjectWizard}) is a separate, simpler surface.
 */
class HaxeProjectGenerator : GeneratorNewProjectWizard {

  override val id: String = "haxe.project.templates"

  override val name: String
    get() = HaxeWizardBundle.message("haxe.wizard.generator.name")

  override val icon: Icon
    get() = HaxeIcons.HAXE_LOGO

  override val ordinal: Int = 600

  override fun createStep(context: WizardContext): NewProjectWizardStep =
    RootNewProjectWizardStep(context)
      .nextStep(::NewProjectWizardBaseStep)
      .nextStep(::GitNewProjectWizardStep)
      .nextStep(::HaxeProjectSdkStep)
      .nextStep(::HaxeTemplateStep)
}

/** The ModuleBuilder registration shell the New Project dialog picks up. */
class HaxeProjectGeneratorBuilder : GeneratorNewProjectWizardBuilderAdapter(HaxeProjectGenerator())
