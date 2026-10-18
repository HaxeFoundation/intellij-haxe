package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.plugins.haxe.util.HaxeExternalPaths;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * hxml content as a compiler argument list: line parsing, expansion of hxml
 * file references and the working directory the arguments select. The
 * structured view (target, defines, libraries) is {@link HxmlFileParser}.
 */
@CustomLog
public final class HxmlArguments {

  /** {@code --cwd <directory>}: where the compiler runs and resolves relative paths from; a later one composes with the earlier. */
  public static final String CWD_FLAG = "--cwd";

  private HxmlArguments() {
  }

  /**
   * The directory the arguments make the compiler run in: every {@code --cwd}
   * composed in order, a relative one resolving against the previous, as haxe
   * does; null without any. A relative first one stays relative.
   */
  @Nullable
  public static Path workDirectory(@NotNull List<String> args) {
    Path cwd = null;
    for (int i = 0; i + 1 < args.size(); i++) {
      if (args.get(i).equals(CWD_FLAG)) cwd = composedCwd(cwd, args.get(i + 1));
    }
    return cwd;
  }

  /** The --cwd value applied to the directory so far; one naming no usable path leaves it unchanged. */
  @Nullable
  private static Path composedCwd(@Nullable Path cwd, @NotNull String value) {
    Path next = HaxeExternalPaths.pathOf(value);
    if (next == null) return cwd;
    return cwd == null ? next : cwd.resolve(next);
  }

  /**
   * hxml semantics: one flag per line, everything after the first space is
   * that flag's SINGLE argument (unquoted spaces included); bare lines are
   * standalone arguments, {@code #} starts a comment.
   */
  @NotNull
  public static List<String> parseLines(@NotNull List<String> lines) {
    List<String> args = new ArrayList<>();
    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
      if (trimmed.startsWith("-")) {
        int space = trimmed.indexOf(' ');
        if (space < 0) {
          args.add(trimmed);
        } else {
          args.add(trimmed.substring(0, space));
          args.add(trimmed.substring(space + 1).trim());
        }
      } else {
        args.add(trimmed);
      }
    }
    return args;
  }

  /// Args may carry an .hxml file REFERENCE instead of flags (an HXML build
  /// context is `["--cwd", dir, "build.hxml"]` — the compiler expands
  /// the file server-side). Callers that need to SEE the real flags (output
  /// redirection, define removal) expand them here. A reference resolves
  /// against the preceding `--cwd`, and references nested in an expanded
  /// file expand too: a define two includes down is as real to the compiler
  /// as a top-level one. A reference already being expanded (an include
  /// cycle) and an unreadable reference are kept unchanged.
  @NotNull
  public static List<String> expandReferences(@NotNull List<String> args) {
    List<String> expanded = new ArrayList<>(args.size() + 16);
    expandInto(args, null, new HashSet<>(), expanded);
    return expanded;
  }

  private static void expandInto(@NotNull List<String> args,
                                 @Nullable Path cwd,
                                 @NotNull Set<Path> expanding,
                                 @NotNull List<String> out) {
    for (int i = 0; i < args.size(); i++) {
      String arg = args.get(i);
      if (arg.equals(CWD_FLAG) && i + 1 < args.size()) {
        cwd = composedCwd(cwd, args.get(i + 1));
        out.add(arg);
        out.add(args.get(i + 1));
        i++;
        continue;
      }
      if (HxmlFileParser.isHxmlReference(arg) && expandReference(arg, cwd, expanding, out)) continue;
      out.add(arg);
    }
  }

  /** Expands one reference into {@code out}; false when it names no usable path, is unreadable or is already being expanded. */
  private static boolean expandReference(@NotNull String reference,
                                         @Nullable Path cwd,
                                         @NotNull Set<Path> expanding,
                                         @NotNull List<String> out) {
    Path referenced = HaxeExternalPaths.pathOf(reference);
    if (referenced == null) return false;
    Path hxmlFile = (cwd != null ? cwd.resolve(referenced) : referenced).normalize();
    if (!expanding.add(hxmlFile)) return false;
    try {
      expandInto(parseLines(Files.readAllLines(hxmlFile)), cwd, expanding, out);
      return true;
    } catch (IOException e) {
      log.info("cannot expand hxml reference " + hxmlFile + ": " + e.getMessage());
      return false;
    } finally {
      expanding.remove(hxmlFile);
    }
  }
}
