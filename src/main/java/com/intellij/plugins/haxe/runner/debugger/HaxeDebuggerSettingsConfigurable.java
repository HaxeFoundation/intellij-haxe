package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Settings → Build, Execution, Deployment → Debugger → Haxe: the home of the
 * toString-rendering opt-in outside a debug session (the Variables view's
 * gear menu hosts the same toggle inside one). The UI lives in
 * {@link HaxeDebuggerSettingsForm} (a GUI-Designer .form); the warning above
 * the checkbox spells out what opting in means — the debugger runs debuggee
 * code while rendering.
 *
 * Applying a change pushes the new value to every RUNNING Haxe debug session
 * (same live path as the gear toggle), so no session restart is needed.
 */
public final class HaxeDebuggerSettingsConfigurable implements SearchableConfigurable {
  private final Project project;
  private @Nullable HaxeDebuggerSettingsForm form;

  public HaxeDebuggerSettingsConfigurable(Project project) {
    this.project = project;
  }

  @Override
  public @NotNull String getId() {
    return "haxe.debugger.settings";
  }

  @Override
  public String getDisplayName() {
    return HaxeDebuggerBundle.message("haxe.debugger.settings.display.name");
  }

  @Override
  public @Nullable JComponent createComponent() {
    form = new HaxeDebuggerSettingsForm();
    return form.getPanel();
  }

  @Override
  public boolean isModified() {
    return form != null && form.isModified(settings());
  }

  @Override
  public void apply() {
    if (form == null || !form.isModified(settings())) {
      return;
    }
    form.applyEditorTo(settings());
    // live: every running Haxe session re-renders with the new labels
    boolean enabled = settings().isRenderObjectsWithToString();
    for (XDebugSession session : XDebuggerManager.getInstance(project).getDebugSessions()) {
      if (session.getDebugProcess() instanceof DapDebugProcess hxcpp) {
        hxcpp.pushToStringRendering(enabled);
      }
    }
  }

  @Override
  public void reset() {
    if (form != null) {
      form.resetEditorFrom(settings());
    }
  }

  @Override
  public void disposeUIResources() {
    form = null;
  }

  private HaxeDebuggerSettings settings() {
    return HaxeDebuggerSettings.getInstance(project);
  }
}
