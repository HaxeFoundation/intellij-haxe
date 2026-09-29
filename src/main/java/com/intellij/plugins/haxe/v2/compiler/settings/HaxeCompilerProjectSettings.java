package com.intellij.plugins.haxe.v2.compiler.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Persistent implementation of {@link HaxeCompilerSettings}, stored in {@code .idea/haxeCompiler.xml}.
 */
@State(name = "HaxeCompilerConfiguration", storages = @Storage("haxeCompiler.xml"))
public final class HaxeCompilerProjectSettings implements HaxeCompilerSettings, PersistentStateComponent<HaxeCompilerProjectSettings.State> {

  /** Stored in place of a version to select "use compiler level" mode. */
  static final String USE_COMPILER_LEVEL = "auto";

  public static final class State {
    public String defaultLanguageLevel = USE_COMPILER_LEVEL;
    // keys are CONTAINER ids (module name, or /project-root); the field
    // name predates non-module containers and stays for storage compatibility
    public Map<String, String> moduleLanguageLevels = new TreeMap<>();
    public boolean compilerDiagnostics = false;
    public boolean compilerDiagnosticsErrors = true;
    public boolean compilerDiagnosticsUnusedImports = false;
    public boolean compilerDiagnosticsRemovableCode = false;
    public boolean compilerDiagnosticsOnly = false;
    public boolean useLanguageLevelForConditionals = true;
    public String completionMode = HaxeCompletionMode.IDE_AND_COMPILER.getId();
    public boolean compilerIdeFeatures = false;
  }

  private final @Nullable Project project;
  private State state = new State();

  /** Null project (tests): "use compiler level" cannot resolve an SDK and falls back to the latest level. */
  public HaxeCompilerProjectSettings(@Nullable Project project) {
    this.project = project;
  }

  @Override
  public @NotNull State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    if (state.moduleLanguageLevels == null) {
      state.moduleLanguageLevels = new TreeMap<>();
    }
    this.state = state;
  }

  @Override
  public @NotNull HaxeLanguageLevel getDefaultLanguageLevel() {
    return defaultLevelFor(null);
  }

  @Override
  public @NotNull HaxeLanguageLevel getDefaultLanguageLevel(@NotNull String containerId) {
    return defaultLevelFor(containerId);
  }

  @Override
  public @Nullable HaxeLanguageLevel getExplicitDefaultLanguageLevel() {
    return HaxeLanguageLevel.fromVersionString(state.defaultLanguageLevel);
  }

  @Override
  public void setDefaultLanguageLevel(@Nullable HaxeLanguageLevel level) {
    state.defaultLanguageLevel = level == null ? USE_COMPILER_LEVEL : level.getVersionString();
  }

  // In "use compiler level" mode (also the fallback for unparsable stored
  // values) the container's SDK decides; latest() when no SDK is registered.
  @NotNull
  private HaxeLanguageLevel defaultLevelFor(@Nullable String containerId) {
    HaxeLanguageLevel explicit = getExplicitDefaultLanguageLevel();
    if (explicit != null) return explicit;
    HaxeLanguageLevel fromCompiler = project == null ? null : HaxeLanguageLevelUtil.fromCompiler(project, containerId);
    return fromCompiler != null ? fromCompiler : HaxeLanguageLevel.latest();
  }

  @Override
  public @NotNull Map<String, HaxeLanguageLevel> getContainerLanguageLevelOverrides() {
    Map<String, HaxeLanguageLevel> result = new LinkedHashMap<>();
    state.moduleLanguageLevels.forEach((containerId, version) -> {
      HaxeLanguageLevel level = HaxeLanguageLevel.fromVersionString(version);
      if (level != null) {
        result.put(containerId, level);
      }
    });
    return result;
  }

  @Override
  public void setContainerLanguageLevelOverrides(@NotNull Map<String, HaxeLanguageLevel> overrides) {
    Map<String, String> serialized = new TreeMap<>();
    overrides.forEach((containerId, level) -> serialized.put(containerId, level.getVersionString()));
    state.moduleLanguageLevels = serialized;
  }

  @Override
  public @Nullable HaxeLanguageLevel getContainerLanguageLevelOverride(@NotNull String containerId) {
    return HaxeLanguageLevel.fromVersionString(state.moduleLanguageLevels.get(containerId));
  }

  @Override
  public void setContainerLanguageLevelOverride(@NotNull String containerId, @Nullable HaxeLanguageLevel level) {
    if (level == null) {
      state.moduleLanguageLevels.remove(containerId);
    }
    else {
      state.moduleLanguageLevels.put(containerId, level.getVersionString());
    }
  }

  @Override
  public @NotNull HaxeLanguageLevel getEffectiveLanguageLevel(@NotNull String containerId) {
    HaxeLanguageLevel override = getContainerLanguageLevelOverride(containerId);
    return override != null ? override : defaultLevelFor(containerId);
  }

  @Override
  public boolean isCompilerDiagnosticsEnabled() {
    return state.compilerDiagnostics;
  }

  @Override
  public void setCompilerDiagnosticsEnabled(boolean enabled) {
    state.compilerDiagnostics = enabled;
  }

  @Override
  public boolean isDiagnosticsErrorsEnabled() {
    return state.compilerDiagnosticsErrors;
  }

  @Override
  public void setDiagnosticsErrorsEnabled(boolean enabled) {
    state.compilerDiagnosticsErrors = enabled;
  }

  @Override
  public boolean isDiagnosticsUnusedImportsEnabled() {
    return state.compilerDiagnosticsUnusedImports;
  }

  @Override
  public void setDiagnosticsUnusedImportsEnabled(boolean enabled) {
    state.compilerDiagnosticsUnusedImports = enabled;
  }

  @Override
  public boolean isDiagnosticsRemovableCodeEnabled() {
    return state.compilerDiagnosticsRemovableCode;
  }

  @Override
  public void setDiagnosticsRemovableCodeEnabled(boolean enabled) {
    state.compilerDiagnosticsRemovableCode = enabled;
  }

  @Override
  public boolean isCompilerDiagnosticsOnly() {
    return state.compilerDiagnosticsOnly;
  }

  @Override
  public void setCompilerDiagnosticsOnly(boolean enabled) {
    state.compilerDiagnosticsOnly = enabled;
  }

  @Override
  public boolean isUseLanguageLevelForConditionals() {
    return state.useLanguageLevelForConditionals;
  }

  @Override
  public void setUseLanguageLevelForConditionals(boolean enabled) {
    state.useLanguageLevelForConditionals = enabled;
  }

  @Override
  public @NotNull HaxeCompletionMode getCompletionMode() {
    return HaxeCompletionMode.fromId(state.completionMode);
  }

  @Override
  public void setCompletionMode(@NotNull HaxeCompletionMode mode) {
    state.completionMode = mode.getId();
  }

  @Override
  public boolean isCompilerIdeFeaturesEnabled() {
    return state.compilerIdeFeatures;
  }

  @Override
  public void setCompilerIdeFeaturesEnabled(boolean enabled) {
    state.compilerIdeFeatures = enabled;
  }
}
