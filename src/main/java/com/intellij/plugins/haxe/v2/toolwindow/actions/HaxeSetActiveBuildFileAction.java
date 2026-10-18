package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeActiveBuildFileStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.BuildFileRow;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu: marks the selected build file as the project's active
 * configuration. There is exactly one per project - the IDE keeps one parse tree
 * per file, so conditional compilation follows a single build configuration.
 */
public final class HaxeSetActiveBuildFileAction extends HaxeSelectedNodeAction<BuildFileRow> {

  public HaxeSetActiveBuildFileAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, BuildFileRow.class, HaxeBundle.message("haxe.toolwindow.set.active.build.file"));
  }

  @Override
  protected boolean applies(@NotNull BuildFileRow row) {
    return !row.active();
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull BuildFileRow row) {
    HaxeActiveBuildFileStore.getInstance(project).setActiveFile(row.buildFile().file().getPath());
  }
}
