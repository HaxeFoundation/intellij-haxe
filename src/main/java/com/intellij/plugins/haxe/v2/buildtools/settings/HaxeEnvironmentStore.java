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
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.TestOnly;

/**
 * Per-container (module / project root) environment: the Haxe SDK to use and
 * user-managed defines applied on top of the active build file's own defines.
 * A define entry either sets a value (adding or overriding the build file's) or
 * removes a define the build file declares. Shares {@code .idea/haxeBuildConfig.xml}
 * with the active-build-file selection.
 */
@Service(Service.Level.PROJECT)
@State(name = "HaxeEnvironments", storages = @Storage("haxeBuildConfig.xml"))
public final class HaxeEnvironmentStore implements PersistentStateComponent<HaxeEnvironmentStore.State> {

  public static final class State {
    public List<ContainerEnvironment> environments = new ArrayList<>();
  }

  public static final class ContainerEnvironment {
    public String containerId;
    public String sdkName;
    public List<DefineState> defines = new ArrayList<>();
    public String customTarget;
    public String compileFilePath;
    public String compileActionName;
    public String compileArguments = "";
    public boolean useCompilationServer = true;
  }

  /**
   * What compiling the container runs: a build file, an optional action override
   * (null = the type-derived default command) and extra arguments (e.g. "-clean -debug").
   */
  public record CompileCommand(@NotNull String buildFilePath, @Nullable String actionName, @NotNull String arguments) {
  }

  public static final class DefineState {
    public String name;
    public String value = "";
    public String effect = DefineEffect.SET.name();
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
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    if (state.environments == null) {
      state.environments = new ArrayList<>();
    }
    state.environments.forEach(HaxeEnvironmentStore::sanitizeDefines);
    this.state = state;
    HaxeBuildSettingsListener.publish(project);
  }

  /**
   * XML from an older storage format (defines were a map) deserializes into the list
   * as foreign objects (Strings) because of type erasure - they must be dropped here
   * or every later access explodes with a ClassCastException.
   */
  private static void sanitizeDefines(@NotNull ContainerEnvironment environment) {
    if (environment.defines == null) {
      environment.defines = new ArrayList<>();
      return;
    }
    List<DefineState> cleaned = new ArrayList<>();
    for (Object entry : (List<?>)environment.defines) {
      if (entry instanceof DefineState defineState) {
        cleaned.add(defineState);
      }
    }
    environment.defines = cleaned;
  }

  /** Whether the user overrode anything for the container: SDK, defines, target, compile command or server opt-out. */
  public boolean hasUserOverrides(@NotNull String containerId) {
    ContainerEnvironment environment = find(containerId);
    if (environment == null) return false;
    return StringUtil.isNotEmpty(environment.sdkName)
           || StringUtil.isNotEmpty(environment.customTarget)
           || StringUtil.isNotEmpty(environment.compileFilePath)
           || !environment.defines.isEmpty()
           || !environment.useCompilationServer;
  }

  /** Drops every stored setting of the container (a removed module leaves no stale state behind). */
  public void clearContainer(@NotNull String containerId) {
    state.environments.removeIf(environment -> containerId.equals(environment.containerId));
    HaxeBuildSettingsListener.publish(project);
  }

  /** SDK name chosen for the container, or null to use the Build Tools default. */
  @Nullable
  public String getSdkName(@NotNull String containerId) {
    ContainerEnvironment environment = find(containerId);
    return environment == null ? null : StringUtil.nullize(environment.sdkName);
  }

  public void setSdkName(@NotNull String containerId, @Nullable String sdkName) {
    getOrCreate(containerId).sdkName = sdkName;
    HaxeBuildSettingsListener.publish(project);
  }

  /** The container's compile command, or null when unset (the container is skipped on project build). */
  @Nullable
  public CompileCommand getCompileCommand(@NotNull String containerId) {
    ContainerEnvironment environment = find(containerId);
    if (environment == null || StringUtil.isEmptyOrSpaces(environment.compileFilePath)) return null;
    return new CompileCommand(environment.compileFilePath,
                              StringUtil.nullize(environment.compileActionName),
                              StringUtil.notNullize(environment.compileArguments));
  }

