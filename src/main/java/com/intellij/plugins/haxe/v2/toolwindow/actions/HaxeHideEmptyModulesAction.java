package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowUiState;
import org.jetbrains.annotations.NotNull;

/** Toolbar toggle hiding module rows that have no build files and nothing user-configured. */
public final class HaxeHideEmptyModulesAction extends ToggleAction implements DumbAware {

  private final Runnable refresh;

  public HaxeHideEmptyModulesAction(@NotNull Runnable refresh) {
    super(HaxeBundle.message("haxe.toolwindow.hide.empty.modules"), null, AllIcons.General.Filter);
    this.refresh = refresh;
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    return project != null && HaxeToolWindowUiState.getInstance(project).isHideEmptyModules();
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    Project project = e.getProject();
    if (project == null) return;
    HaxeToolWindowUiState.getInstance(project).setHideEmptyModules(state);
    refresh.run();
  }
}
