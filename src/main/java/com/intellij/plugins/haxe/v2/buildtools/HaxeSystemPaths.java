package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.application.PathManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Content-addressed directories for generated/extracted files under the IDE
 * system path: the directory is keyed by a hash of its inputs, so changed
 * inputs land in a fresh directory and unchanged ones reuse the previous one.
 */
public final class HaxeSystemPaths {

  private HaxeSystemPaths() {
  }

  /** {@code <ide-system>/haxe/<name>/<contentHash>}; nothing is created on disk. */
  @NotNull
  public static Path cacheDirectory(@NotNull String name, @NotNull String contentHash) {
    return Path.of(PathManager.getSystemPath(), "haxe", name, contentHash);
  }

  /**
   * Extracts the sources under the {@link #cacheDirectory} keyed by their content
   * and returns that root, the classpath to add. A file already there is left
   * alone: a plugin update with changed sources lands in a fresh directory and
   * an unchanged one reuses the previous extraction.
   */
  @NotNull
  public static Path extractSources(@NotNull String name, @NotNull Map<String, byte[]> sourcesByRelativePath) throws IOException {
    String contentHash = shortHash(sourcesByRelativePath.values().toArray(byte[][]::new));
    Path root = cacheDirectory(name, contentHash);
    for (Map.Entry<String, byte[]> source : sourcesByRelativePath.entrySet()) {
      Path target = root.resolve(source.getKey());
      if (Files.exists(target)) continue;
      Files.createDirectories(target.getParent());
      Files.write(target, source.getValue());
    }
    return root;
  }

  /** A 16-hex-character SHA-256 prefix over the concatenated content — the {@link #cacheDirectory} key. */
  @NotNull
  public static String shortHash(byte[]... content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (byte[] chunk : content) {
        digest.update(chunk);
      }
      return HexFormat.of().formatHex(digest.digest(), 0, 8);
    }
    catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
