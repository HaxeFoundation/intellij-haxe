package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildtools.settings.DefineOverride;
import com.intellij.plugins.haxe.v2.buildtools.settings.DefineEffect;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlArguments;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlFileParser;
import com.intellij.plugins.haxe.v2.buildtools.HaxeIdeOverrides;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeIdeOverridesStore;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Applies the build file's IDE overrides (its define entries and its custom
 * target) to the build's arguments before a display request sends them. Without the overrides, the server's view of which {@code #if}
 * branches are active and which target-specific modules ({@code Module.go.hx})
 * load differs from the editor's define context, and diagnostics and
 * completion would follow the raw build file instead of what the user
 * configured.
 */
public final class HaxeDisplayConfiguration {

  /**
   * A build file's IDE overrides in argument form: the defines to remove, by
   * compiler name (the name {@code #if} looks up: dashes become underscores,
   * whatever spelling was given), the {@code -D} arguments of the defines to
   * set, and the custom target to compile for (null when none is forwarded).
   */
  public record OverrideArguments(@NotNull Set<String> removedNames,
                                   @NotNull List<String> defineArgs,
                                   @Nullable String customTarget) {

    public static final OverrideArguments EMPTY = new OverrideArguments(Set.of(), List.of(), null);

    public OverrideArguments {
      removedNames = compilerNames(removedNames);
    }

    @NotNull
    private static Set<String> compilerNames(@NotNull Set<String> names) {
      Set<String> normalized = new LinkedHashSet<>();
      for (String name : names) {
        normalized.add(HaxeDefine.compilerName(name));
      }
      return Collections.unmodifiableSet(normalized);
    }

    public boolean isEmpty() {
      return removedNames.isEmpty() && defineArgs.isEmpty() && customTarget == null;
    }

    /** Part of the context's cache key, since different overrides make different server contexts. */
    @NotNull
    public String signature() {
      if (isEmpty()) return "";
      String target = customTarget == null ? "" : "@" + customTarget;
      return "-" + String.join(",", removedNames) + "+" + String.join(",", defineArgs) + target;
    }
  }

  private HaxeDisplayConfiguration() {
  }

  /// The build file's IDE overrides as arguments: a SET becomes
  /// `-D name[=value]`, a REMOVE lists the define to strip, and the custom
  /// target is forwarded as described at [#customTargetToForward]. The
  /// container decides the compiler. Call in a read action.
  @NotNull
  public static OverrideArguments overridesFor(@NotNull Project project,
                                               @NotNull HaxeBuildFile buildFile,
                                               @NotNull String containerId) {
    Set<String> removed = new LinkedHashSet<>();
    List<String> defineArgs = new ArrayList<>();
    for (DefineOverride override : HaxeIdeOverridesStore.getInstance(project).getDefines(buildFile.file().getPath())) {
      if (override.effect() == DefineEffect.REMOVE) {
        removed.add(override.name());
      }
      else {
        defineArgs.add("-D");
        defineArgs.add(override.value().isEmpty() ? override.name() : override.name() + "=" + override.value());
      }
    }
    String customTarget = HaxeIdeOverrides.effectiveCustomTarget(project, buildFile);
    HaxeLanguageLevel compiler = HaxeLanguageLevelUtil.fromCompiler(project, containerId);
    String forwardedTarget = customTargetToForward(customTarget, compiler);
    if (removed.isEmpty() && defineArgs.isEmpty() && forwardedTarget == null) return OverrideArguments.EMPTY;
    return new OverrideArguments(removed, defineArgs, forwardedTarget);
  }

  /**
   * The custom target to add to the build's arguments: the one analysis uses
   * ({@link HaxeIdeOverrides#effectiveCustomTarget}), only to a Haxe 5
   * compiler, the first with {@code --custom-target}. Without it, a build
   * such as a library's {@code extraParams.hxml} compiles for the default
   * platform, and the target's {@code Module.<target>.hx} files are not part
   * of it.
   */
  @Nullable
  static String customTargetToForward(@Nullable String customTarget, @Nullable HaxeLanguageLevel compiler) {
    if (customTarget == null) return null;
    return compiler != null && compiler.isAtLeast(HaxeLanguageLevel.HAXE_5_0) ? customTarget : null;
  }

  /// Applies the overrides to the build's base arguments. SETs are simply
  /// appended, because a later `-D` wins over an earlier value of the same
  /// define. A REMOVE has no command-line form, so the matching `-D` pairs
  /// are dropped instead, after expanding the hxml references (nested ones
  /// included), since a reference can hide the `-D` lines to strip. Defines
  /// match by compiler name, as in the editor's define context, and the
  /// debug flag counts as the define `debug`. A forwarded custom target
  /// comes last.
  @NotNull
  public static List<String> applyOverrides(@NotNull List<String> baseArgs, @NotNull OverrideArguments overrides) {
    if (overrides.isEmpty()) return baseArgs;
    List<String> args = baseWithRemovals(baseArgs, overrides);
    args.addAll(overrides.defineArgs());
    if (overrides.customTarget() != null) {
      args.addAll(List.of(HxmlFileParser.CUSTOM_TARGET_FLAG, overrides.customTarget()));
    }
    return args;
  }

  @NotNull
  private static List<String> baseWithRemovals(List<String> baseArgs, OverrideArguments overrides) {
    if (overrides.removedNames().isEmpty()) return new ArrayList<>(baseArgs);
    return withoutRemovedDefines(HxmlArguments.expandReferences(baseArgs), overrides.removedNames());
  }

  private static List<String> withoutRemovedDefines(@NotNull List<String> args, @NotNull Set<String> removedNames) {
    boolean debugRemoved = removedNames.contains(HxmlFileParser.DEBUG_DEFINE_NAME);
    List<String> kept = new ArrayList<>(args.size());
    for (int i = 0; i < args.size(); i++) {
      String arg = args.get(i);
      if (debugRemoved && HxmlFileParser.DEBUG_FLAGS.contains(arg)) continue;
      boolean isDefineFlag = HxmlFileParser.DEFINE_FLAGS.contains(arg);
      if (isDefineFlag && i + 1 < args.size() && removedNames.contains(definedName(args.get(i + 1)))) {
        i++;
        continue;
      }
      kept.add(arg);
    }
    return kept;
  }

  /** The compiler name of the define a {@code -D} value sets: everything before the optional {@code =value}, dashes as underscores. */
  @NotNull
  private static String definedName(@NotNull String defineValue) {
    int equals = defineValue.indexOf('=');
    String name = equals >= 0 ? defineValue.substring(0, equals) : defineValue;
    return HaxeDefine.compilerName(name);
  }
}
