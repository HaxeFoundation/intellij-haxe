package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeActionReferences;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.ActionReference;
import com.intellij.plugins.haxe.v2.runconfig.HaxeProgramLaunches;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.actions.HaxeCustomActionRemoval.Decisions;
import com.intellij.plugins.haxe.v2.toolwindow.actions.HaxeCustomActionRemoval.References;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ActionNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tree context menu on a custom action row: removes it after a confirmation
 * (the entry sits beside Edit and Duplicate); when other actions run it first,
 * a second question decides whether their references go with it or stay as
 * missing ({@link HaxeCustomActionRemoval}). The "Run Haxe action" build steps
 * that named it are dropped either way.
 */
public final class HaxeRemoveCustomActionAction extends HaxeSelectedNodeAction<ActionNode> {

  public HaxeRemoveCustomActionAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, ActionNode.class, HaxeBundle.message("haxe.toolwindow.remove.action"));
  }

  @Override
  protected boolean applies(@NotNull ActionNode actionNode) {
    return actionNode.custom();
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull ActionNode actionNode) {
    HaxeCustomActionsStore store = HaxeCustomActionsStore.getInstance(project);
    Decisions decisions = new DialogDecisions(project, actionNode.ownerId());
    if (!HaxeCustomActionRemoval.remove(store, actionNode.ownerId(), actionNode.name(), decisions)) return;
    HaxeProgramLaunches.retargetBuildSteps(project, actionNode.ownerId(), actionNode.name(), null);
  }

  /** The two questions as dialogs: Yes/No for the removal, then "Remove There Too" / "Keep as Missing" / Cancel for the references. */
  private record DialogDecisions(@NotNull Project project, @NotNull String ownerId) implements Decisions {

    @Override
    public boolean confirmRemoval(@NotNull String name) {
      String question = HaxeBundle.message("haxe.toolwindow.remove.action.confirm", name);
      return MessageDialogBuilder.yesNo(title(), question)
        .icon(Messages.getQuestionIcon())
        .ask(project);
    }

    @Override
    public @NotNull References references(@NotNull String name, @NotNull List<ActionReference> dependents) {
      String dependentLabels = dependents.stream()
        .map(reference -> HaxeActionReferences.label(project, ownerId, reference))
        .collect(Collectors.joining(", "));
      String question = HaxeBundle.message("haxe.toolwindow.remove.action.references", name, dependentLabels);
      int answer = MessageDialogBuilder.yesNoCancel(title(), question)
        .yesText(HaxeBundle.message("haxe.toolwindow.remove.action.references.remove"))
        .noText(HaxeBundle.message("haxe.toolwindow.remove.action.references.keep"))
        .icon(Messages.getQuestionIcon())
        .show(project);
      return switch (answer) {
        case Messages.YES -> References.REMOVE;
        case Messages.NO -> References.KEEP;
        default -> References.CANCEL;
      };
    }

    private static String title() {
      return HaxeBundle.message("haxe.toolwindow.remove.action");
    }
  }
}
