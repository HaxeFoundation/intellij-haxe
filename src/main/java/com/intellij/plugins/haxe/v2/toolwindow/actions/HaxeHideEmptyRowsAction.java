package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowUiState;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowUiState.EmptyRows;
import org.jetbrains.annotations.NotNull;

/** Toolbar filter toggle hiding one kind of empty row: modules with nothing in them, or grouping and value rows with nothing to show. */
public final class HaxeHideEmptyRowsAction extends ToggleAction implements DumbAware {

  private final EmptyRows rows;
  private final Runnable refresh;

  public HaxeHideEmptyRowsAction(@NotNull EmptyRows rows, @NotNull Runnable refresh) {
    super(HaxeBundle.message(rows == EmptyRows.MODULES ? "haxe.toolwindow.hide.empty.modules" : "haxe.toolwindow.hide.empty.nodes"));
    this.rows = rows;
    this.refresh = refresh;
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    return project != null && HaxeToolWindowUiState.getInstance(project).isHiding(rows);
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    Project project = e.getProject();
    if (project == null) return;
    HaxeToolWindowUiState.getInstance(project).setHiding(rows, state);
    refresh.run();
  }
}
