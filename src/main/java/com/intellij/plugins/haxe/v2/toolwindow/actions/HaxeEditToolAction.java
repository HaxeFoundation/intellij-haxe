package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomToolsStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ToolNode;
import com.intellij.plugins.haxe.v2.toolwindow.ui.HaxeCustomActionDialog;
import org.jetbrains.annotations.NotNull;

/** Tree context menu on a custom tool row: edits its name and command. */
public final class HaxeEditToolAction extends HaxeSelectedNodeAction<ToolNode> {

  public HaxeEditToolAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, ToolNode.class, HaxeBundle.message("haxe.toolwindow.edit.tool"));
  }

  @Override
  protected boolean applies(@NotNull ToolNode toolNode) {
    return toolNode.custom();
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull ToolNode toolNode) {
    // edit the STORED values - the node's are the resolved display forms
    HaxeCustomToolsStore.CustomTool stored = HaxeCustomToolsStore.getInstance(project).findTool(toolNode.containerId(), toolNode.name());
    if (stored == null) stored = new HaxeCustomToolsStore.CustomTool(toolNode.name(), toolNode.detail(), "");
    var initial = new HaxeCustomActionDialog.EditedCommand(stored.name(), stored.command(), stored.workDirectory());
    HaxeCustomActionDialog dialog = HaxeCustomActionDialog.forTool(project, toolNode.containerId(), initial);
    if (dialog.showAndGet()) {
      var edited = dialog.getEditedCommand();
      var tool = new HaxeCustomToolsStore.CustomTool(edited.name(), edited.command(), edited.workDirectory());
      HaxeCustomToolsStore.getInstance(project).updateTool(toolNode.containerId(), toolNode.name(), tool);
    }
  }
}
