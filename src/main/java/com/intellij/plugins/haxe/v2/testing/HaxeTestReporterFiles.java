package com.intellij.plugins.haxe.v2.testing;

import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.plugins.haxe.v2.buildtools.HaxeSystemPaths;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts a framework's shipped live-reporter Haxe sources (see the READMEs
 * under {@code resources/testing/}) into the IDE system directory, so test
 * compiles can add them via {@code -cp} and inject the framework's reporter
 * with its {@code --macro} entry point. The sources every reporter shares
 * ({@code sharedLiveReporter}) are extracted alongside, so the one classpath
 * root serves both. The target directory is keyed by a content hash: a
 * plugin update with changed sources lands in a fresh directory, and an
 * unchanged one reuses the previous extraction.
 */
@CustomLog
final class HaxeTestReporterFiles {

  /** Sources every framework's reporter calls into; extracted next to the framework's own files. */
  private static final String SHARED_ROOT = "/testing/sharedLiveReporter/";
  private static final List<String> SHARED_FILES =
    List.of("intellij_haxe_test/TcOutput.hx", "intellij_haxe_test/FlashSupport.hx");

  private HaxeTestReporterFiles() {
  }

  /** The reporter sources' extracted classpath root, or null when extraction fails (the run then reports without them). */
  @Nullable
  static String classpath(@NotNull String resourceRoot,
                          @NotNull String directoryName,
                          @NotNull List<String> sourceFiles) {
    try {
      Map<String, byte[]> sources = new LinkedHashMap<>();
      for (String file : sourceFiles) {
        sources.put(file, readResource(resourceRoot, file));
      }
      for (String file : SHARED_FILES) {
        sources.put(file, readResource(SHARED_ROOT, file));
      }
      String contentHash = HaxeSystemPaths.shortHash(sources.values().toArray(byte[][]::new));

      Path root = HaxeSystemPaths.cacheDirectory(directoryName, contentHash);
      for (Map.Entry<String, byte[]> source : sources.entrySet()) {
        Path target = root.resolve(source.getKey());
        if (!Files.exists(target)) {
          Files.createDirectories(target.getParent());
          Files.write(target, source.getValue());
        }
      }
      return root.toString();
    }
    catch (IOException | RuntimeException e) {
      log.warn("test reporter extraction failed for " + resourceRoot, e);
      return null;
    }
  }

  @NotNull
  private static byte[] readResource(@NotNull String resourceRoot, @NotNull String relativePath) throws IOException {
    try (InputStream stream = HaxeTestReporterFiles.class.getResourceAsStream(resourceRoot + relativePath)) {
      if (stream == null) {
        throw new IOException("missing resource " + resourceRoot + relativePath);
      }
      return StreamUtil.readBytes(stream);
    }
  }
}
