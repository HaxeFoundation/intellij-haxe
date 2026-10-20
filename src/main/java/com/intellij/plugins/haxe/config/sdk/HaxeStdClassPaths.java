package com.intellij.plugins.haxe.config.sdk;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.util.HaxeEnvironmentVariables;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The folders the Haxe compiler searches for the standard library, in the compiler's own order: every
 * {@code HAXE_STD_PATH} entry when the variable is set - nothing else, even when no entry exists -
 * otherwise folders relative to the compiler's directory. The {@code extraLibs} folders the compiler
 * adds beside them hold no standard library and are left out.
 */
public final class HaxeStdClassPaths {
  /** Tried under the prefix (the compiler directory's parent) on Unix, before {@code <compiler directory>/std}. */
  private static final List<String> UNIX_PREFIX_STD_FOLDERS = List.of("lib/haxe/std", "share/haxe/std");

  private HaxeStdClassPaths() {
  }

  /**
   * The candidate std folders in search order, existing or not; duplicates dropped, order kept.
   *
   * @param haxeStdPathValue  the {@code HAXE_STD_PATH} value, {@code null} when the variable is unset
   * @param compilerDirectory the compiler's directory as {@link #compilerDirectory} resolves it
   * @param unix              whether the compiler runs on a Unix-like OS (Linux, macOS)
   */
  @NotNull
  public static Set<String> candidates(@Nullable String haxeStdPathValue, @NotNull Path compilerDirectory, boolean unix) {
    Set<String> candidates = new LinkedHashSet<>();
    if (haxeStdPathValue != null) {
      candidates.addAll(HaxeEnvironmentVariables.parsePathList(haxeStdPathValue));
      return candidates;
    }

    if (unix) {
      // dirname of "/" is "/" for the compiler as well
      Path prefix = Objects.requireNonNullElse(compilerDirectory.getParent(), compilerDirectory);
      for (String folder : UNIX_PREFIX_STD_FOLDERS) {
        candidates.add(systemIndependentPath(prefix.resolve(folder)));
      }
    }
    candidates.add(systemIndependentPath(compilerDirectory.resolve("std")));
    return candidates;
  }

  /**
   * The directory the compiler resolves its default folders against. Linux names the running executable
   * through {@code /proc/self/exe}, which already resolves symlinks; macOS reports the path it was launched
   * by. On Unix the directory then goes through {@code realpath}; on Windows only through a case
   * normalisation that the IDE's case-insensitive file lookup does not need. A path that cannot be
   * resolved is kept as it is, as the compiler does.
   */
  @NotNull
  public static Path compilerDirectory(@NotNull Path haxeExecutable) {
    Path executable = SystemInfo.isLinux ? realPathOrSelf(haxeExecutable) : haxeExecutable;
    Path directory = executable.toAbsolutePath()
      .normalize()
      .getParent();
    return SystemInfo.isWindows ? directory : realPathOrSelf(directory);
  }

  @NotNull
  private static Path realPathOrSelf(@NotNull Path path) {
    try {
      return path.toRealPath();
    }
    catch (IOException e) {
      return path;
    }
  }

  @NotNull
  private static String systemIndependentPath(@NotNull Path path) {
    return FileUtil.toSystemIndependentName(path.toString());
  }
}
