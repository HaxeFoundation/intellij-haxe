package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-build-file working directory overrides (keyed by the file's path),
 * replacing the type-derived default of
 * {@link com.intellij.plugins.haxe.v2.buildtools.HaxeBuildWorkDirectories}.
 * Shares {@code .idea/haxeBuildConfig.xml} with the other build-config stores.
 */
@Service(Service.Level.PROJECT)
@State(name = "HaxeWorkDirectories", storages = @Storage("haxeBuildConfig.xml"))
public final class HaxeWorkDirectoryStore implements PersistentStateComponent<HaxeWorkDirectoryStore.State> {

  public static final class State {
    public List<FileWorkDirectory> files = new ArrayList<>();
  }

  public static final class FileWorkDirectory {
    public String buildFilePath;
    public String workDirectory;
  }

  private final @NotNull Project project;
  private State state = new State();

  public HaxeWorkDirectoryStore(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  public static HaxeWorkDirectoryStore getInstance(@NotNull Project project) {
    return project.getService(HaxeWorkDirectoryStore.class);
  }

  @Override
  public @NotNull State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    if (state.files == null) {
      state.files = new ArrayList<>();
    }
    this.state = state;
  }

  /** The stored override (absolute path), or null to use the type-derived default. */
  @Nullable
  public String getWorkDirectory(@NotNull String buildFilePath) {
    return state.files.stream()
      .filter(file -> buildFilePath.equals(file.buildFilePath))
      .findFirst()
      .map(file -> StringUtil.nullize(file.workDirectory))
      .orElse(null);
  }

  /** Sets the override; null or blank clears it back to the default. */
  public void setWorkDirectory(@NotNull String buildFilePath, @Nullable String workDirectory) {
    state.files.removeIf(file -> buildFilePath.equals(file.buildFilePath));
    if (!StringUtil.isEmptyOrSpaces(workDirectory)) {
      FileWorkDirectory entry = new FileWorkDirectory();
      entry.buildFilePath = buildFilePath;
      entry.workDirectory = workDirectory;
      state.files.add(entry);
    }
    HaxeBuildSettingsListener.publish(project);
  }
}
