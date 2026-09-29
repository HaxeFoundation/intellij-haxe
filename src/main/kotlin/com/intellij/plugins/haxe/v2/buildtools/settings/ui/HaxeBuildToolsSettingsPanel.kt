package com.intellij.plugins.haxe.v2.buildtools.settings.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.plugins.haxe.HaxeBundle
import com.intellij.plugins.haxe.config.sdk.ui.FlexSdkSelector
import com.intellij.plugins.haxe.config.sdk.ui.executableField
import com.intellij.plugins.haxe.config.sdk.ui.setInheritedDefault
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver.InheritedRuntimeDefaults
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.CollapsibleRow
import com.intellij.ui.dsl.builder.panel
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

/**
 * Swing panel for Settings | Build Tools | Haxe: SDK selection, the haxelib
 * tool, per-project runtime overrides (over the SDK's runtimes), the
 * compilation server and test options. Free of project service access; the
 * configurable supplies the available SDK names and reads the edited values
 * back.
 */
class HaxeBuildToolsSettingsPanel {

  private val sdkCombo = ComboBox<String>()
  private val haxelibField = executableField(HaxeBundle.message("haxe.build.tools.haxelib.chooser.title"))
  private val nekoField = executableField(HaxeBundle.message("haxe.build.tools.neko.chooser.title"))
  private val hashlinkField = executableField(HaxeBundle.message("haxe.build.tools.hashlink.chooser.title"))
  private val nodeField = executableField(HaxeBundle.message("haxe.build.tools.node.chooser.title"))
  private val flashPlayerField = executableField(HaxeBundle.message("haxe.build.tools.flash.player.chooser.title"))
  private val flexSdkSelector = FlexSdkSelector(HaxeBundle.message("haxe.build.tools.flex.sdk.auto"))
  private val serverEnabledCheckBox = JBCheckBox(HaxeBundle.message("haxe.build.tools.server.enabled"))
  private val serverPortField = JBTextField()
  private val serverArgumentsField = JBTextField()
  private val liveTestReportingCheckBox = JBCheckBox(HaxeBundle.message("haxe.build.tools.live.test.reporting"))

  private val knownSdkNames = LinkedHashSet<String>()
  private lateinit var runtimeOverridesGroup: CollapsibleRow
  private val mainPanel: JComponent

  init {
    sdkCombo.renderer = sdkComboRenderer(knownSdkNames)
    serverEnabledCheckBox.addActionListener { updateServerFieldsEnabled() }

    mainPanel = panel {
      row(HaxeBundle.message("haxe.build.tools.sdk")) {
        cell(sdkCombo)
      }
      row(HaxeBundle.message("haxe.build.tools.haxelib.path")) {
        cell(haxelibField)
          .align(AlignX.FILL)
          .comment(HaxeBundle.message("haxe.build.tools.path.hint"))
      }
      runtimeOverridesGroup = collapsibleGroup(HaxeBundle.message("haxe.build.tools.runtime.overrides")) {
        row(HaxeBundle.message("haxe.build.tools.neko.path")) {
          cell(nekoField).align(AlignX.FILL)
        }
        row(HaxeBundle.message("haxe.build.tools.hashlink.path")) {
          cell(hashlinkField).align(AlignX.FILL)
        }
        row(HaxeBundle.message("haxe.build.tools.node.path")) {
          cell(nodeField).align(AlignX.FILL)
        }
        row(HaxeBundle.message("haxe.build.tools.flash.player.path")) {
          cell(flashPlayerField).align(AlignX.FILL)
        }
        row(HaxeBundle.message("haxe.build.tools.flex.sdk")) {
          cell(flexSdkSelector.getComponent())
        }
        row {
          comment(HaxeBundle.message("haxe.build.tools.runtime.overrides.hint"))
        }
      }
      group(HaxeBundle.message("haxe.build.tools.server.section")) {
        row {
          cell(serverEnabledCheckBox)
        }
        row(HaxeBundle.message("haxe.build.tools.server.port")) {
          cell(serverPortField).align(AlignX.FILL)
        }
        row(HaxeBundle.message("haxe.build.tools.server.arguments")) {
          cell(serverArgumentsField)
            .align(AlignX.FILL)
            .comment(HaxeBundle.message("haxe.build.tools.server.hint"))
        }
      }
      group(HaxeBundle.message("haxe.build.tools.tests.section")) {
        row {
          cell(liveTestReportingCheckBox)
            .comment(HaxeBundle.message("haxe.build.tools.live.test.reporting.hint"))
        }
      }
    }
  }

