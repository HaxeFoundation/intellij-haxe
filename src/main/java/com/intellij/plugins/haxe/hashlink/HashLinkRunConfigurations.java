package com.intellij.plugins.haxe.hashlink;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.compilation.HaxeCompilerUtil;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Shared gating and path logic for the (experimental) HashLink runners.
 *
 * The HashLink runners are registered with {@code order="first"} and claim a
 * run configuration ONLY when this gate matches, so the pre-existing generic
 * runners keep handling every other configuration untouched.
 *
 * There is no dedicated "HashLink" configuration type: users create a normal
 * Haxe Application configuration, and these runners engage when the module's
 * build produces HashLink bytecode — via the target dropdown, the compiler
 * arguments, or the {@code -hl <out>.hl} line inside an hxml build file
 * (see {@link HlBuildSniffer}).
 */
final class HashLinkRunConfigurations {
  private HashLinkRunConfigurations() {
  }

  /**
   * True when the profile is a plain Haxe application configuration whose
   * build produces HashLink bytecode. NME/OpenFL builds and custom-executable
   * configurations stay with the generic runners.
   */
  static boolean isHashLinkConfiguration(RunProfile profile) {
    if (!(profile instanceof HaxeApplicationConfiguration configuration)) {
      return false;
    }
    if (configuration.isCustomExecutable()) {
      return false;
    }
    Module module = configuration.getConfigurationModule().getModule();
    if (module == null) {
      return false;
    }
    HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    if (settings.isUseNmmlToBuild() || settings.isUseOpenFLToBuild()) {
      return false;
    }
    return hlBuild(module, settings).hlBytecode()
           || settings.getCompilationTarget() == HaxeTarget.HL;
  }

  /** What the module's build produces, from the hxml file or the arguments field. */
  static HlBuildSniffer.HlBuild hlBuild(Module module, HaxeModuleSettings settings) {
    if (settings.isUseHxmlToBuild()) {
      Path hxml = resolveAgainstModule(module, settings.getHxmlPath());
      return hxml != null ? HlBuildSniffer.fromHxml(hxml) : HlBuildSniffer.HlBuild.NONE;
    }
    return HlBuildSniffer.fromArguments(settings.getArguments());
  }

  /**
   * The .hl file to execute: the custom-file override, the {@code -hl} output
   * from the build (resolved against the module and hxml directories), or the
   * calculated compiler output for settings-based builds.
   */
  static Path resolveHlOutput(HaxeApplicationConfiguration configuration, Module module) throws ExecutionException {
    FileDocumentManager.getInstance().saveAllDocuments();
    List<String> tried = new ArrayList<>();

    if (configuration.isCustomFileToLaunch()) {
      Path custom = existingFile(configuration.getCustomFileToLaunchPath());
      if (custom != null) {
        return custom;
      }
      tried.add(String.valueOf(configuration.getCustomFileToLaunchPath()));
      throw outputMissing(tried);
    }

    HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    HlBuildSniffer.HlBuild build = hlBuild(module, settings);
    if (build.output() != null) {
      for (Path candidate : outputCandidates(module, settings, build.output())) {
        if (Files.isRegularFile(candidate)) {
          return candidate;
        }
        tried.add(candidate.toString());
      }
    } else if (!settings.isUseHxmlToBuild()) {
      // settings-based build: the plugin can compute the output location
      String calculated = HaxeCompilerUtil.calculateCompilerOutput(module);
      Path fromSettings = existingFile(calculated);
      if (fromSettings != null) {
        return fromSettings;
      }
      tried.add(String.valueOf(calculated));
    }
    throw outputMissing(tried);
  }

  /** The HashLink executable, or an {@link ExecutionException} pointing at the SDK setting. */
  static Path resolveHlExecutable(@Nullable Module module) throws ExecutionException {
    return HlExecutableResolver.resolve(module)
      .orElseThrow(() -> new ExecutionException(HaxeBundle.message("haxe.run.bad.hl.bin.path")));
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

  private static @Nullable Path resolveAgainstModule(Module module, @Nullable String path) {
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

  private static @Nullable Path moduleDir(Module module) {
    VirtualFile dir = ProjectUtil.guessModuleDir(module);
    return dir != null ? Path.of(dir.getPath()) : null;
  }

  private static @Nullable Path existingFile(@Nullable String path) {
    if (path == null || path.isBlank()) {
      return null;
    }
    try {
      Path asPath = Path.of(path);
      return Files.isRegularFile(asPath) ? asPath : null;
    } catch (InvalidPathException e) {
      return null;
    }
  }

  private static ExecutionException outputMissing(List<String> tried) {
    return new ExecutionException(
      HaxeBundle.message("haxe.run.hl.output.missing", String.join(", ", tried.isEmpty() ? List.of("<none>") : tried)));
  }
}
