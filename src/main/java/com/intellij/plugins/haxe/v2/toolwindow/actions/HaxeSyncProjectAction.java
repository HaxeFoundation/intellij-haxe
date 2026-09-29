package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.plugins.haxe.v2.buildtools.HaxeProjectSync;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;

/**
 * Tool window "sync" button: runs the v2 sync pipeline (library sync, cache
 * refresh, config broadcast) and refreshes the tool window tree afterwards.
 */
public final class HaxeSyncProjectAction extends DumbAwareAction {

  private final Runnable afterSync;

  public HaxeSyncProjectAction(@NotNull Runnable afterSync) {
    super(HaxeBundle.message("haxe.toolwindow.sync.text"),
          HaxeBundle.message("haxe.toolwindow.sync.description"),
          AllIcons.Actions.Refresh);
    this.afterSync = afterSync;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    if (e.getProject() != null) {
      HaxeProjectSync.sync(e.getProject(), afterSync);
    }
    else {
      afterSync.run();
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(e.getProject() != null);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }
}
