package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetSelectionStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.TargetNode;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu: makes the target shown on a selectable target row the
 * project's default for its build file, which everyone starts with until they
 * pick another one.
 */
public final class HaxeUseTargetAsProjectDefaultAction extends HaxeSelectedNodeAction<TargetNode> {

  public HaxeUseTargetAsProjectDefaultAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, TargetNode.class, HaxeBundle.message("haxe.toolwindow.use.as.project.default"));
  }

  @Override
  protected boolean applies(@NotNull TargetNode targetNode) {
    return targetNode.selectable() && !targetNode.projectDefault();
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull TargetNode targetNode) {
    // a target's id is its configured name, which the row shows
    HaxeTargetSelectionStore.getInstance(project).setProjectDefault(targetNode.buildFile().file(), targetNode.displayName());
  }
}
