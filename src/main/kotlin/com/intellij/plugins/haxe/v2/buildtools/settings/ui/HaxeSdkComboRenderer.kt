@file:JvmName("HaxeSdkComboRenderer")

package com.intellij.plugins.haxe.v2.buildtools.settings.ui

import com.intellij.plugins.haxe.HaxeBundle
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.listCellRenderer.listCellRenderer
import javax.swing.ListCellRenderer

/**
 * Renderer for SDK-name combos: null renders the no-SDK placeholder, a name
 * absent from [knownSdkNames] renders red (settings reference an SDK that is
 * no longer configured). The set is read live, so entries added after the
 * combo is built are honored.
 */
fun sdkComboRenderer(knownSdkNames: Set<String>): ListCellRenderer<String?> =
  listCellRenderer(HaxeBundle.message("haxe.build.tools.no.sdk")) {
    val sdkName = value
    text(sdkName) {
      if (sdkName !in knownSdkNames) foreground = JBColor.RED
    }
  }
