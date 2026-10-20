package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.List;

/**
 * The project's single active build file. A file's conditional compilation is
 * evaluated against the define context of the build it is analysed under (its
 * resolve context); the active build file is the fallback for files without
 * one, and the default build file of its container when that container has no
 * build command. Stored in {@code .idea/haxeBuildConfig.xml}.
 */
@Service(Service.Level.PROJECT)
@State(name = "HaxeActiveBuildFiles", storages = @Storage("haxeBuildConfig.xml"))
public final class HaxeActiveBuildFileStore implements PersistentStateComponent<HaxeActiveBuildFileStore.State> {

  public static final class State {
    public String activeFile;
  }

  private final @Nullable Project project;
  private State state = new State();

  public HaxeActiveBuildFileStore(@NotNull Project project) {
    this.project = project;
  }

  /** State tests exercise load/get/set without a project; no events fire then. */
  @TestOnly
  public HaxeActiveBuildFileStore() {
    this.project = null;
  }

  @NotNull
  public static HaxeActiveBuildFileStore getInstance(@NotNull Project project) {
    return project.getService(HaxeActiveBuildFileStore.class);
  }

  @Override
  public @NotNull State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    this.state = state;
    HaxeBuildSettingsListener.publisher(project).stateLoaded();
  }

  /** The active build file stays active when it, or a folder above it, is moved or renamed; whether anything changed. */
  public boolean moveFile(@NotNull String oldPath, @NotNull String newPath) {
    if (state.activeFile == null) return false;
    String movedActive = HaxeStoredPaths.moved(state.activeFile, oldPath, newPath);
    boolean changed = !movedActive.equals(state.activeFile);
    state.activeFile = movedActive;
    return changed;
  }

  @Nullable
  public String getActiveFilePath() {
    return StringUtil.nullize(state.activeFile);
  }

  public void setActiveFile(@NotNull String filePath) {
    state.activeFile = filePath;
    HaxeBuildSettingsListener.publisher(project).activeBuildFileChanged();
  }

  /**
   * The project's active build file among all known build files: the stored choice
   * when it still exists, otherwise the only build file when there is exactly one
   * (a project with a single build file needs no explicit selection).
   */
  @Nullable
  public String resolveActivePath(@NotNull List<String> candidatePaths) {
    String stored = getActiveFilePath();
    if (stored != null && candidatePaths.contains(stored)) {
      return stored;
    }
    return candidatePaths.size() == 1 ? candidatePaths.getFirst() : null;
  }
}
