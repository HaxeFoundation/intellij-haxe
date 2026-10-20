package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomToolsStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ToolNode;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu on a custom tool row: removes it. Detected tools have no
 * stored entry, so there is nothing to remove for them.
 */
public final class HaxeRemoveToolAction extends HaxeSelectedNodeAction<ToolNode> {

  public HaxeRemoveToolAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, ToolNode.class, HaxeBundle.message("haxe.toolwindow.remove.tool"));
  }

  @Override
  protected boolean applies(@NotNull ToolNode toolNode) {
    return toolNode.custom();
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull ToolNode toolNode) {
    HaxeCustomToolsStore.getInstance(project).removeTool(toolNode.containerId(), toolNode.name());
  }
}
