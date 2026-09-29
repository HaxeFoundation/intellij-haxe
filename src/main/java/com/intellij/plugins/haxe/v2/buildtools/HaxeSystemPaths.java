package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.application.PathManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

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
