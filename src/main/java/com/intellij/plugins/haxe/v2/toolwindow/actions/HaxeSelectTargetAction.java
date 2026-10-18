package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowEditors;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.TargetNode;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu: opens the target dropdown for a selectable target row
 * (XML/HXP based projects; HXML targets are fixed by the file).
 */
public final class HaxeSelectTargetAction extends HaxeSelectedNodeAction<TargetNode> {

  public HaxeSelectTargetAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, TargetNode.class, HaxeBundle.message("haxe.toolwindow.select.target.title"));
  }

  @Override
  protected boolean applies(@NotNull TargetNode targetNode) {
    return targetNode.selectable();
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull TargetNode targetNode) {
    HaxeToolWindowEditors.showTargetPopup(project, targetNode, panel.getSelectionPopupPoint());
  }
}
