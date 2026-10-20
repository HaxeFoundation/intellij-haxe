package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.plugins.haxe.haxelib.HaxelibRepositoryLayout;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph.Requirement;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A library's haxelib.json as the sync reads it for the dependency graph. The
 * file sits at the root of the library's install directory, which is a
 * classpath root itself or one of its parents (a {@code classPath} entry in
 * the manifest puts the sources below it).
 */
@CustomLog
final class HaxelibManifest {

  /** How many directories above a classpath root the manifest may sit: the classPath entry is a short relative path. */
  private static final int ROOT_SEARCH_DEPTH = 4;
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private HaxelibManifest() {
  }

  /** The manifest governing the classpath roots, or null when none sits at or within a few levels above them. */
  @Nullable
  static Path find(@NotNull List<String> classpaths) {
    for (String classpath : classpaths) {
      Path directory = Path.of(classpath);
      for (int level = 0; level <= ROOT_SEARCH_DEPTH && directory != null; level++) {
        Path manifest = directory.resolve(HaxelibRepositoryLayout.MANIFEST_FILE_NAME);
        if (Files.isRegularFile(manifest)) return manifest;
        directory = directory.getParent();
      }
    }
    return null;
  }

  /** The dependencies the manifest declares, in file order; empty for an unreadable manifest or one without any. */
  @NotNull
  static List<Requirement> requirements(@NotNull Path manifest) {
    try {
      JsonNode dependencies = MAPPER.readTree(Files.readAllBytes(manifest)).path("dependencies");
      List<Requirement> requirements = new ArrayList<>();
      for (Map.Entry<String, JsonNode> dependency : dependencies.properties()) {
        String version = dependency.getValue().asString("").trim();
        requirements.add(new Requirement(dependency.getKey(), version.isEmpty() ? null : version));
      }
      return List.copyOf(requirements);
    } catch (IOException | RuntimeException e) {
      log.info("haxelib.json unreadable at " + manifest + ": " + e.getMessage());
      return List.of();
    }
  }
}
