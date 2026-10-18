package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.module.Module;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapRunConfigurationBase;
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

  /** The HashLink executable, or an {@link ExecutionException} naming the places to set it. */
  static Path resolveHlExecutable(Module module) throws ExecutionException {
    return HlExecutableResolver.resolve(module.getProject(), module)
      .orElseThrow(() -> new ExecutionException(HaxeDebuggerBundle.message("haxe.run.bad.hl.bin.path")));
  }

  /**
   * The build-detected .hl output of the module, when the build declares one:
   * prefers a candidate that exists on disk, else the most likely location.
   */
  static Optional<Path> detectedOutput(Module module) {
    HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);

    HlBuildSniffer.HlBuild build;
    if (settings.isUseHxmlToBuild()) {
      Path hxml = hxmlPath(module, settings);
      build = hxml != null ? HlBuildSniffer.fromHxml(hxml) : HlBuildSniffer.HlBuild.NONE;
    } else {
      build = HlBuildSniffer.fromArguments(settings.getArguments());
    }
    if (!build.hlBytecode() || build.output() == null) {
      return Optional.empty();
    }

    List<Path> candidates = outputCandidates(module, settings, build.output());
    return candidates.stream()
      .filter(Files::isRegularFile)
      .findFirst()
      .or(() -> candidates.stream().findFirst());
  }

  /** The hxml the module builds from, module-relative; null when it builds from arguments or names none. */
  private static @Nullable Path hxmlPath(Module module, HaxeModuleSettings settings) {
    String hxmlPath = settings.getHxmlPath();
    if (!settings.isUseHxmlToBuild() || hxmlPath == null || hxmlPath.isBlank()) {
      return null;
    }
    return DapRunConfigurationBase.resolveAgainstModule(module, module.getProject(), hxmlPath);
  }

  // A relative -hl output is resolved the way the compiler would: against the
  // compilation working directory (the module folder), with the hxml's own
  // directory as a fallback.
  private static List<Path> outputCandidates(Module module, HaxeModuleSettings settings, String output) {
    List<Path> candidates = new ArrayList<>();
    try {
      Path path = Path.of(output);
      if (path.isAbsolute()) {
        candidates.add(path);
        return candidates;
      }
      Path baseDir = DapRunConfigurationBase.baseDirectory(module, module.getProject());
      if (baseDir != null) {
        candidates.add(baseDir.resolve(path));
      }
      Path hxml = hxmlPath(module, settings);
      if (hxml != null && hxml.getParent() != null) {
        Path fromHxml = hxml.getParent().resolve(path);
        if (!candidates.contains(fromHxml)) {
          candidates.add(fromHxml);
        }
      }
    } catch (InvalidPathException ignored) {
      // fall through with the candidates collected so far
    }
    return candidates;
  }
}
