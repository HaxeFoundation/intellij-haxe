package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * A tree context-menu action on one kind of selected row. It is shown and
 * enabled only while the selection is that kind and {@link #applies} to it,
 * and runs on the EDT, as every read of the selection must.
 */
abstract class HaxeSelectedNodeAction<N> extends DumbAwareAction {
  protected final HaxeToolWindowPanel panel;
  private final Class<N> nodeType;

  protected HaxeSelectedNodeAction(@NotNull HaxeToolWindowPanel panel, @NotNull Class<N> nodeType, @Nullable String text) {
    this(panel, nodeType, text, null);
  }

  protected HaxeSelectedNodeAction(@NotNull HaxeToolWindowPanel panel,
                                   @NotNull Class<N> nodeType,
                                   @Nullable String text,
                                   @Nullable Icon icon) {
    super(text, null, icon);
    this.panel = panel;
    this.nodeType = nodeType;
  }

  /** Whether the action applies to the selected row of its kind; every row by default. */
  protected boolean applies(@NotNull N node) {
    return true;
  }

  protected abstract void perform(@NotNull Project project, @NotNull N node);

  /** The selection the action reads: the selected row itself; an action on a row's ancestor reads that instead. */
  @Nullable
  protected Object selection() {
    return panel.getSelectedUserObject();
  }

  /** The selected row when it is of the action's kind and the action applies to it. */
  @Nullable
  protected final N selectedNode() {
    Object selected = selection();
    if (!nodeType.isInstance(selected)) return null;
    N node = nodeType.cast(selected);
    return applies(node) ? node : null;
  }

  @Override
  public final void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    N node = selectedNode();
    if (project == null || node == null) return;
    perform(project, node);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(selectedNode() != null);
  }

  @Override
  public final @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
