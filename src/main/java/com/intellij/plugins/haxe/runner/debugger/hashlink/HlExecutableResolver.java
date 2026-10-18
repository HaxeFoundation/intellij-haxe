package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.plugins.haxe.v2.buildtools.HaxeRuntimeTool.Environment;
import com.intellij.plugins.haxe.v2.buildtools.HaxeRuntimeTool;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The HashLink executable for the HashLink run/debug support, through
 * {@link HaxeRuntimeTool#HASHLINK}: the Build Tools override decides alone
 * (an unresolvable value yields nothing, so a typo fails visibly instead of
 * silently running another hl), else the module's SDK path, the
 * HASHLINK_BIN/HASHLINK/HASHLINKPATH variables, then PATH. The environment is
 * injectable so the precedence is unit-testable.
 */
public final class HlExecutableResolver {

  private HlExecutableResolver() {
  }

  /** Resolves with the project's Build Tools override first, then the module's SDK path (no module: no SDK path). */
  public static Optional<Path> resolve(@NotNull Project project, @Nullable Module module) {
    String buildToolsOverride = HaxeBuildToolSettings.getInstance(project).getHashlinkPath();
    return resolve(buildToolsOverride, sdkHlBinPath(module), HaxeRuntimeTool.PROCESS_ENVIRONMENT);
  }

  /** Testable core without a Build Tools override: SDK path, then environment, then PATH. */
  public static Optional<Path> resolve(@Nullable String sdkHlBinPath, @NotNull Environment env) {
    return resolve(null, sdkHlBinPath, env);
  }

  /** Testable core: a non-blank override decides alone; else SDK path (the executable or its directory), then environment, then PATH. */
  public static Optional<Path> resolve(@Nullable String buildToolsOverride, @Nullable String sdkHlBinPath, @NotNull Environment env) {
    String sdkExecutable = HaxeRuntimeTool.HASHLINK.executableOrInDirectory(sdkHlBinPath);
    String resolved = HaxeRuntimeTool.HASHLINK.resolve(buildToolsOverride, sdkExecutable, env);
    return Optional.ofNullable(resolved).map(Path::of);
  }

  @Nullable
  private static String sdkHlBinPath(@Nullable Module module) {
    if (module == null) return null;
    Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
    return HaxeRuntimeTool.HASHLINK.sdkConfiguredPath(sdk);
  }
}
