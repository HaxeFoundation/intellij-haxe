package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

/**
 * The Variables-view settings (gear) toggle "Expression stepping", shown for
 * EVAL debug sessions only (the interpreter has a position per expression
 * node; compiled targets step by line). On: every step is one raw
 * sub-expression step and the editor highlights the exact expression about
 * to run. SESSION-scoped, not persisted — it is an investigation mode, and a
 * fresh session starts in normal line stepping.
 */
public final class HaxeExpressionSteppingToggleAction extends ToggleAction implements DumbAware {
  private final Supplier<Boolean> state;
  private final Consumer<Boolean> push;

  public HaxeExpressionSteppingToggleAction(@NotNull Supplier<Boolean> state, @NotNull Consumer<Boolean> push) {
    super(HaxeDebuggerBundle.message("haxe.debugger.expression.stepping"),
          HaxeDebuggerBundle.message("haxe.debugger.expression.stepping.description"), null);
    this.state = state;
    this.push = push;
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    return state.get();
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean selected) {
    push.accept(selected);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }
}
