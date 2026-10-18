package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildCaches;
import org.jetbrains.annotations.NotNull;

/** Tool window button that drops every cached value ({@link HaxeBuildCaches#purge}); the tree follows the clear. */
public final class HaxePurgeCachesAction extends DumbAwareAction {

  public HaxePurgeCachesAction() {
    super(HaxeBundle.message("haxe.toolwindow.purge.caches.text"),
          HaxeBundle.message("haxe.toolwindow.purge.caches.description"),
          AllIcons.Actions.GC);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;
    HaxeBuildCaches.purge(project);
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
