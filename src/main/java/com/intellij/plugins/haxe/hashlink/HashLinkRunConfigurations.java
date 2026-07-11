package com.intellij.plugins.haxe.hashlink;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.compilation.HaxeCompilerUtil;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.Nullable;

/**
 * Shared gating and path logic for the (experimental) HashLink runners.
 *
 * The HashLink runners are registered with {@code order="first"} and claim a
 * run configuration ONLY when this gate matches, so the pre-existing generic
 * runners keep handling every other configuration untouched.
 */
final class HashLinkRunConfigurations {
  private HashLinkRunConfigurations() {
  }

  /**
   * True when the profile is a plain Haxe application configuration whose
   * compilation target is HashLink. NME/OpenFL builds and custom-executable
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
    return settings.getCompilationTarget() == HaxeTarget.HL;
  }

  /** The .hl file to execute: the custom-file override, or the module's compiler output. */
  static Path resolveHlOutput(HaxeApplicationConfiguration configuration, Module module) throws ExecutionException {
    FileDocumentManager.getInstance().saveAllDocuments();
    String path = configuration.isCustomFileToLaunch()
                  ? configuration.getCustomFileToLaunchPath()
                  : HaxeCompilerUtil.calculateCompilerOutput(module);
    if (path == null || path.isBlank() || !Files.isRegularFile(Path.of(path))) {
      throw new ExecutionException(HaxeBundle.message("haxe.run.hl.output.missing", String.valueOf(path)));
    }
    return Path.of(path);
  }

  /** The HashLink executable, or an {@link ExecutionException} pointing at the SDK setting. */
  static Path resolveHlExecutable(@Nullable Module module) throws ExecutionException {
    return HlExecutableResolver.resolve(module)
      .orElseThrow(() -> new ExecutionException(HaxeBundle.message("haxe.run.bad.hl.bin.path")));
  }
}
