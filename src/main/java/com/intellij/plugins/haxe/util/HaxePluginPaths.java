package com.intellij.plugins.haxe.util;

import com.intellij.openapi.application.PathManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Locates files shipped inside the plugin's installation directory.
 * PluginManagerCore.getPlugin is @ApiStatus.Internal, so the plugin home is
 * derived from this class's own jar ({@code <plugin>/lib/<jar>}) instead.
 */
public final class HaxePluginPaths {

  private HaxePluginPaths() {
  }

  /** The plugin's installation directory, or null when it cannot be derived (exploded test classpath). */
  @Nullable
  public static Path pluginHome() {
    Path jar = PathManager.getJarForClass(HaxePluginPaths.class);
    Path lib = jar != null ? jar.getParent() : null;
    return lib != null ? lib.getParent() : null;
  }

  /** A file shipped under the plugin directory, or null when the plugin home cannot be derived or the file is missing. */
  @Nullable
  public static Path bundledFile(@NotNull String relativePath) {
    Path home = pluginHome();
    Path file = home != null ? home.resolve(relativePath) : null;
    return file != null && Files.isRegularFile(file) ? file : null;
  }
}
