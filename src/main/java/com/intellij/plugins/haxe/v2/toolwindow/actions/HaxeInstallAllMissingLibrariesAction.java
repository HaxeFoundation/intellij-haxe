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

import java.util.ArrayList;
import java.util.List;

/**
 * Tree context menu on a Libraries group with missing entries: installs every
 * missing library sequentially, then syncs and refreshes once.
 */
public final class HaxeInstallAllMissingLibrariesAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeInstallAllMissingLibrariesAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.install.all.missing"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    List<LibraryNode> missing = panel.getSelectedGroupMissingLibraries();
    if (project != null && !missing.isEmpty()) {
      installAllInBackground(project, missing);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(!panel.getSelectedGroupMissingLibraries().isEmpty());
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }

  private void installAllInBackground(@NotNull Project project, @NotNull List<LibraryNode> missing) {
    new Task.Backgroundable(project, HaxeBundle.message("haxe.toolwindow.install.all.missing.progress"), true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        List<String> failed = new ArrayList<>();
        for (LibraryNode library : missing) {
          indicator.checkCanceled();
          indicator.setText(HaxeBundle.message("haxe.toolwindow.install.library.progress", library.name()));
          String failure = HaxelibInstaller.install(project, library.name(), library.version(), library.resolvedVersion());
          if (failure != null) {
            failed.add(library.name());
            String title = HaxeBundle.message("haxe.toolwindow.install.library.failed", library.name());
            HaxeCommandNotifications.notify(project, title, failure, NotificationType.ERROR);
          }
        }

        int installed = missing.size() - failed.size();
        if (installed > 0) {
          String summary = HaxeBundle.message("haxe.toolwindow.install.all.missing.success", installed);
          HaxeCommandNotifications.notify(project, summary, NotificationType.INFORMATION);
        }
        if (!failed.isEmpty()) {
          String summary = HaxeBundle.message("haxe.toolwindow.install.all.missing.failed", String.join(", ", failed));
          HaxeCommandNotifications.notify(project, summary, NotificationType.ERROR);
        }
        HaxeLibrarySync.sync(project, panel::refreshTree);
      }
    }.queue();
  }
}
