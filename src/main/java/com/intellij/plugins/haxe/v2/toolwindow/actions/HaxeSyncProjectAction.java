package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.plugins.haxe.v2.buildtools.HaxeProjectSync;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;

/**
 * Tool window "sync" button: runs the v2 sync pipeline (cache refresh,
 * library sync); the tree follows the sync's end.
 */
public final class HaxeSyncProjectAction extends DumbAwareAction {

  public HaxeSyncProjectAction() {
    super(HaxeBundle.message("haxe.toolwindow.sync.text"),
          HaxeBundle.message("haxe.toolwindow.sync.description"),
          AllIcons.Actions.Refresh);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    if (e.getProject() != null) {
      HaxeProjectSync.sync(e.getProject(), null);
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
