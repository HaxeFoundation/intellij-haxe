package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Balloon notifications for haxe tool commands - the one home of the
 * {@code haxe.command} notification group registered in plugin.xml.
 */
public final class HaxeCommandNotifications {

  private static final String GROUP_ID = "haxe.command";

  private HaxeCommandNotifications() {
  }

  public static void notify(@NotNull Project project, @NotNull String title, @NotNull String content,
                            @NotNull NotificationType type) {
    group().createNotification(title, content, type).notify(project);
  }

  /** Title-less balloon: the message is the whole notification. */
  public static void notify(@NotNull Project project, @NotNull String content, @NotNull NotificationType type) {
    group().createNotification(content, type).notify(project);
  }

  /** Balloon carrying one action button (e.g. an install fix). */
  public static void notify(@NotNull Project project, @NotNull String title, @NotNull String content,
                            @NotNull NotificationType type, @NotNull AnAction action) {
    group().createNotification(title, content, type)
      .addAction(action)
      .notify(project);
  }

  @NotNull
  private static NotificationGroup group() {
    return NotificationGroupManager.getInstance().getNotificationGroup(GROUP_ID);
  }
}
