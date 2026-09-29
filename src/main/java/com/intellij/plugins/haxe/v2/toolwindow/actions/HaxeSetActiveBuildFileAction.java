package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeActiveBuildFileStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.BuildFileRow;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu: marks the selected build file as the project's active
 * configuration. There is exactly one per project - the IDE keeps one parse tree
 * per file, so conditional compilation follows a single build configuration.
 */
public final class HaxeSetActiveBuildFileAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeSetActiveBuildFileAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.set.active.build.file"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project != null && panel.getSelectedUserObject() instanceof BuildFileRow row && !row.active()) {
      HaxeActiveBuildFileStore.getInstance(project).setActiveFile(row.buildFile().file().getPath());
      // module libraries follow the active file - detach the previous file's set
      HaxeLibrarySync.sync(project, panel::refreshTree);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    boolean applicable = panel.getSelectedUserObject() instanceof BuildFileRow row && !row.active();
    e.getPresentation().setEnabledAndVisible(applicable);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
