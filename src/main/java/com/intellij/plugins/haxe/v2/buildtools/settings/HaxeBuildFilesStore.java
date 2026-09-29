package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * Manual corrections to build file auto-detection, per container: files the user
 * added by hand (subfolders, unusually named project xml) and auto-detected files
 * the user hid. Shares {@code .idea/haxeBuildConfig.xml} with the other build-config
 * stores. The known build files decide the active one's single-file fallback, so
 * every change publishes {@link HaxeBuildSettingsListener}.
 */
@Service(Service.Level.PROJECT)
@State(name = "HaxeBuildFiles", storages = @Storage("haxeBuildConfig.xml"))
public final class HaxeBuildFilesStore implements PersistentStateComponent<HaxeBuildFilesStore.State> {

  public static final class State {
    public List<ContainerFiles> containers = new ArrayList<>();
  }

  public static final class ContainerFiles {
    public String containerId;
    public List<String> addedPaths = new ArrayList<>();
    public List<String> hiddenPaths = new ArrayList<>();
  }

  private final @Nullable Project project;
  private State state = new State();

  public HaxeBuildFilesStore(@NotNull Project project) {
    this.project = project;
  }

  /** State tests exercise load/get/set without a project; no events fire then. */
  @TestOnly
  public HaxeBuildFilesStore() {
    this.project = null;
  }

  @NotNull
  public static HaxeBuildFilesStore getInstance(@NotNull Project project) {
    return project.getService(HaxeBuildFilesStore.class);
  }

  @Override
  public @NotNull State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    if (state.containers == null) {
      state.containers = new ArrayList<>();
    }
    state.containers.forEach(container -> {
      if (container.addedPaths == null) container.addedPaths = new ArrayList<>();
      if (container.hiddenPaths == null) container.hiddenPaths = new ArrayList<>();
    });
    this.state = state;
    HaxeBuildSettingsListener.publish(project);
  }

  @NotNull
  public List<String> getAddedPaths(@NotNull String containerId) {
    ContainerFiles container = find(containerId);
    return container == null ? List.of() : List.copyOf(container.addedPaths);
  }

  @NotNull
  public List<String> getHiddenPaths(@NotNull String containerId) {
    ContainerFiles container = find(containerId);
    return container == null ? List.of() : List.copyOf(container.hiddenPaths);
  }

  /** Every manually added path across all containers (for the build file watcher). */
  @NotNull
  public List<String> getAllAddedPaths() {
    return state.containers.stream()
      .flatMap(container -> container.addedPaths.stream())
      .toList();
  }

  /** Adds a file manually; adding a previously hidden file un-hides it. */
  public void addFile(@NotNull String containerId, @NotNull String path) {
    ContainerFiles container = getOrCreate(containerId);
    container.hiddenPaths.remove(path);
    if (!container.addedPaths.contains(path)) {
      container.addedPaths.add(path);
    }
    HaxeBuildSettingsListener.publish(project);
  }

  /** Removes a manual entry, or hides an auto-detected file. */
  public void removeFile(@NotNull String containerId, @NotNull String path) {
    ContainerFiles container = getOrCreate(containerId);
    if (!container.addedPaths.remove(path) && !container.hiddenPaths.contains(path)) {
      container.hiddenPaths.add(path);
    }
    HaxeBuildSettingsListener.publish(project);
  }

  @Nullable
  private ContainerFiles find(@NotNull String containerId) {
    return state.containers.stream()
      .filter(container -> containerId.equals(container.containerId))
      .findFirst()
      .orElse(null);
  }

  @NotNull
  private ContainerFiles getOrCreate(@NotNull String containerId) {
    ContainerFiles container = find(containerId);
    if (container == null) {
      container = new ContainerFiles();
      container.containerId = containerId;
      state.containers.add(container);
    }
    return container;
  }
}