  public void setCompileCommand(@NotNull String containerId, @Nullable CompileCommand compileCommand) {
    ContainerEnvironment environment = getOrCreate(containerId);
    environment.compileFilePath = compileCommand == null ? null : compileCommand.buildFilePath();
    environment.compileActionName = compileCommand == null ? null : compileCommand.actionName();
    environment.compileArguments = compileCommand == null ? "" : compileCommand.arguments();
    HaxeBuildSettingsListener.publish(project);
  }

  /** Whether the container's compile command connects to the project's compilation server (when enabled). */
  public boolean isUsingCompilationServer(@NotNull String containerId) {
    ContainerEnvironment environment = find(containerId);
    return environment == null || environment.useCompilationServer;
  }

  public void setUsingCompilationServer(@NotNull String containerId, boolean use) {
    getOrCreate(containerId).useCompilationServer = use;
    HaxeBuildSettingsListener.publish(project);
  }

  /**
   * The container's custom compilation target ({@code --custom-target} name), or
   * null when unset. Activates the target's {@code <Module>.<name>.hx} variant
   * files and wins over an hxml-declared custom target.
   */
  @Nullable
  public String getCustomTarget(@NotNull String containerId) {
    ContainerEnvironment environment = find(containerId);
    return environment == null ? null : StringUtil.nullize(environment.customTarget, true);
  }

  public void setCustomTarget(@NotNull String containerId, @Nullable String customTarget) {
    getOrCreate(containerId).customTarget = customTarget == null ? null : StringUtil.nullize(customTarget.trim());
    HaxeBuildSettingsListener.publish(project);
  }

  /** The container's define entries, in name order. */
  @NotNull
  public List<EnvironmentDefine> getDefines(@NotNull String containerId) {
    ContainerEnvironment environment = find(containerId);
    if (environment == null) return List.of();
    return environment.defines.stream()
      .filter(define -> !StringUtil.isEmptyOrSpaces(define.name))
      .map(HaxeEnvironmentStore::toDefine)
      .sorted(Comparator.comparing(EnvironmentDefine::name, String.CASE_INSENSITIVE_ORDER))
      .toList();
  }

  /** Replaces the container's define entries (the Configure Environment dialog applies the whole list). */
  public void setDefines(@NotNull String containerId, @NotNull List<EnvironmentDefine> defines) {
    List<DefineState> serialized = new ArrayList<>();
    for (EnvironmentDefine define : defines) {
      if (StringUtil.isEmptyOrSpaces(define.name())) continue;
      DefineState defineState = new DefineState();
      defineState.name = define.name();
      defineState.value = define.value();
      defineState.effect = define.effect().name();
      serialized.add(defineState);
    }
    getOrCreate(containerId).defines = serialized;
    HaxeBuildSettingsListener.publish(project);
  }

  /** Adds or updates a SET define, keeping any other entries. */
  public void putDefine(@NotNull String containerId, @NotNull String name, @NotNull String value) {
    DefineState defineState = new DefineState();
    defineState.name = name;
    defineState.value = value;

    ContainerEnvironment environment = getOrCreate(containerId);
    environment.defines.removeIf(define -> name.equals(define.name));
    environment.defines.add(defineState);

    HaxeBuildSettingsListener.publish(project);
  }

  public void removeDefine(@NotNull String containerId, @NotNull String name) {
    ContainerEnvironment environment = find(containerId);
    if (environment != null) {
      environment.defines.removeIf(define -> name.equals(define.name));
    }
    HaxeBuildSettingsListener.publish(project);
  }

  @NotNull
  private static EnvironmentDefine toDefine(@NotNull DefineState defineState) {
    DefineEffect effect;
    try {
      effect = DefineEffect.valueOf(StringUtil.notNullize(defineState.effect, DefineEffect.SET.name()));
    }
    catch (IllegalArgumentException e) {
      effect = DefineEffect.SET;
    }
    return new EnvironmentDefine(defineState.name, StringUtil.notNullize(defineState.value), effect);
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
