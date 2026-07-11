package com.intellij.plugins.haxe.hashlink;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Path/executable helpers shared by the HashLink run configuration and
 * runners. Output auto-detection reads the {@code -hl <out>.hl} argument from
 * the module's build (hxml file or compiler arguments, see
 * {@link HlBuildSniffer}) so a fresh configuration usually needs no manual
 * .hl path at all.
 */
final class HashLinkRunConfigurations {
  private HashLinkRunConfigurations() {
  }

  /** The HashLink executable, or an {@link ExecutionException} pointing at the SDK setting. */
  static Path resolveHlExecutable(@Nullable Module module) throws ExecutionException {
    return HlExecutableResolver.resolve(module)
      .orElseThrow(() -> new ExecutionException(HaxeBundle.message("haxe.run.bad.hl.bin.path")));
  }

  /**
   * The build-detected .hl output of the module, when the build declares one:
   * prefers a candidate that exists on disk, else the most likely location.
   */
  static Optional<Path> detectedOutput(Module module) {
    HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    HlBuildSniffer.HlBuild build;
    if (settings.isUseHxmlToBuild()) {
      Path hxml = resolveAgainstModule(module, settings.getHxmlPath());
      build = hxml != null ? HlBuildSniffer.fromHxml(hxml) : HlBuildSniffer.HlBuild.NONE;
    } else {
      build = HlBuildSniffer.fromArguments(settings.getArguments());
    }
    if (!build.hlBytecode() || build.output() == null) {
      return Optional.empty();
    }

    List<Path> candidates = outputCandidates(module, settings, build.output());
    return candidates.stream().filter(Files::isRegularFile).findFirst()
      .or(() -> candidates.stream().findFirst());
  }

  /** Resolves a possibly-relative user path against the module directory. */
  static @Nullable Path resolveAgainstModule(Module module, @Nullable String path) {
    if (path == null || path.isBlank()) {
      return null;
    }
    try {
      Path asPath = Path.of(path);
      if (asPath.isAbsolute()) {
        return asPath;
      }
      Path moduleDir = moduleDir(module);
      return moduleDir != null ? moduleDir.resolve(asPath) : asPath;
    } catch (InvalidPathException e) {
      return null;
    }
  }

  // A relative -hl output is resolved the way the compiler would: against the
  // compilation working directory (the module dir), with the hxml's own
  // directory as a fallback.
  private static List<Path> outputCandidates(Module module, HaxeModuleSettings settings, String output) {
    List<Path> candidates = new ArrayList<>();
    try {
      Path path = Path.of(output);
      if (path.isAbsolute()) {
        candidates.add(path);
        return candidates;
      }
      Path moduleDir = moduleDir(module);
      if (moduleDir != null) {
        candidates.add(moduleDir.resolve(path));
      }
      if (settings.isUseHxmlToBuild()) {
        Path hxml = resolveAgainstModule(module, settings.getHxmlPath());
        if (hxml != null && hxml.getParent() != null) {
          Path fromHxml = hxml.getParent().resolve(path);
          if (!candidates.contains(fromHxml)) {
            candidates.add(fromHxml);
          }
        }
      }
    } catch (InvalidPathException ignored) {
      // fall through with what we have
    }
    return candidates;
  }

  private static @Nullable Path moduleDir(Module module) {
    VirtualFile dir = ProjectUtil.guessModuleDir(module);
    return dir != null ? Path.of(dir.getPath()) : null;
  }
}
