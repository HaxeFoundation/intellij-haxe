package com.intellij.plugins.haxe.matrix;

import java.nio.file.Files;
import java.nio.file.Path;

/** OS-specific naming for the tool: executables, wrapper script, archives. */
final class Platform {
  static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");

  private Platform() {
  }

  static String exe(String name) {
    return WINDOWS ? name + ".exe" : name;
  }

  static String gradlew() {
    return WINDOWS ? "gradlew.bat" : "gradlew";
  }

  /** The haxe/hl binary inside a provisioned directory, or null if absent. */
  static Path findBinary(Path dir, String name) {
    if (dir == null || !Files.isDirectory(dir)) {
      return null;
    }
    Path direct = binaryAt(dir, name);
    if (direct != null) {
      return direct;
    }
    // archives usually contain one top-level folder; look one level down
    try (var children = Files.list(dir)) {
      return children.filter(Files::isDirectory)
        .map(sub -> binaryAt(sub, name))
        .filter(java.util.Objects::nonNull)
        .findFirst()
        .orElse(null);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Windows archives keep the binary at the archive root; the linux
   * node/HashLink layouts put it under bin/ — both spots count.
   */
  private static Path binaryAt(Path dir, String name) {
    Path direct = dir.resolve(exe(name));
    if (Files.isRegularFile(direct)) {
      return direct;
    }
    Path inBin = dir.resolve("bin").resolve(exe(name));
    return Files.isRegularFile(inBin) ? inBin : null;
  }
}
