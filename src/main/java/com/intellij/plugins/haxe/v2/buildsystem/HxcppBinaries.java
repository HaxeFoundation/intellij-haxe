package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Hxcpp's binary naming inside a plain-hxml {@code -cpp} output directory:
 * {@code <MainClassSimpleName>[-debug][.exe]} — the base name is the build's
 * main class simple name, a {@code -debug} compile appends {@code -debug},
 * and Windows adds the {@code .exe} extension (linux/mac binaries have none).
 * The flavor is decided by the COMPILE ARGUMENTS, never by what sits on disk:
 * a stale leftover of the other flavor must not be launched.
 */
public final class HxcppBinaries {

  private HxcppBinaries() {
  }

  @NotNull
  public static Path binary(@NotNull Path outputDirectory, @NotNull String mainClassSimpleName, boolean debugBuild) {
    String baseName = debugBuild ? mainClassSimpleName + "-debug" : mainClassSimpleName;
    return outputDirectory.resolve(HaxeSdkUtilBase.getExecutableName(baseName));
  }
}
