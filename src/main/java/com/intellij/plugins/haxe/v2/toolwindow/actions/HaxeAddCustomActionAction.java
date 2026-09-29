package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ActionNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ActionsGroupNode;
import com.intellij.plugins.haxe.v2.toolwindow.ui.HaxeCustomActionDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tree context menu on the Actions group (or an action row): creates a custom action.
 */
public final class HaxeAddCustomActionAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeAddCustomActionAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.add.action"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    String ownerId = selectedOwnerId();
    if (project == null || ownerId == null) return;

    HaxeCustomActionDialog dialog = HaxeCustomActionDialog.forAction(project, null);
    if (dialog.showAndGet()) {
      HaxeCustomActionsStore.getInstance(project).addAction(ownerId, dialog.getAction());
      panel.refreshTree();
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(selectedOwnerId() != null);
  }

  @Nullable
  private String selectedOwnerId() {
    return switch (panel.getSelectedUserObject()) {
      case ActionsGroupNode actionsGroup -> actionsGroup.ownerId();
      case ActionNode actionNode -> actionNode.ownerId();
      case null, default -> null;
    };
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
