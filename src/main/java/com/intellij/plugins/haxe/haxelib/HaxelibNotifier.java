package com.intellij.plugins.haxe.haxelib;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.Nullable;

public class HaxelibNotifier {

  public static void notifyMissingLib(@Nullable Project project, String content) {
    NotificationGroupManager.getInstance()
      .getNotificationGroup("haxe.haxelib.warning")
      .createNotification(content, NotificationType.WARNING)
      .setTitle(HaxeBundle.message("haxe.haxelib.library.dependencies"))
      .setContent(HaxeBundle.message("haxe.haxelib.library.missing", content))
      //.addAction(new DumbAwareAction() {
      //  //TODO create haxelib install logic
      //  @Override
      //  public void actionPerformed(@NotNull AnActionEvent e) {
      //
      //  }
      //})
      .notify(project);
  }
}
