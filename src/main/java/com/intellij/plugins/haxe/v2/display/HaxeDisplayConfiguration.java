package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.v2.buildtools.settings.EnvironmentDefine;
import com.intellij.plugins.haxe.v2.buildtools.settings.DefineEffect;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlArguments;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlFileParser;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Applies the container's IDE define overrides to the build's arguments
 * before a display request sends them. Without the overrides, the server's
 * view of which {@code #if} branches are active differs from the editor's
 * define context, and diagnostics and completion would follow the raw build
 * file instead of what the user configured.
 */
public final class HaxeDisplayConfiguration {

  /**
   * A container's define overrides in argument form: the names of the
   * defines to remove, and the {@code -D} arguments of the defines to set.
   */
  public record DefineOverrides(@NotNull Set<String> removedNames, @NotNull List<String> defineArgs) {

    public static final DefineOverrides EMPTY = new DefineOverrides(Set.of(), List.of());

    public boolean isEmpty() {
      return removedNames.isEmpty() && defineArgs.isEmpty();
    }

    /** Part of the context's cache key, since different overrides make different server contexts. */
    @NotNull
    public String signature() {
      return isEmpty() ? "" : "-" + String.join(",", removedNames) + "+" + String.join(",", defineArgs);
    }
  }

  private HaxeDisplayConfiguration() {
  }

  /// The container's overrides as arguments: a SET becomes `-D name[=value]`, a REMOVE lists the define to strip.
  @NotNull
  public static DefineOverrides overridesFor(@NotNull Project project, @NotNull String containerId) {
    // TODO: the container's Custom target setting is not forwarded as
    //  "--custom-target" - the flag exists only in Haxe 5, so it must be gated
    //  on the container's compiler version first (an hxml-declared custom
    //  target reaches the display server through the hxml itself).
    Set<String> removed = new LinkedHashSet<>();
    List<String> defineArgs = new ArrayList<>();
    for (EnvironmentDefine override : HaxeEnvironmentStore.getInstance(project).getDefines(containerId)) {
      if (override.effect() == DefineEffect.REMOVE) {
        removed.add(override.name());
      }
      else {
        defineArgs.add("-D");
        defineArgs.add(override.value().isEmpty() ? override.name() : override.name() + "=" + override.value());
      }
    }
    return removed.isEmpty() && defineArgs.isEmpty() ? DefineOverrides.EMPTY : new DefineOverrides(removed, defineArgs);
  }

  /// Applies the overrides to the build's base arguments. SETs are simply
  /// appended, because a later `-D` wins over an earlier value of the same
  /// define. A REMOVE has no command-line form. The hxml references are
  /// therefore expanded one level, since a reference can hide the `-D` lines
  /// to strip, and the matching define pairs are dropped.
  @NotNull
  public static List<String> applyOverrides(@NotNull List<String> baseArgs, @NotNull DefineOverrides overrides) {
    if (overrides.isEmpty()) return baseArgs;
    List<String> args = baseWithRemovals(baseArgs, overrides);
    args.addAll(overrides.defineArgs());
    return args;
  }

  @NotNull
  private static List<String> baseWithRemovals(List<String> baseArgs, DefineOverrides overrides) {
    if (overrides.removedNames().isEmpty()) return new ArrayList<>(baseArgs);
    return withoutRemovedDefines(HxmlArguments.expandReferences(baseArgs), overrides.removedNames());
  }

  private static List<String> withoutRemovedDefines(@NotNull List<String> args, @NotNull Set<String> removedNames) {
    List<String> kept = new ArrayList<>(args.size());
    for (int i = 0; i < args.size(); i++) {
      String arg = args.get(i);
      boolean isDefineFlag = HxmlFileParser.DEFINE_FLAGS.contains(arg);
      if (isDefineFlag && i + 1 < args.size()) {
        String value = args.get(i + 1);
        // the define's name is everything before the optional =value
        int equals = value.indexOf('=');
        String name = equals >= 0 ? value.substring(0, equals) : value;
        if (removedNames.contains(name)) {
          i++;
          continue;
        }
      }
      kept.add(arg);
    }
    return kept;
  }
}
