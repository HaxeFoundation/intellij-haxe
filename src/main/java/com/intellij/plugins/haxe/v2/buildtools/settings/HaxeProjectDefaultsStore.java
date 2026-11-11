package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Map;
import java.util.TreeMap;

/**
 * The project's default target of each Lime, OpenFL and NME build file, and
 * the default {@code --next} section of each hxml: what everyone working on
 * the project starts with until they pick another one
 * ({@link HaxeTargetSelectionStore}, {@link HaxeSectionSelectionStore}).
 * Shares {@code .idea/haxeBuildConfig.xml} with the other build-config
 * stores, so the defaults travel with the project.
 */
@Service(Service.Level.PROJECT)
@State(name = "HaxeProjectDefaults", storages = @Storage("haxeBuildConfig.xml"))
public final class HaxeProjectDefaultsStore implements PersistentStateComponent<HaxeProjectDefaultsStore.State> {

  /** The layout {@link #getState} writes; a stored lower number marks an older layout. */
  static final int STATE_VERSION = 1;

  public static final class State {
    /**
     * The layout version, 0 for a layout older than the field. Values equal
     * to the default are not written, so the default stays 0 and
     * {@link #getState} stamps the current version.
     */
    public int version;
    public Map<String, String> targetsByFile = new TreeMap<>();
    public Map<String, String> sectionsByFile = new TreeMap<>();
  }

  private final @Nullable Project project;
  private State state = new State();

  public HaxeProjectDefaultsStore(@NotNull Project project) {
    this.project = project;
  }

  /** State tests exercise load/get/set without a project; no events fire then. */
  @TestOnly
  public HaxeProjectDefaultsStore() {
    this.project = null;
  }

  @NotNull
  public static HaxeProjectDefaultsStore getInstance(@NotNull Project project) {
    return project.getService(HaxeProjectDefaultsStore.class);
  }

  @Override
  public @NotNull State getState() {
    state.version = STATE_VERSION;
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    if (state.targetsByFile == null) {
      state.targetsByFile = new TreeMap<>();
    }
    if (state.sectionsByFile == null) {
      state.sectionsByFile = new TreeMap<>();
    }
    this.state = state;
    HaxeBuildSettingsListener.publisher(project).stateLoaded();
  }

  /** A build file keeps its defaults when it, or a folder above it, is moved or renamed; whether anything changed. */
  public boolean moveFile(@NotNull String oldPath, @NotNull String newPath) {
    boolean targetsMoved = HaxeStoredPaths.moveKeys(state.targetsByFile, oldPath, newPath);
    boolean sectionsMoved = HaxeStoredPaths.moveKeys(state.sectionsByFile, oldPath, newPath);
    return targetsMoved || sectionsMoved;
  }

  @Nullable
  public String getTargetId(@NotNull VirtualFile buildFile) {
    return state.targetsByFile.get(buildFile.getPath());
  }

  public void setTargetId(@NotNull VirtualFile buildFile, @NotNull String targetId) {
    state.targetsByFile.put(buildFile.getPath(), targetId);
    HaxeBuildSettingsListener.publisher(project).targetSelected(buildFile.getPath());
  }

  /** The default section's identity ({@code HxmlFileParser.sectionIds}), or null when the project sets none. */
  @Nullable
  public String getSectionId(@NotNull VirtualFile buildFile) {
    return state.sectionsByFile.get(buildFile.getPath());
  }

  public void setSectionId(@NotNull VirtualFile buildFile, @NotNull String sectionId) {
    state.sectionsByFile.put(buildFile.getPath(), sectionId);
    HaxeBuildSettingsListener.publisher(project).sectionSelected(buildFile.getPath());
  }
}
