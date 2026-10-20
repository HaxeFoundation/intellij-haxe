package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildSettingsListener.EnvironmentAspect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.TestOnly;

/**
 * Per-container (module / project root) environment: the Haxe SDK to use, the
 * Build command, whether builds use the compilation server and whether the
 * container runs a server of its own. Shares
 * {@code .idea/haxeBuildConfig.xml} with the active-build-file selection.
 * Define entries and the custom target are per build file
 * ({@link HaxeIdeOverridesStore}).
 */
@Service(Service.Level.PROJECT)
@State(name = "HaxeEnvironments", storages = @Storage("haxeBuildConfig.xml"))
public final class HaxeEnvironmentStore implements PersistentStateComponent<HaxeEnvironmentStore.State> {

  /** The layout version {@link #getState} stamps; define entries and the custom target live per build file. */
  static final int STATE_VERSION = 1;

  public static final class State {
    /**
     * The layout version, 0 for a layout older than the field. Values equal
     * to the default are not written, so the default stays 0 and
     * {@link #getState} stamps the current version.
     */
    public int version;
    public List<ContainerEnvironment> environments = new ArrayList<>();
  }

  /** Options of an older layout that this class no longer declares are skipped on load and dropped on the next save. */
  public static final class ContainerEnvironment {
    public String containerId;
    public String sdkName;
    public String compileFilePath;
    public String compileActionName;
    public String compileArguments = "";
    public boolean skipBuild;
    public boolean useCompilationServer = true;
    public boolean dedicatedServer;
  }

  /**
   * What compiling the container runs: a build file, an optional action override
   * (null = the type-derived default command) and extra arguments (e.g. "-clean -debug").
   */
  public record CompileCommand(@NotNull String buildFilePath, @Nullable String actionName, @NotNull String arguments) {
  }

  private final @Nullable Project project;
  private State state = new State();

  public HaxeEnvironmentStore(@NotNull Project project) {
    this.project = project;
  }

  /** State tests exercise load/get/set without a project; no events fire then. */
  @TestOnly
  public HaxeEnvironmentStore() {
    this.project = null;
  }

  @NotNull
  public static HaxeEnvironmentStore getInstance(@NotNull Project project) {
    return project.getService(HaxeEnvironmentStore.class);
  }

  @Override
  public @NotNull State getState() {
    state.version = STATE_VERSION;
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    if (state.environments == null) {
      state.environments = new ArrayList<>();
    }
    this.state = state;
    HaxeBuildSettingsListener.publisher(project).stateLoaded();
  }

  /** Whether the user overrode anything for the container: SDK, compile command, server opt-out or dedicated server. */
  public boolean hasUserOverrides(@NotNull String containerId) {
    ContainerEnvironment environment = find(containerId);
    if (environment == null) return false;
    return StringUtil.isNotEmpty(environment.sdkName)
           || StringUtil.isNotEmpty(environment.compileFilePath)
           || environment.skipBuild
           || !environment.useCompilationServer
           || environment.dedicatedServer;
  }

  /** Drops every stored setting of the container (a removed module leaves no stale state behind). */
  public void clearContainer(@NotNull String containerId) {
    state.environments.removeIf(environment -> containerId.equals(environment.containerId));
    HaxeBuildSettingsListener.publisher(project).environmentChanged(containerId, EnvironmentAspect.ALL);
  }

  /** SDK name chosen for the container, or null to use the Build Tools default. */
  @Nullable
  public String getSdkName(@NotNull String containerId) {
    ContainerEnvironment environment = find(containerId);
    return environment == null ? null : StringUtil.nullize(environment.sdkName);
  }

  public void setSdkName(@NotNull String containerId, @Nullable String sdkName) {
    getOrCreate(containerId).sdkName = sdkName;
    HaxeBuildSettingsListener.publisher(project).environmentChanged(containerId, EnvironmentAspect.SDK);
  }

