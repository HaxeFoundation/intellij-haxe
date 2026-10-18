package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import com.intellij.plugins.haxe.util.HaxeEnvironmentVariables;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/**
 * The runtimes a project needs beside the compiler and where each one is
 * looked up, in this order: the Build Tools override, the SDK's configured
 * path, the runtime's own environment variables, the PATH. Launches, SDK
 * creation and both settings panels (whose grayed defaults show what an
 * empty field inherits) all resolve through this one table.
 */
public enum HaxeRuntimeTool {
  HAXELIB("haxelib", HaxeSdkAdditionalDataBase::getHaxelibPath, HaxeBuildToolSettings::getHaxelibPath, List.of()) {
    /** haxelib ships beside the compiler: without a usable configured path the copy in the SDK home (or its bin) serves. */
    @Override
    @Nullable
    public String fromSdk(@Nullable Sdk sdk) {
      String configured = super.fromSdk(sdk);
      if (configured != null || sdk == null || sdk.getHomePath() == null) return configured;
      return HaxeSdkUtilBase.getHaxelibPathByFolderPath(sdk.getHomePath());
    }
  },
  // Neko's install folder, then its own search path (on Homebrew NEKOPATH names <prefix>/lib/neko, which holds no executable and is skipped)
  NEKO("neko", HaxeSdkAdditionalDataBase::getNekoBinPath, HaxeBuildToolSettings::getNekoPath,
       List.of(HaxeEnvironmentVariables.NEKO_INSTPATH, HaxeEnvironmentVariables.NEKOPATH)),
  // the variable names are class-qualified: the bare HASHLINK would name this constant
  HASHLINK("hl", HaxeSdkAdditionalDataBase::getHlBinPath, HaxeBuildToolSettings::getHashlinkPath,
           List.of(HaxeEnvironmentVariables.HASHLINK_BIN, HaxeEnvironmentVariables.HASHLINK, HaxeEnvironmentVariables.HASHLINKPATH)),
  NODE("node", HaxeSdkAdditionalDataBase::getNodeBinPath, HaxeBuildToolSettings::getNodePath, List.of());

  /** The process environment, replaceable in tests. */
  @FunctionalInterface
  public interface Environment {
    @Nullable
    String get(@NotNull String name);
  }

  public static final Environment PROCESS_ENVIRONMENT = System::getenv;

  private static final String PATH_VARIABLE = "PATH";

  private final String name;
  private final Function<HaxeSdkAdditionalDataBase, String> sdkConfiguredPath;
  private final Function<HaxeBuildToolSettings, String> override;
  private final List<String> environmentVariables;

  HaxeRuntimeTool(@NotNull String name,
                  @NotNull Function<HaxeSdkAdditionalDataBase, String> sdkConfiguredPath,
                  @NotNull Function<HaxeBuildToolSettings, String> override,
                  @NotNull List<String> environmentVariables) {
    this.name = name;
    this.sdkConfiguredPath = sdkConfiguredPath;
    this.override = override;
    this.environmentVariables = environmentVariables;
  }

  /** The executable's file name on this OS ({@code neko.exe} on Windows). */
  @NotNull
  public String executableName() {
    return HaxeSdkUtilBase.getExecutableName(name);
  }

  /** The project's Build Tools override, empty when unset. */
  @NotNull
  public String override(@NotNull Project project) {
    return override.apply(HaxeBuildToolSettings.getInstance(project));
  }

  /** The path configured on the SDK (the executable or its directory); null without SDK data or when blank. */
  @Nullable
  public String sdkConfiguredPath(@Nullable Sdk sdk) {
    if (sdk == null || !(sdk.getSdkAdditionalData() instanceof HaxeSdkData data)) return null;
    String configured = sdkConfiguredPath.apply(data);
    return configured == null || configured.isBlank() ? null : configured;
  }

  /** The executable the SDK's configured path names, itself or inside the directory; null when it does not exist. */
  @Nullable
  public String fromSdk(@Nullable Sdk sdk) {
    return executableOrInDirectory(sdkConfiguredPath(sdk));
  }

  /** The executable the tool's own variables name, every entry tried as the file itself and as a directory holding it, in table order. */
  @Nullable
  public String fromEnvironment(@NotNull Environment environment) {
    for (String variable : environmentVariables) {
      for (String entry : HaxeEnvironmentVariables.parsePathList(environment.get(variable))) {
        String found = executableOrInDirectory(entry);
        if (found != null) return found;
      }
    }
    return null;
  }

  /** The copy a PATH lookup finds (absolute); null when none. */
  @Nullable
  public String fromPath(@NotNull Environment environment) {
    String path = environment.get(PATH_VARIABLE);
    if (path == null) return null;
    File onPath = PathEnvironmentVariableUtil.findInPath(executableName(), path, null);
    return onPath != null ? onPath.getAbsolutePath() : null;
  }

  /** The environment's copy, else the PATH's: what an empty SDK field inherits, and what a new SDK starts with. */
  @Nullable
  public String fromEnvironmentOrPath(@NotNull Environment environment) {
    String fromEnvironment = fromEnvironment(environment);
    return fromEnvironment != null ? fromEnvironment : fromPath(environment);
  }

  /** The SDK's copy, else the environment's, else the PATH's: what an empty Build Tools override inherits. */
  @Nullable
  public String inheritedDefault(@Nullable Sdk sdk, @NotNull Environment environment) {
    return inheritedDefault(fromSdk(sdk), environment);
  }

  /**
   * The resolution for a launch: a non-blank Build Tools override decides
   * alone (null when it names nothing, so a typo fails visibly instead of
   * silently running another copy), else the SDK's executable, else the
   * environment's, else the PATH's.
   */
  @Nullable
  public String resolve(@Nullable String buildToolsOverride, @Nullable String sdkExecutable, @NotNull Environment environment) {
    if (buildToolsOverride != null && !buildToolsOverride.isBlank()) return executableOrInDirectory(buildToolsOverride);
    return inheritedDefault(sdkExecutable, environment);
  }

  /** The executable a configured value names, as the file itself or inside it as a directory; null when neither exists or the value is malformed. */
  @Nullable
  public String executableOrInDirectory(@Nullable String configuredPath) {
    if (configuredPath == null || configuredPath.isBlank()) return null;
    try {
      Path candidate = Path.of(configuredPath);
      if (Files.isRegularFile(candidate)) return candidate.toString();
      Path inDirectory = candidate.resolve(executableName());
      return Files.isRegularFile(inDirectory) ? inDirectory.toString() : null;
    }
    catch (InvalidPathException e) {
      return null;
    }
  }

  @Nullable
  private String inheritedDefault(@Nullable String sdkExecutable, @NotNull Environment environment) {
    return sdkExecutable != null ? sdkExecutable : fromEnvironmentOrPath(environment);
  }
}
