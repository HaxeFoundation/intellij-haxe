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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The IDE overrides of each build file, keyed by its path: define entries
 * applied on top of the build's own defines, and a custom target for a build
 * that selects no target itself. Only code analysis uses them - the
 * conditional-compilation context and the compiler's display requests;
 * builds and runs never receive them. Shares {@code .idea/haxeBuildConfig.xml}
 * with the other build-config stores, so they travel with the project.
 */
@Service(Service.Level.PROJECT)
@State(name = "HaxeIdeOverrides", storages = @Storage("haxeBuildConfig.xml"))
public final class HaxeIdeOverridesStore implements PersistentStateComponent<HaxeIdeOverridesStore.State> {

  /** The layout {@link #getState} writes; a stored lower number marks an older layout. */
  static final int STATE_VERSION = 1;

  public static final class State {
    /**
     * The layout version, 0 for a layout older than the field. Values equal
     * to the default are not written, so the default stays 0 and
     * {@link #getState} stamps the current version.
     */
    public int version;
    public List<BuildFileOverrides> buildFiles = new ArrayList<>();
  }

  public static final class BuildFileOverrides {
    public String buildFilePath;
    public List<DefineState> defines = new ArrayList<>();
    public String customTarget;
  }

  public static final class DefineState {
    public String name;
    public String value = "";
    public String effect = DefineEffect.SET.name();
  }

  private final @Nullable Project project;
  private State state = new State();

  public HaxeIdeOverridesStore(@NotNull Project project) {
    this.project = project;
  }

  /** State tests exercise load/get/set without a project; no events fire then. */
  @TestOnly
  public HaxeIdeOverridesStore() {
    this.project = null;
  }

  @NotNull
  public static HaxeIdeOverridesStore getInstance(@NotNull Project project) {
    return project.getService(HaxeIdeOverridesStore.class);
  }

  @Override
  public @NotNull State getState() {
    state.version = STATE_VERSION;
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    if (state.buildFiles == null) {
      state.buildFiles = new ArrayList<>();
    }
    state.buildFiles.removeIf(overrides -> StringUtil.isEmptyOrSpaces(overrides.buildFilePath));
    for (BuildFileOverrides overrides : state.buildFiles) {
      if (overrides.defines == null) {
        overrides.defines = new ArrayList<>();
      }
    }
    this.state = state;
    HaxeBuildSettingsListener.publisher(project).stateLoaded();
  }

  /** The build file's define entries, in name order. */
  @NotNull
  public List<DefineOverride> getDefines(@NotNull String buildFilePath) {
    BuildFileOverrides overrides = find(buildFilePath);
    if (overrides == null) return List.of();
    return overrides.defines.stream()
      .filter(define -> !StringUtil.isEmptyOrSpaces(define.name))
      .map(HaxeIdeOverridesStore::toDefine)
      .sorted(Comparator.comparing(DefineOverride::name, String.CASE_INSENSITIVE_ORDER))
      .toList();
  }

  /** Replaces the build file's define entries (the IDE Overrides dialog applies the whole list). */
  public void setDefines(@NotNull String buildFilePath, @NotNull List<DefineOverride> defines) {
    List<DefineState> serialized = new ArrayList<>();
    for (DefineOverride define : defines) {
      if (StringUtil.isEmptyOrSpaces(define.name())) continue;
      serialized.add(toState(define.name(), define.value(), define.effect()));
    }
    getOrCreate(buildFilePath).defines = serialized;
    changed(buildFilePath);
  }

  /** Adds or updates a SET entry, keeping any other entries. */
  public void putDefine(@NotNull String buildFilePath, @NotNull String name, @NotNull String value) {
    putEntry(buildFilePath, toState(name, value, DefineEffect.SET));
  }

  /** Adds a REMOVE entry, which hides a define the build sets; replaces any entry of the same name. */
  public void putUndefine(@NotNull String buildFilePath, @NotNull String name) {
    putEntry(buildFilePath, toState(name, "", DefineEffect.REMOVE));
  }

  public void removeDefine(@NotNull String buildFilePath, @NotNull String name) {
    BuildFileOverrides overrides = find(buildFilePath);
    if (overrides != null) {
      overrides.defines.removeIf(define -> name.equals(define.name));
    }
    changed(buildFilePath);
  }

  /** The stored custom target ({@code --custom-target} name), or null when unset. */
  @Nullable
  public String getCustomTarget(@NotNull String buildFilePath) {
    BuildFileOverrides overrides = find(buildFilePath);
    return overrides == null ? null : StringUtil.nullize(overrides.customTarget, true);
  }

  public void setCustomTarget(@NotNull String buildFilePath, @Nullable String customTarget) {
    getOrCreate(buildFilePath).customTarget = customTarget == null ? null : StringUtil.nullize(customTarget.trim());
    changed(buildFilePath);
  }

  /** The overrides follow their build file when it, or a folder above it, is moved or renamed; whether anything moved. */
  public boolean moveFile(@NotNull String oldPath, @NotNull String newPath) {
    boolean moved = false;
    for (BuildFileOverrides overrides : state.buildFiles) {
      String movedPath = HaxeStoredPaths.moved(overrides.buildFilePath, oldPath, newPath);
      moved |= !movedPath.equals(overrides.buildFilePath);
      overrides.buildFilePath = movedPath;
    }
    return moved;
  }

  private void putEntry(@NotNull String buildFilePath, @NotNull DefineState entry) {
    BuildFileOverrides overrides = getOrCreate(buildFilePath);
    overrides.defines.removeIf(define -> entry.name.equals(define.name));
    overrides.defines.add(entry);
    changed(buildFilePath);
  }

  /** Drops an entry left with nothing set, so the stored file lists only build files that have overrides. */
  private void changed(@NotNull String buildFilePath) {
    state.buildFiles.removeIf(overrides -> buildFilePath.equals(overrides.buildFilePath) && isEmpty(overrides));
    HaxeBuildSettingsListener.publisher(project).ideOverridesChanged(buildFilePath);
  }

  private static boolean isEmpty(@NotNull BuildFileOverrides overrides) {
    return overrides.defines.isEmpty() && StringUtil.isEmptyOrSpaces(overrides.customTarget);
  }

  @NotNull
  private static DefineState toState(@NotNull String name, @NotNull String value, @NotNull DefineEffect effect) {
    DefineState defineState = new DefineState();
    defineState.name = name;
    defineState.value = value;
    defineState.effect = effect.name();
    return defineState;
  }

  @NotNull
  private static DefineOverride toDefine(@NotNull DefineState defineState) {
    DefineEffect effect;
    try {
      effect = DefineEffect.valueOf(StringUtil.notNullize(defineState.effect, DefineEffect.SET.name()));
    }
    catch (IllegalArgumentException e) {
      effect = DefineEffect.SET;
    }
    return new DefineOverride(defineState.name, StringUtil.notNullize(defineState.value), effect);
  }

  @Nullable
  private BuildFileOverrides find(@NotNull String buildFilePath) {
    return state.buildFiles.stream()
      .filter(overrides -> buildFilePath.equals(overrides.buildFilePath))
      .findFirst()
      .orElse(null);
  }

  @NotNull
  private BuildFileOverrides getOrCreate(@NotNull String buildFilePath) {
    BuildFileOverrides overrides = find(buildFilePath);
    if (overrides == null) {
      overrides = new BuildFileOverrides();
      overrides.buildFilePath = buildFilePath;
      state.buildFiles.add(overrides);
    }
    return overrides;
  }
}
