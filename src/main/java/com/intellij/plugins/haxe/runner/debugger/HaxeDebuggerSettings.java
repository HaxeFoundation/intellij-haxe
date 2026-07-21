package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Project-level debugger preferences, persisted with the project
 * configuration (.idea/haxeDebuggerSettings.xml).
 *
 * {@code renderObjectsWithToString} switches Variables-view object labels
 * from the class name to the object's own {@code toString()} result. OFF by
 * default and an explicit opt-in: toString is user code the debugger runs
 * while rendering — it can have side effects, be slow, and on hxcpp a
 * self-recursing toString kills the debuggee (stack overflow is uncatchable
 * there). Toggled live from the Variables view's settings (gear) menu; each
 * debug process pushes the value to its backend when it changes (see
 * {@link HaxeToStringRenderToggleAction}).
 */
@State(name = "HaxeDebuggerSettings", storages = @Storage("haxeDebuggerSettings.xml"))
public final class HaxeDebuggerSettings implements PersistentStateComponent<HaxeDebuggerSettings.State> {

  public static final class State {
    public boolean renderObjectsWithToString = false;
  }

  private State state = new State();

  public static HaxeDebuggerSettings getInstance(@NotNull Project project) {
    return project.getService(HaxeDebuggerSettings.class);
  }

  public boolean isRenderObjectsWithToString() {
    return state.renderObjectsWithToString;
  }

  public void setRenderObjectsWithToString(boolean value) {
    state.renderObjectsWithToString = value;
  }

  @Override
  public State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    this.state = state;
  }
}
