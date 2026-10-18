package com.intellij.plugins.haxe.v2.compiler.settings;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Project-level Haxe compiler configuration (v2).
 * <p>
 * Level overrides are keyed by CONTAINER id: a module's name, or a
 * non-module container id like {@code /project-root} for files outside
 * every module. Plain strings so the settings can be read and tested
 * without a live {@link Module} instance.
 */
public interface HaxeCompilerSettings {

  @NotNull
  static HaxeCompilerSettings getInstance(@NotNull Project project) {
    return project.getService(HaxeCompilerSettings.class);
  }

  /**
   * The effective project-wide default level: the explicit choice, or in
   * "use compiler level" mode the level resolved from the configured SDK
   * (latest known level when no SDK is registered).
   */
  @NotNull
  HaxeLanguageLevel getDefaultLanguageLevel();

  /**
   * The effective default for one container: same as {@link #getDefaultLanguageLevel()},
   * except that "use compiler level" mode resolves the CONTAINER's SDK, so
   * containers with different Environment SDKs get different levels.
   */
  @NotNull
  HaxeLanguageLevel getDefaultLanguageLevel(@NotNull String containerId);

  /** The explicitly chosen default level, or null in "use compiler level" mode. */
  @Nullable
  HaxeLanguageLevel getExplicitDefaultLanguageLevel();

  /** Null selects "use compiler level" mode (the default for new projects). */
  void setDefaultLanguageLevel(@Nullable HaxeLanguageLevel level);

  /** Explicit per-container overrides, keyed by container id. Containers without an entry use the default. */
  @NotNull
  Map<String, HaxeLanguageLevel> getContainerLanguageLevelOverrides();

  void setContainerLanguageLevelOverrides(@NotNull Map<String, HaxeLanguageLevel> overrides);

  @Nullable
  HaxeLanguageLevel getContainerLanguageLevelOverride(@NotNull String containerId);

  /** Sets or clears (when {@code level} is null) the override for a single container. */
  void setContainerLanguageLevelOverride(@NotNull String containerId, @Nullable HaxeLanguageLevel level);

  @NotNull
  HaxeLanguageLevel getEffectiveLanguageLevel(@NotNull String containerId);

  /**
   * The master toggle: whether editor highlighting uses the compilation
   * server's {@code display/diagnostics} (experimental). The per-feature
   * toggles below only take effect while this one is on.
   */
  boolean isCompilerDiagnosticsEnabled();

  void setCompilerDiagnosticsEnabled(boolean enabled);

  /** Compiler/parser errors, deprecation warnings, unresolved identifiers and missing fields. */
  boolean isDiagnosticsErrorsEnabled();

  void setDiagnosticsErrorsEnabled(boolean enabled);

  /** Unused imports from the compiler; while on it REPLACES the plugin's own unused-import inspection. */
  boolean isDiagnosticsUnusedImportsEnabled();

  void setDiagnosticsUnusedImportsEnabled(boolean enabled);

  /**
   * Removable code from the compiler: unused local variables and functions
   * (the compiler reports no unused fields or methods). While on it REPLACES
   * the plugin's unused local-variable and local-function inspections.
   */
  boolean isDiagnosticsRemovableCodeEnabled();

  void setDiagnosticsRemovableCodeEnabled(boolean enabled);

  /**
   * Whether the compiler's diagnostics are the ONLY analysis: the plugin's
   * own semantic annotators and inspections stand down while this and the
   * master toggle are on. Syntax coloring, inactive-code dimming and the
   * parser's error elements are not analysis and stay.
   */
  boolean isCompilerDiagnosticsOnly();

  void setCompilerDiagnosticsOnly(boolean enabled);

  /** Whether the plugin's own analysis is off: {@link #isCompilerDiagnosticsOnly()}, which counts only under the master toggle. */
  default boolean isStaticAnalysisSuppressed() {
    return isCompilerDiagnosticsEnabled() && isCompilerDiagnosticsOnly();
  }

  /**
   * Whether conditional compilation (`#if haxe_ver` and friends) evaluates
   * against the module's LANGUAGE LEVEL rather than the container's actual
   * compiler version.
   */
  boolean isUseLanguageLevelForConditionals();

  void setUseLanguageLevelForConditionals(boolean enabled);

  /** Where completion and resolve get their symbols; see {@link HaxeCompletionMode}. */
  @NotNull
  HaxeCompletionMode getCompletionMode();

  void setCompletionMode(@NotNull HaxeCompletionMode mode);

  /**
   * The master toggle for the compiler-backed IDE features: the per-feature
   * toggles below count only while this one is on, and references the IDE
   * cannot resolve are looked up among the members that exist only after
   * macros. Rename and Safe Delete stay static regardless.
   */
  boolean isCompilerIdeFeaturesEnabled();

  void setCompilerIdeFeaturesEnabled(boolean enabled);

  /** Type inlays from the compilation server only, in place of the IDE's own type hints. */
  boolean isCompilerInlaysEnabled();

  void setCompilerInlaysEnabled(boolean enabled);

  /** Find Usages through {@code display/references}; identifier highlighting stays static. */
  boolean isCompilerFindUsagesEnabled();

  void setCompilerFindUsagesEnabled(boolean enabled);

  /** Go to declaration and implementation through the compilation server, generated members included. */
  boolean isCompilerNavigationEnabled();

  void setCompilerNavigationEnabled(boolean enabled);

  /**
   * Whether go-to-declaration on a macro-generated member opens the
   * post-macro preview, where the member exists, instead of the source
   * position the macro gave it. Counts only under compiler navigation.
   */
  boolean isGeneratedMemberPreviewEnabled();

  void setGeneratedMemberPreviewEnabled(boolean enabled);

  /** Whether completion may use the compiler at all; {@link #getCompletionMode()} then says how. */
  boolean isCompilerCompletionEnabled();

  void setCompilerCompletionEnabled(boolean enabled);

  default boolean usesCompilerInlays() {
    return isCompilerIdeFeaturesEnabled() && isCompilerInlaysEnabled();
  }

  default boolean usesCompilerFindUsages() {
    return isCompilerIdeFeaturesEnabled() && isCompilerFindUsagesEnabled();
  }

  default boolean usesCompilerNavigation() {
    return isCompilerIdeFeaturesEnabled() && isCompilerNavigationEnabled();
  }

  default boolean usesGeneratedMemberPreview() {
    return usesCompilerNavigation() && isGeneratedMemberPreviewEnabled();
  }

  /** The completion mode in force: the chosen one while the master and the completion toggles are on, else IDE only. */
  @NotNull
  default HaxeCompletionMode getEffectiveCompletionMode() {
    boolean enabled = isCompilerIdeFeaturesEnabled() && isCompilerCompletionEnabled();
    return enabled ? getCompletionMode() : HaxeCompletionMode.IDE_ONLY;
  }

  /** Whether completion, and the generated symbols it draws on, come from the compiler in any form. */
  default boolean usesCompilerCompletion() {
    return getEffectiveCompletionMode().usesCompiler();
  }

  /** How display requests reach the compiler; see {@link HaxeDisplayConnectionMode}. */
  @NotNull
  HaxeDisplayConnectionMode getDisplayConnectionMode();

  void setDisplayConnectionMode(@NotNull HaxeDisplayConnectionMode mode);
}
