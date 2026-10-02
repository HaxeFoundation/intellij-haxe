package com.intellij.plugins.haxe.v2.migration

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.plugins.haxe.HaxeProjectBundle

/**
 * Detects legacy (V1) HAXE_MODULE modules on project open and offers the V2
 * conversion; [HaxeConvertV1ModulesAction] offers the same from the Project
 * view. V1 is no longer supported — the sticky notification says so and
 * insists on a backup before converting.
 */
class HaxeV1MigrationStartup : ProjectActivity {

  override suspend fun execute(project: Project) {
    val legacyModules = readAction { HaxeV1Migrator.getInstance(project).legacyModuleNames() }
    if (legacyModules.isEmpty()) return

    NotificationGroupManager.getInstance()
      .getNotificationGroup(HaxeV1Migrator.NOTIFICATION_GROUP_ID)
      .createNotification(
        HaxeProjectBundle.message("haxe.v1.migration.title"),
        HaxeProjectBundle.message("haxe.v1.migration.content", legacyModules.joinToString(", ")),
        NotificationType.WARNING)
      .addAction(ConvertAction(project))
      .notify(project)
  }

  private class ConvertAction(private val project: Project)
    : NotificationAction(HaxeProjectBundle.message("haxe.v1.migration.convert")) {

    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
      if (HaxeV1Migrator.getInstance(project).confirmAndConvert()) notification.expire()
    }
  }
}