  private fun updateServerFieldsEnabled() {
    serverPortField.isEnabled = serverEnabledCheckBox.isSelected
    serverArgumentsField.isEnabled = serverEnabledCheckBox.isSelected
  }

  fun getComponent(): JComponent = mainPanel

  /** Fires on every SDK selection change (including programmatic resets); recompute the inherited defaults there. */
  fun addSdkSelectionListener(listener: Runnable) {
    sdkCombo.addActionListener { listener.run() }
  }

  /** Shows what the empty overrides inherit from the selected SDK as grayed empty text. */
  fun setInheritedDefaults(defaults: InheritedRuntimeDefaults) {
    setInheritedDefault(haxelibField, defaults.haxelib)
    setInheritedDefault(nekoField, defaults.neko)
    setInheritedDefault(hashlinkField, defaults.hashlink)
    setInheritedDefault(nodeField, defaults.node)
    setInheritedDefault(flashPlayerField, defaults.flashPlayer)

    val emptyText = defaults.flexSdkName
                      ?.let { HaxeBundle.message("haxe.build.tools.flex.sdk.inherited", it) }
                    ?: HaxeBundle.message("haxe.build.tools.flex.sdk.auto")
    flexSdkSelector.setEmptyText(emptyText)
  }

  fun reset(availableSdkNames: Set<String>, selectedSdkName: String?, haxelibPath: String) {
    knownSdkNames.clear()
    knownSdkNames.addAll(availableSdkNames)

    val model = DefaultComboBoxModel<String>()
    model.addElement(null)
    availableSdkNames.forEach(model::addElement)
    // Keep a stored-but-missing SDK selectable so applying without touching it does not silently drop it.
    if (selectedSdkName != null && !availableSdkNames.contains(selectedSdkName)) {
      model.addElement(selectedSdkName)
    }
    sdkCombo.model = model
    sdkCombo.selectedItem = selectedSdkName

    haxelibField.text = haxelibPath
  }

  fun resetRuntimeOverrides(nekoPath: String,
                            hashlinkPath: String,
                            nodePath: String,
                            flashPlayerPath: String,
                            flexSdkName: String) {
    nekoField.text = nekoPath
    hashlinkField.text = hashlinkPath
    nodeField.text = nodePath
    flashPlayerField.text = flashPlayerPath
    flexSdkSelector.setSelectedName(flexSdkName)

    val anyOverride = nekoPath.isNotBlank() || hashlinkPath.isNotBlank() || nodePath.isNotBlank()
                      || flashPlayerPath.isNotBlank() || flexSdkName.isNotBlank()
    runtimeOverridesGroup.expanded = anyOverride
  }

  fun resetServerFields(enabled: Boolean, port: Int, arguments: String) {
    serverEnabledCheckBox.isSelected = enabled
    serverPortField.text = if (port > 0) port.toString() else ""
    serverArgumentsField.text = arguments
    updateServerFieldsEnabled()
  }

  fun resetLiveTestReporting(enabled: Boolean) {
    liveTestReportingCheckBox.isSelected = enabled
  }

  fun isLiveTestReporting(): Boolean = liveTestReportingCheckBox.isSelected
  fun isServerEnabled(): Boolean = serverEnabledCheckBox.isSelected

  /** 0 when blank or unparsable (= pick a free port automatically). */
  fun getServerPort(): Int = serverPortField.text.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0

  fun getServerArguments(): String = serverArgumentsField.text.trim()
  fun getSelectedSdkName(): String? = sdkCombo.selectedItem as String?
  fun getHaxelibPath(): String = haxelibField.text.trim()
  fun getNekoPath(): String = nekoField.text.trim()
  fun getHashlinkPath(): String = hashlinkField.text.trim()
  fun getNodePath(): String = nodeField.text.trim()
  fun getFlashPlayerPath(): String = flashPlayerField.text.trim()
  fun getFlexSdkName(): String = flexSdkSelector.getSelectedName()
}
