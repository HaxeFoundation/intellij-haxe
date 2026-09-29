package com.intellij.plugins.haxe.v2.buildsystem;

import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * hxml content as a compiler argument list: line parsing and one-level
 * expansion of hxml file references. The structured view (target, defines,
 * libraries) is {@link HxmlFileParser}.
 */
@CustomLog
public final class HxmlArguments {

  private HxmlArguments() {
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
  /// redirection, define removal) expand one level here, resolving against
  /// the preceding `--cwd`. A reference nested inside an expanded file stays
  /// as-is: its flags remain invisible to the caller. An unreadable reference
  /// is kept unchanged.
  @NotNull
  public static List<String> expandReferences(@NotNull List<String> args) {
    List<String> expanded = new ArrayList<>(args.size() + 16);
    Path cwd = null;
    for (int i = 0; i < args.size(); i++) {
      String arg = args.get(i);
      if (arg.equals("--cwd") && i + 1 < args.size()) {
        cwd = Path.of(args.get(i + 1));
        expanded.add(arg);
        expanded.add(args.get(i + 1));
        i++;
        continue;
      }
      if (arg.endsWith(".hxml")) {
        Path hxmlFile = cwd != null ? cwd.resolve(arg) : Path.of(arg);
        try {
          expanded.addAll(parseLines(Files.readAllLines(hxmlFile)));
          continue;
        } catch (IOException e) {
          log.info("cannot expand hxml reference " + hxmlFile + ": " + e.getMessage());
        }
      }
      expanded.add(arg);
    }
    return expanded;
  }
}
