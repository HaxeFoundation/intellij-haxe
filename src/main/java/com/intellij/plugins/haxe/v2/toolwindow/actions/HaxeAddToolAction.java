package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeContainers;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomToolsStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.ui.HaxeCustomActionDialog;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ModuleNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ProjectNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ToolNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ToolsGroupNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tree context menu on a container (or its Tools rows): adds a custom tool.
 */
public final class HaxeAddToolAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeAddToolAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.add.tool"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    String containerId = selectedContainerId(e.getProject());
    if (project == null || containerId == null) return;

    HaxeCustomActionDialog dialog = HaxeCustomActionDialog.forTool(project, null);
    if (dialog.showAndGet()) {
      var edited = dialog.getEditedCommand();
      var tool = new HaxeCustomToolsStore.CustomTool(edited.name(), edited.command(), edited.workDirectory());
      HaxeCustomToolsStore.getInstance(project).addTool(containerId, tool);
      panel.refreshTree();
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(selectedContainerId(e.getProject()) != null);
  }

  @Nullable
  private String selectedContainerId(@Nullable Project project) {
    return switch (panel.getSelectedUserObject()) {
      case ModuleNode moduleNode -> moduleNode.name();
      case ToolsGroupNode toolsGroup -> toolsGroup.containerId();
      case ToolNode toolNode -> toolNode.containerId();
      case ProjectNode ignored -> projectContainerId(project);
      case null, default -> null;
    };
  }

  /** The project node's container id: the module owning the base directory, else the project-root container. */
  @Nullable
  private static String projectContainerId(@Nullable Project project) {
    if (project == null) return null;
    VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
    return baseDir == null ? HaxeContainers.PROJECT_ROOT_CONTAINER : HaxeContainers.containerIdFor(project, baseDir);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
