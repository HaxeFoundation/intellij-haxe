package com.intellij.plugins.haxe.v2.toolwindow;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Per-developer UI state of the Haxe tool window, stored in the workspace file
 * — the same persistence home the Maven and Gradle tool windows use for their
 * tree state. Holds the expanded-row chain keys the panel maintains across
 * rebuilds; the platform's TreeState is not used because it matches rows by
 * their rendered text, which here carries volatile parts (counts, the active
 * marker).
 */
@Service(Service.Level.PROJECT)
@State(name = "HaxeToolWindowUiState", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class HaxeToolWindowUiState implements PersistentStateComponent<HaxeToolWindowUiState.State> {

  public static final class State {
    public List<String> expandedKeys = new ArrayList<>();
    public boolean hideEmptyModules;
  }

  private State state = new State();

  @NotNull
  public static HaxeToolWindowUiState getInstance(@NotNull Project project) {
    return project.getService(HaxeToolWindowUiState.class);
  }

  @Override
  public @NotNull State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    if (state.expandedKeys == null) {
      state.expandedKeys = new ArrayList<>();
    }
    this.state = state;
  }

  public boolean isHideEmptyModules() {
    return state.hideEmptyModules;
  }

  public void setHideEmptyModules(boolean hide) {
    state.hideEmptyModules = hide;
  }

  /** Empty when nothing was saved yet — the panel then applies its default expansion. */
  @NotNull
  public Set<String> getExpandedKeys() {
    return Set.copyOf(state.expandedKeys);
  }

  public void setExpandedKeys(@NotNull Collection<String> keys) {
    List<String> sorted = new ArrayList<>(keys);
    Collections.sort(sorted);
    state.expandedKeys = sorted;
  }
}
