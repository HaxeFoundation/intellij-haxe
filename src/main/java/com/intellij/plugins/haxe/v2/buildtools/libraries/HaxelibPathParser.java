package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.plugins.haxe.util.HaxeExternalPaths;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses `haxelib path <lib>` output: lines naming an absolute local path are
 * classpath roots (including the lib's dependencies), lines starting with a dash
 * are compiler flags (-D/-L/--macro). Anything else is haxelib talking - an
 * error, or a notice such as a newer haxelib's repository-format warning - and
 * is skipped.
 */
public final class HaxelibPathParser {

  private static final String DEFINE_PREFIX = "-D ";

  private HaxelibPathParser() {
  }

  /**
   * One library of a `haxelib path` run: its own classpath roots only, not its
   * dependencies', and the defines its extraParams.hxml sets with {@code -D}
   * lines. A {@code --macro} sets its defines at compile time; only the
   * compiler knows them.
   */
  public record LibrarySection(@NotNull String name,
                               @Nullable String version,
                               @NotNull List<String> classpaths,
                               @NotNull List<HaxeDefine> extraDefines) {
  }

  /// Splits `haxelib path <lib>` output into per-library sections. Each
  /// library prints its extraParams.hxml lines, then its classpath roots, then
  /// the `-D name=version` marker that closes its section (the requested lib
  /// first, dependencies after). Attributing the whole output to the requested
  /// lib mounts dependency sources under the wrong External Libraries entry -
  /// and, with version skew between entries, duplicates type definitions. A
  /// `-D` line with no classpath before it is an extraParams define of the
  /// section that follows, never a marker. Trailing classpaths without a marker
  /// (extraParams.hxml additions) fall to `requestedLib` with no version.
  @NotNull
  public static List<LibrarySection> parseSections(@NotNull String requestedLib, @NotNull List<String> outputLines) {
    List<LibrarySection> sections = new ArrayList<>();
    List<String> pending = new ArrayList<>();
    List<HaxeDefine> pendingDefines = new ArrayList<>();
    for (String line : outputLines) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) continue;
      if (trimmed.startsWith(DEFINE_PREFIX)) {
        HaxeDefine define = defineOf(trimmed);
        if (define.value() != null && !pending.isEmpty()) {
          sections.add(new LibrarySection(define.name(), define.value(), List.copyOf(pending), List.copyOf(pendingDefines)));
          pending.clear();
          pendingDefines.clear();
        }
        else {
          pendingDefines.add(define);
        }
        continue;
      }
      if (trimmed.startsWith("-")) continue;
      if (HaxeExternalPaths.absolutePathOf(trimmed) == null) continue;
      pending.add(trimmed);
    }
    if (!pending.isEmpty()) {
      sections.add(new LibrarySection(requestedLib, null, List.copyOf(pending), List.copyOf(pendingDefines)));
    }
    return sections;
  }

  /** {@code -D name} or {@code -D name=value}. */
  @NotNull
  private static HaxeDefine defineOf(@NotNull String flagLine) {
    String define = flagLine.substring(DEFINE_PREFIX.length()).trim();
    int equals = define.indexOf('=');
    if (equals < 0) return new HaxeDefine(define, null);
    return new HaxeDefine(define.substring(0, equals).trim(), define.substring(equals + 1).trim());
  }
}
