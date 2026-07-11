package com.intellij.plugins.haxe.hashlink;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;

/**
 * Detects whether a build produces HashLink BYTECODE and where, by scanning
 * compiler arguments — either a plain argument string or an hxml file
 * (following one level of .hxml includes, comments skipped).
 *
 * {@code -hl out.hl} (or {@code --hl}) is a bytecode build; {@code -hl out.c}
 * is HL/C native compilation, which the bytecode debugger cannot attach to and
 * is therefore reported as NOT a HashLink-bytecode build.
 *
 * hxml lookups are cached by file modification time: the run-configuration
 * gate is evaluated often (UI updates) and must not re-read files each time.
 */
final class HlBuildSniffer {
  private static final int MAX_INCLUDE_DEPTH = 4;

  /** What the build produces: bytecode or not, and the -hl output argument. */
  record HlBuild(boolean hlBytecode, @Nullable String output) {
    static final HlBuild NONE = new HlBuild(false, null);
  }

  private record CacheEntry(List<Path> files, List<Long> stamps, HlBuild build) {
    boolean isFresh() {
      for (int i = 0; i < files.size(); i++) {
        Long stamp = stamps.get(i);
        try {
          if (!stamp.equals(Files.getLastModifiedTime(files.get(i)).toMillis())) {
            return false;
          }
        } catch (IOException e) {
          return false;
        }
      }
      return true;
    }
  }

  private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

  private HlBuildSniffer() {
  }

  /** Scans a compiler-arguments string (single line, space separated). */
  static HlBuild fromArguments(@Nullable String arguments) {
    if (arguments == null || arguments.isBlank()) {
      return HlBuild.NONE;
    }
    return parse(List.of(arguments.trim().split("\\s+")));
  }

  /** Scans an hxml file (cached until any involved file changes). */
  static HlBuild fromHxml(Path hxmlFile) {
    String key = hxmlFile.toAbsolutePath().normalize().toString();
    CacheEntry cached = CACHE.get(key);
    if (cached != null && cached.isFresh()) {
      return cached.build();
    }

    List<Path> visited = new ArrayList<>();
    List<String> tokens = new ArrayList<>();
    collectTokens(hxmlFile, tokens, visited, 0);
    HlBuild build = parse(tokens);

    List<Long> stamps = new ArrayList<>(visited.size());
    try {
      for (Path file : visited) {
        stamps.add(Files.getLastModifiedTime(file).toMillis());
      }
      CACHE.put(key, new CacheEntry(List.copyOf(visited), List.copyOf(stamps), build));
    } catch (IOException ignored) {
      // don't cache what we can't validate
    }
    return build;
  }

  private static void collectTokens(Path hxmlFile, List<String> tokens, List<Path> visited, int depth) {
    if (depth > MAX_INCLUDE_DEPTH || !Files.isRegularFile(hxmlFile)) {
      return;
    }
    visited.add(hxmlFile);
    List<String> lines;
    try {
      lines = Files.readAllLines(hxmlFile);
    } catch (IOException e) {
      return;
    }
    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("#")) {
        continue;
      }
      for (String token : trimmed.split("\\s+")) {
        if (token.endsWith(".hxml")) {
          // an hxml include, relative to the including file
          Path include = hxmlFile.getParent() != null
                         ? hxmlFile.getParent().resolve(token) : Path.of(token);
          collectTokens(include.normalize(), tokens, visited, depth + 1);
        } else {
          tokens.add(token);
        }
      }
    }
  }

  private static HlBuild parse(List<String> tokens) {
    for (int i = 0; i < tokens.size(); i++) {
      String token = tokens.get(i);
      if (token.equals("-hl") || token.equals("--hl")) {
        String output = i + 1 < tokens.size() ? tokens.get(i + 1) : null;
        boolean bytecode = output != null && output.toLowerCase().endsWith(".hl");
        return new HlBuild(bytecode, output);
      }
    }
    return HlBuild.NONE;
  }
}
