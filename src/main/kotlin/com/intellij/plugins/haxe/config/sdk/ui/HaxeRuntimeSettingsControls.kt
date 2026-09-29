@file:JvmName("HaxeRuntimeSettingsControls")

package com.intellij.plugins.haxe.config.sdk.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.projectRoots.ui.ProjectJdksEditor
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.plugins.haxe.HaxeBundle
import com.intellij.plugins.haxe.HaxeDebuggerBundle
import com.intellij.ui.JBColor
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.listCellRenderer.listCellRenderer
import java.util.Locale
import javax.swing.DefaultComboBoxModel
import org.jetbrains.annotations.Nls

/**
 * Controls shared by the runtime-configuring settings panels (the Haxe SDK's
 * additional data page and Build Tools | Haxe).
 */

fun executableField(@Nls chooserTitle: String): TextFieldWithBrowseButton {
  // an ExtendableTextField, so the field can show the inherited value as grayed empty text
  val field = TextFieldWithBrowseButton(ExtendableTextField())
  val descriptor = FileChooserDescriptorFactory.singleFile().withTitle(chooserTitle)
  field.addBrowseFolderListener(TextBrowseFolderListener(descriptor))
  return field
}

/** Shows what an empty field inherits as grayed empty text; null clears it. */
fun setInheritedDefault(field: TextFieldWithBrowseButton, inherited: String?) {
  val textField = field.textField as? ExtendableTextField ?: return
  textField.emptyText.text = inherited?.let(FileUtil::toSystemDependentName) ?: ""
}

/**
 * A Flex/AIR SDK selector: entries come from the IDE's SDK table, matched by
 * type name so this control never depends on the (optional) Flash plugin's
 * classes. Entries render with their SDK type's icon; a stored name whose
 * entry is gone stays visible in RED instead of being silently dropped. The
 * null first item means "not set" and renders as [emptyText]; without any
 * matching entry the combo is disabled with a tooltip explaining where the
 * entries come from. The browse button opens the platform SDK editor, so an
 * entry can be added or fixed in place — the list reloads when it closes.
 */
class FlexSdkSelector(@Nls emptyText: String) {
  private val combo = ComboBox<String?>()
  private val component = ComponentWithBrowseButton(combo, null)

  /** With the run-configuration editors' shared placeholder: the Flex SDK inherited from the Haxe SDK/runtimes chain. */
  constructor() : this(HaxeDebuggerBundle.message("flash.runner.editor.flex.sdk.from.haxe.sdk"))

  init {
    setEmptyText(emptyText)
    reload(null)
    component.addActionListener { openSdkEditor() }
  }

  fun getComponent(): ComponentWithBrowseButton<ComboBox<String?>> = component

  /** What the "not set" item reads as (panels showing an inherited value update it live). */
  fun setEmptyText(@Nls emptyText: String) {
    combo.renderer = listCellRenderer(emptyText) {
      // the platform overload renders emptyText for the null item itself - the block only sees real names
      val sdk = ProjectJdkTable.getInstance().findJdk(value)
      if (sdk == null) {
        text(value) { foreground = JBColor.RED }
      } else {
        (sdk.sdkType as? SdkType)?.icon?.let { icon(it) }
        text(value)
      }
    }
  }

  /** Selects the stored name (reloading the entries first, so late-added SDKs appear); empty means "not set". */
  fun setSelectedName(name: String) {
    reload(name.ifEmpty { null })
  }

  fun getSelectedName(): String = (combo.selectedItem as String?) ?: ""

  private fun reload(select: String?) {
    val model = DefaultComboBoxModel<String?>()
    model.addElement(null)
    for (sdk in flexSdks()) {
      model.addElement(sdk.name)
    }
    if (select != null && model.getIndexOf(select) < 0) {
      model.addElement(select) // renders red: the entry is gone
    }
    combo.model = model
    combo.selectedItem = select
    val hasEntries = model.size > 1
    combo.isEnabled = hasEntries
    combo.toolTipText = if (hasEntries) null else HaxeBundle.message("flex.sdk.none")
  }

  private fun flexSdks() = ProjectJdkTable.getInstance().allJdks.filter(::isFlexSdk)

  private fun isFlexSdk(sdk: Sdk) = sdk.sdkType.name.lowercase(Locale.ROOT).contains("flex")

  private fun openSdkEditor() {
    // any project serves as the editor's context - the SDK table is
    // application-level; the default project covers the welcome screen
    val project = ProjectManager.getInstance().openProjects.firstOrNull()
                  ?: ProjectManager.getInstance().defaultProject
    val current = getSelectedName().ifEmpty { null }?.let { ProjectJdkTable.getInstance().findJdk(it) }
    val editor = ProjectJdksEditor(current, project, component)
    if (editor.showAndGet()) {
      val chosen = editor.selectedJdk?.takeIf(::isFlexSdk)
      reload(chosen?.name ?: getSelectedName().ifEmpty { null })
    }
  }
}
