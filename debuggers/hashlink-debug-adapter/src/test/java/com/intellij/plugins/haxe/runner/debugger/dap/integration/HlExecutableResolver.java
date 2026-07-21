package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Locates the HashLink executable for integration tests, in order:
 * <ol>
 *   <li>system property {@code hashlink.executable} (forwarded from -PhashlinkBin by the test task)</li>
 *   <li>environment variables {@code HASHLINK_BIN}, {@code HASHLINK}, {@code HASHLINKPATH} —
 *       each tried as the executable itself, then as a directory containing it</li>
 *   <li>directories on {@code PATH}</li>
 * </ol>
 */
public final class HlExecutableResolver {
  private static final String[] ENV_VARIABLES = {"HASHLINK_BIN", "HASHLINK", "HASHLINKPATH"};
  private static final String[] EXECUTABLE_NAMES = {"hl.exe", "hl"};

  private HlExecutableResolver() {
  }

  public static Optional<Path> resolve() {
    String property = System.getProperty("hashlink.executable");
    if (property != null && !property.isBlank()) {
      Optional<Path> fromProperty = asExecutableOrContainingDirectory(property);
      if (fromProperty.isPresent()) {
        return fromProperty;
      }
    }

    for (String variable : ENV_VARIABLES) {
      String value = System.getenv(variable);
      if (value != null && !value.isBlank()) {
        Optional<Path> fromEnv = asExecutableOrContainingDirectory(value);
        if (fromEnv.isPresent()) {
          return fromEnv;
        }
      }
    }

    String pathVariable = System.getenv("PATH");
    if (pathVariable != null) {
      for (String entry : pathVariable.split(File.pathSeparator)) {
        if (entry.isBlank()) {
          continue;
        }
        Optional<Path> fromPath = findInDirectory(entry);
        if (fromPath.isPresent()) {
          return fromPath;
        }
      }
    }
    return Optional.empty();
  }

  private static Optional<Path> asExecutableOrContainingDirectory(String value) {
    try {
      Path path = Path.of(value);
      if (Files.isRegularFile(path)) {
        return Optional.of(path);
      }
      if (Files.isDirectory(path)) {
        return findInDirectory(value);
      }
    } catch (InvalidPathException ignored) {
      // malformed entry; keep looking
    }
    return Optional.empty();
  }

  private static Optional<Path> findInDirectory(String directory) {
    try {
      for (String name : EXECUTABLE_NAMES) {
        Path candidate = Path.of(directory, name);
        if (Files.isRegularFile(candidate)) {
          return Optional.of(candidate);
        }
      }
    } catch (InvalidPathException ignored) {
      // malformed entry; keep looking
    }
    return Optional.empty();
  }
}
