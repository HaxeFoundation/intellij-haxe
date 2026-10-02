package com.intellij.plugins.haxe.v2.migration

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys

/**
 * Project view entry for the V2 conversion, for when the startup notification
 * was dismissed. Shown on a module that still uses the legacy (V1) type, and
 * converts every legacy module of the project, as the notification does.
 */
class HaxeConvertV1ModulesAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    val module = e.getData(LangDataKeys.MODULE_CONTEXT)
    e.presentation.isEnabledAndVisible = module != null && HaxeV1Migrator.isLegacy(module)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    HaxeV1Migrator.getInstance(project).confirmAndConvert()
  }
}
