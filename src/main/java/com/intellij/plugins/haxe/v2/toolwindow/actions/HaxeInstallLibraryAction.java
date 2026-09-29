package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCommandNotifications;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxelibInstaller;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.LibraryNode;
import org.jetbrains.annotations.NotNull;

/// Tree context menu on a missing library: runs `haxelib install <name> [version] --always`
/// in the background and refreshes the tree on success.
public final class HaxeInstallLibraryAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeInstallLibraryAction(@NotNull HaxeToolWindowPanel panel) {
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project != null && panel.getSelectedUserObject() instanceof LibraryNode library && !library.installed()) {
      installInBackground(project, library);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    if (panel.getSelectedUserObject() instanceof LibraryNode library && !library.installed()) {
      e.getPresentation().setEnabledAndVisible(true);
      e.getPresentation().setText(HaxeBundle.message("haxe.toolwindow.install.library", library.name()));
    }
    else {
      e.getPresentation().setEnabledAndVisible(false);
    }
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }

  private void installInBackground(@NotNull Project project, @NotNull LibraryNode library) {
    new Task.Backgroundable(project, HaxeBundle.message("haxe.toolwindow.install.library.progress", library.name()), true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        String failure = HaxelibInstaller.install(project, library.name(), library.version(), library.resolvedVersion());
        if (failure == null) {
          String installed = HaxeBundle.message("haxe.toolwindow.install.library.success", library.name());
          HaxeCommandNotifications.notify(project, installed, NotificationType.INFORMATION);
          HaxeLibrarySync.sync(project, panel::refreshTree);
        }
        else {
          String title = HaxeBundle.message("haxe.toolwindow.install.library.failed", library.name());
          HaxeCommandNotifications.notify(project, title, failure, NotificationType.ERROR);
        }
      }
    }.queue();
  }
}