  /** A Build command keeps its build file when the file, or a folder above it, is moved or renamed; whether anything changed. */
  public boolean moveFile(@NotNull String oldPath, @NotNull String newPath) {
    boolean changed = false;
    for (ContainerEnvironment environment : state.environments) {
      if (environment.compileFilePath == null) continue;
      String movedFile = HaxeStoredPaths.moved(environment.compileFilePath, oldPath, newPath);
      changed |= !movedFile.equals(environment.compileFilePath);
      environment.compileFilePath = movedFile;
    }
    return changed;
  }

  /** The build command set for the container; null when none is set, so it builds its default or skips building. */
  @Nullable
  public CompileCommand getCompileCommand(@NotNull String containerId) {
    ContainerEnvironment environment = find(containerId);
    if (environment == null || StringUtil.isEmptyOrSpaces(environment.compileFilePath)) return null;
    return new CompileCommand(environment.compileFilePath,
                              StringUtil.nullize(environment.compileActionName),
                              StringUtil.notNullize(environment.compileArguments));
  }

  /** Sets the container's build command; null returns it to its default. Either way the container builds again. */
  public void setCompileCommand(@NotNull String containerId, @Nullable CompileCommand compileCommand) {
    ContainerEnvironment environment = getOrCreate(containerId);
    environment.compileFilePath = compileCommand == null ? null : compileCommand.buildFilePath();
    environment.compileActionName = compileCommand == null ? null : compileCommand.actionName();
    environment.compileArguments = compileCommand == null ? "" : compileCommand.arguments();
    environment.skipBuild = false;
    HaxeBuildSettingsListener.publisher(project).environmentChanged(containerId, EnvironmentAspect.COMPILE_COMMAND);
  }

  /** Whether Build Project skips the container, whatever build files it has. */
  public boolean isBuildSkipped(@NotNull String containerId) {
    ContainerEnvironment environment = find(containerId);
    return environment != null && environment.skipBuild;
  }

  /** Makes Build Project skip the container and clears its build command. */
  public void skipBuild(@NotNull String containerId) {
    ContainerEnvironment environment = getOrCreate(containerId);
    environment.compileFilePath = null;
    environment.compileActionName = null;
    environment.compileArguments = "";
    environment.skipBuild = true;
    HaxeBuildSettingsListener.publisher(project).environmentChanged(containerId, EnvironmentAspect.COMPILE_COMMAND);
  }

  /** Whether the container's compile command connects to the project's compilation server (when enabled). */
  public boolean isUsingCompilationServer(@NotNull String containerId) {
    ContainerEnvironment environment = find(containerId);
    return environment == null || environment.useCompilationServer;
  }

  public void setUsingCompilationServer(@NotNull String containerId, boolean use) {
    getOrCreate(containerId).useCompilationServer = use;
    HaxeBuildSettingsListener.publisher(project).environmentChanged(containerId, EnvironmentAspect.COMPILATION_SERVER);
  }

  /** Whether the container's builds and compiler requests use a compilation server of their own instead of its SDK's shared one. */
  public boolean isUsingDedicatedServer(@NotNull String containerId) {
    ContainerEnvironment environment = find(containerId);
    return environment != null && environment.dedicatedServer;
  }

  public void setUsingDedicatedServer(@NotNull String containerId, boolean dedicated) {
    getOrCreate(containerId).dedicatedServer = dedicated;
    HaxeBuildSettingsListener.publisher(project).environmentChanged(containerId, EnvironmentAspect.COMPILATION_SERVER);
  }

  @Nullable
  private ContainerEnvironment find(@NotNull String containerId) {
    return state.environments.stream()
      .filter(environment -> containerId.equals(environment.containerId))
      .findFirst()
      .orElse(null);
  }

  @NotNull
  private ContainerEnvironment getOrCreate(@NotNull String containerId) {
    ContainerEnvironment environment = find(containerId);
    if (environment == null) {
      environment = new ContainerEnvironment();
      environment.containerId = containerId;
      state.environments.add(environment);
    }
    return environment;
  }
}
