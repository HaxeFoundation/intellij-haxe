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
 * The target platform of each Lime, OpenFL and NME build file: the one the
 * user picked in the Haxe tool window, else the project's default
 * ({@link HaxeProjectDefaultsStore}). The pick is stored in the workspace file,
 * so it stays with this developer; picking the project's default drops it.
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
    HaxeBuildSettingsListener.publisher(project).stateLoaded();
  }

  /** A build file keeps its selected target when it, or a folder above it, is moved or renamed; whether anything changed. */
  public boolean moveFile(@NotNull String oldPath, @NotNull String newPath) {
    return HaxeStoredPaths.moveKeys(state.targetsByFile, oldPath, newPath);
  }

  /** The user's pick, else the project's default; null when neither is set, which means the framework's default target. */
  @Nullable
  public String getSelectedTargetId(@NotNull VirtualFile buildFile) {
    String picked = state.targetsByFile.get(buildFile.getPath());
    return picked != null ? picked : projectDefault(buildFile);
  }

  public void setSelectedTargetId(@NotNull VirtualFile buildFile, @NotNull String targetId) {
    if (targetId.equals(projectDefault(buildFile))) {
      state.targetsByFile.remove(buildFile.getPath());
    }
    else {
      state.targetsByFile.put(buildFile.getPath(), targetId);
    }
    HaxeBuildSettingsListener.publisher(project).targetSelected(buildFile.getPath());
  }

  /** Makes the target the project's default for the build file, which the user then no longer overrides. */
  public void setProjectDefault(@NotNull VirtualFile buildFile, @NotNull String targetId) {
    state.targetsByFile.remove(buildFile.getPath());
    if (project != null) HaxeProjectDefaultsStore.getInstance(project).setTargetId(buildFile, targetId);
  }

  @Nullable
  private String projectDefault(@NotNull VirtualFile buildFile) {
    return project == null ? null : HaxeProjectDefaultsStore.getInstance(project).getTargetId(buildFile);
  }
}
