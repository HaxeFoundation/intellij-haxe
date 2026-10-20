package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.CustomAction;
import com.intellij.plugins.haxe.v2.runconfig.HaxeProgramLaunches;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ActionNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.BuildFileRow;
import com.intellij.plugins.haxe.v2.toolwindow.ui.HaxeCustomActionDialog;
import org.jetbrains.annotations.NotNull;

/** Tree context menu on a custom action row: edits its name, command, work directory and launch. */
public final class HaxeEditCustomActionAction extends HaxeSelectedNodeAction<ActionNode> {

  public HaxeEditCustomActionAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, ActionNode.class, HaxeBundle.message("haxe.toolwindow.edit.action"));
  }

  @Override
  protected boolean applies(@NotNull ActionNode actionNode) {
    return actionNode.custom();
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull ActionNode actionNode) {
    BuildFileRow row = panel.getSelectedBuildFileRowAncestor();
    if (row == null) return;

    // edit the STORED values - the node's command is the ${target}-expanded display form
    CustomAction stored = actionNode.storedAction(project);
    HaxeCustomActionDialog dialog = HaxeCustomActionDialog.forAction(project, row.buildFile(), actionNode.launchOutput(), stored);
    if (dialog.showAndGet()) {
      CustomAction updated = dialog.getAction();
      HaxeCustomActionsStore store = HaxeCustomActionsStore.getInstance(project);
      store.updateAction(actionNode.ownerId(), actionNode.name(), updated);
      store.renameReferences(actionNode.ownerId(), actionNode.name(), updated.name());
      updateBuildSteps(project, actionNode, updated);
    }
  }

  /** Build steps naming the action follow its new name; an emptied command drops them, since they would fail. */
  private static void updateBuildSteps(@NotNull Project project, @NotNull ActionNode before, @NotNull CustomAction updated) {
    String stepName = updated.command().isBlank() ? null : updated.name();
    if (!before.name().equals(stepName)) {
      HaxeProgramLaunches.retargetBuildSteps(project, before.ownerId(), before.name(), stepName);
    }
  }
}
