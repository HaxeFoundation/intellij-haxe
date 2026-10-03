package com.intellij.plugins.haxe.config.sdk.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.plugins.haxe.HaxeBundle
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * The Haxe SDK's additional settings page: the haxelib tool, then the
 * runtimes builds run on (Neko, HashLink, NodeJS and the Flex/AIR SDK for
 * the flash-family lanes). Values persist in
 * [com.intellij.plugins.haxe.config.sdk.HaxeSdkData]; the configurable moves
 * them through the accessors.
 */
class HaxeAdditionalConfigurablePanel {

  private val haxelibField = executableField(HaxeBundle.message("haxe.build.tools.haxelib.chooser.title"))
  private val nekoField = executableField(HaxeBundle.message("haxe.build.tools.neko.chooser.title"))
  private val hlField = executableField(HaxeBundle.message("haxe.build.tools.hashlink.chooser.title"))
  private val nodeField = executableField(HaxeBundle.message("haxe.build.tools.node.chooser.title"))
  private val flashPlayerField = executableField(HaxeBundle.message("haxe.build.tools.flash.player.chooser.title"))
  private val flexSdkSelector = FlexSdkSelector("")
  private val removeDuplicatesCheckBox = JBCheckBox(HaxeBundle.message("sdk.completion.remove.duplicates"))

  private val mainPanel = panel {
    row(HaxeBundle.message("haxelib.executable")) {
      cell(haxelibField)
        .align(AlignX.FILL)
        .warnUnlessFile()
    }
    group(HaxeBundle.message("sdk.runtimes.section")) {
      row(HaxeBundle.message("neko.executable")) {
        cell(nekoField)
          .align(AlignX.FILL)
          .warnUnlessRunnable("neko")
      }
      row(HaxeBundle.message("hl.executable")) {
        cell(hlField)
          .align(AlignX.FILL)
          .warnUnlessRunnable("hl")
      }
      row(HaxeBundle.message("node.executable")) {
        cell(nodeField)
          .align(AlignX.FILL)
          .warnUnlessRunnable("node")
      }
      row(HaxeBundle.message("flash.player.executable")) {
        cell(flashPlayerField)
          .align(AlignX.FILL)
          .warnUnlessRunnable(null)
      }
      row(HaxeBundle.message("flex.sdk")) {
        cell(flexSdkSelector.getComponent())
      }
    }
    group(HaxeBundle.message("sdk.completion.section")) {
      row {
        cell(removeDuplicatesCheckBox)
          .comment(HaxeBundle.message("sdk.completion.remove.duplicates.comment"))
      }
    }
  }

  fun getPanel(): JComponent = mainPanel

  /** Activates the inline path warnings until [parentDisposable] is disposed. */
  fun registerValidators(parentDisposable: Disposable) = mainPanel.registerValidators(parentDisposable)

  /** Shows what the empty fields inherit (bundled haxelib, PATH-detected runtimes) as grayed empty text. */
  fun setInheritedDefaults(haxelib: String?, neko: String?, hashlink: String?, node: String?) {
    setInheritedDefault(haxelibField, haxelib)
    setInheritedDefault(nekoField, neko)
    setInheritedDefault(hlField, hashlink)
    setInheritedDefault(nodeField, node)
  }

  fun setHaxelibPath(path: String) {
    haxelibField.text = FileUtil.toSystemDependentName(path)
  }

  fun getHaxelibPath(): String = FileUtil.toSystemIndependentName(haxelibField.text.trim())

  fun setNekoBinPath(path: String) {
    nekoField.text = FileUtil.toSystemDependentName(path)
  }

  fun getNekoBinPath(): String = FileUtil.toSystemIndependentName(nekoField.text.trim())

  fun setHlBinPath(path: String) {
    hlField.text = FileUtil.toSystemDependentName(path)
  }

  fun getHlBinPath(): String = FileUtil.toSystemIndependentName(hlField.text.trim())

  fun setNodeBinPath(path: String) {
    nodeField.text = FileUtil.toSystemDependentName(path)
  }

  fun getNodeBinPath(): String = FileUtil.toSystemIndependentName(nodeField.text.trim())

  fun setFlashPlayerPath(path: String) {
    flashPlayerField.text = FileUtil.toSystemDependentName(path)
  }

  fun getFlashPlayerPath(): String = FileUtil.toSystemIndependentName(flashPlayerField.text.trim())

  fun setFlexSdkName(name: String) = flexSdkSelector.setSelectedName(name)

  fun getFlexSdkName(): String = flexSdkSelector.getSelectedName()

  fun setRemoveCompletionDuplicatesFlag(state: Boolean) {
    removeDuplicatesCheckBox.isSelected = state
  }

  fun getRemoveCompletionDuplicatesFlag(): Boolean = removeDuplicatesCheckBox.isSelected
}
