package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.xdebugger.XDebugSession;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

/**
 * The Variables-view settings (gear) toggle "Render Objects with
 * toString()", registered per debug session via {@code
 * XDebugProcess.registerAdditionalActions}. Reads/writes the PROJECT-level
 * {@link HaxeDebuggerSettings} and pushes a flip to THIS session's backend
 * through {@code push} — the debug process sends the custom
 * {@code custom/setToStringRendering} request and rebuilds the views, so
 * the current stop re-renders without a restart. Sessions started later
 * pick the setting up at launch.
 */
public final class HaxeToStringRenderToggleAction extends ToggleAction implements DumbAware {
  private final XDebugSession session;
  private final Consumer<Boolean> push;

  public HaxeToStringRenderToggleAction(@NotNull XDebugSession session, @NotNull Consumer<Boolean> push) {
    super(HaxeDebuggerBundle.message("haxe.debugger.render.tostring"),
          HaxeDebuggerBundle.message("haxe.debugger.render.tostring.description"), null);
    this.session = session;
    this.push = push;
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    return HaxeDebuggerSettings.getInstance(session.getProject()).isRenderObjectsWithToString();
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean selected) {
    HaxeDebuggerSettings.getInstance(session.getProject()).setRenderObjectsWithToString(selected);
    push.accept(selected);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }
}
