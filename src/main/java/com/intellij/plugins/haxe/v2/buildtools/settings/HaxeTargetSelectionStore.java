package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Map;
import java.util.TreeMap;

/**
 * Remembers the target platform the user picked per build file in the Haxe tool
 * window. Stored in the workspace file: target choice is a per-developer setting,
 * not project configuration.
 */
@Service(Service.Level.PROJECT)
@State(name = "HaxeToolWindowTargets", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class HaxeTargetSelectionStore implements PersistentStateComponent<HaxeTargetSelectionStore.State> {

  public static final class State {
    public Map<String, String> targetsByFile = new TreeMap<>();
  }

  private final @Nullable Project project;
  private State state = new State();

  public HaxeTargetSelectionStore(@NotNull Project project) {
    this.project = project;
  }

  /** State tests exercise load/get/set without a project; no events fire then. */
  @TestOnly
  public HaxeTargetSelectionStore() {
    this.project = null;
  }

  @NotNull
  public static HaxeTargetSelectionStore getInstance(@NotNull Project project) {
    return project.getService(HaxeTargetSelectionStore.class);
  }

  @Override
  public @NotNull State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    if (state.targetsByFile == null) {
      state.targetsByFile = new TreeMap<>();
    }
    this.state = state;
    HaxeBuildSettingsListener.publish(project);
  }

  @Nullable
  public String getSelectedTargetId(@NotNull VirtualFile buildFile) {
    return state.targetsByFile.get(buildFile.getPath());
  }

  public void setSelectedTargetId(@NotNull VirtualFile buildFile, @NotNull String targetId) {
    state.targetsByFile.put(buildFile.getPath(), targetId);
    HaxeBuildSettingsListener.publish(project);
  }
}
