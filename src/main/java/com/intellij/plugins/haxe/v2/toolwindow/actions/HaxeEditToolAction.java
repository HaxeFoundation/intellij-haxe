package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomToolsStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.ui.HaxeCustomActionDialog;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ToolNode;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu on a custom tool row: edits its name and command.
 */
public final class HaxeEditToolAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeEditToolAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.edit.tool"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null || !(panel.getSelectedUserObject() instanceof ToolNode toolNode) || !toolNode.custom()) return;

    // edit the STORED values - the node's are the resolved display forms
    var stored = HaxeCustomToolsStore.getInstance(project).getTools(toolNode.containerId()).stream()
      .filter(tool -> tool.name().equals(toolNode.name()))
      .findFirst()
      .orElse(new HaxeCustomToolsStore.CustomTool(toolNode.name(), toolNode.detail(), ""));
    var initial = new HaxeCustomActionDialog.EditedCommand(stored.name(), stored.command(), stored.workDirectory());
    HaxeCustomActionDialog dialog = HaxeCustomActionDialog.forTool(project, initial);
    if (dialog.showAndGet()) {
      var edited = dialog.getEditedCommand();
      var tool = new HaxeCustomToolsStore.CustomTool(edited.name(), edited.command(), edited.workDirectory());
      HaxeCustomToolsStore.getInstance(project).updateTool(toolNode.containerId(), toolNode.name(), tool);
      panel.refreshTree();
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    boolean custom = panel.getSelectedUserObject() instanceof ToolNode toolNode && toolNode.custom();
    e.getPresentation().setEnabledAndVisible(custom);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
