package com.intellij.plugins.haxe.v2.migration

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.readAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.ui.Messages
import com.intellij.plugins.haxe.HaxeProjectBundle
import com.intellij.plugins.haxe.ide.module.HaxeModuleType

/**
 * Detects legacy (V1) HAXE_MODULE modules on project open and offers the V2
 * conversion. V1 is no longer supported — the sticky notification says so and
 * insists on a backup before converting.
 */
class HaxeV1MigrationStartup : ProjectActivity {

  override suspend fun execute(project: Project) {
    val legacyModules = readAction {
      ModuleManager.getInstance(project).modules
        .filter { ModuleType.get(it) === HaxeModuleType.getInstance() }
        .map { it.name }
    }
    if (legacyModules.isEmpty()) return

    NotificationGroupManager.getInstance()
      .getNotificationGroup(HaxeV1Migrator.NOTIFICATION_GROUP_ID)
      .createNotification(
        HaxeProjectBundle.message("haxe.v1.migration.title"),
        HaxeProjectBundle.message("haxe.v1.migration.content", legacyModules.joinToString(", ")),
        NotificationType.WARNING)
      .addAction(ConvertAction(project, legacyModules.size))
      .notify(project)
  }

  private class ConvertAction(private val project: Project, private val moduleCount: Int)
    : NotificationAction(HaxeProjectBundle.message("haxe.v1.migration.convert")) {

    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
      val answer = Messages.showYesNoDialog(
        project,
        HaxeProjectBundle.message("haxe.v1.migration.confirm.message", moduleCount),
        HaxeProjectBundle.message("haxe.v1.migration.confirm.title"),
        HaxeProjectBundle.message("haxe.v1.migration.confirm.yes"),
        Messages.getCancelButton(),
        Messages.getWarningIcon())
      if (answer != Messages.YES) return
      notification.expire()
      HaxeV1Migrator.getInstance(project).convertAsync()
    }
  }
}
