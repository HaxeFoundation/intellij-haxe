package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Resolves tool executables for a project from the v2 build tool settings,
 * falling back to any registered Haxe SDK and finally the system PATH.
 */
public final class HaxeToolPathResolver {

  private HaxeToolPathResolver() {
  }

  /** Absolute path to the haxe compiler when resolvable, otherwise the bare executable name for PATH lookup. */
  @NotNull
  public static String resolveHaxeExecutable(@NotNull Project project) {
    return resolveHaxeExecutable(project, null);
  }

  /** Like {@link #resolveHaxeExecutable(Project)}, preferring the named SDK (a container's environment SDK). */
  @NotNull
  public static String resolveHaxeExecutable(@NotNull Project project, @Nullable String preferredSdkName) {
    Sdk sdk = findSdk(project, preferredSdkName);
    if (sdk != null && sdk.getHomePath() != null) {
      String compilerPath = HaxeSdkUtilBase.getCompilerPathByFolderPath(sdk.getHomePath());
      if (compilerPath != null) {
        return compilerPath;
      }
    }
    return HaxeSdkUtilBase.getExecutableName("haxe");
  }

  /** Absolute path to haxelib: the Build Tools setting when set, else the SDK's copy, else PATH lookup. */
  @NotNull
  public static String resolveHaxelibExecutable(@NotNull Project project) {
    return resolveHaxelibExecutable(project, null);
  }

  /** Like {@link #resolveHaxelibExecutable(Project)}, preferring the named SDK (a container's environment SDK). */
  @NotNull
  public static String resolveHaxelibExecutable(@NotNull Project project, @Nullable String preferredSdkName) {
    String configured = HaxeBuildToolSettings.getInstance(project).getHaxelibPath();
    if (!configured.isEmpty()) {
      return configured;
    }
    Sdk sdk = findSdk(project, preferredSdkName);
    if (sdk != null && sdk.getHomePath() != null) {
      String haxelibPath = HaxeSdkUtilBase.getHaxelibPathByFolderPath(sdk.getHomePath());
      if (haxelibPath != null) {
        return haxelibPath;
      }
    }
    return HaxeSdkUtilBase.getExecutableName("haxelib");
  }

  /**
   * Absolute path to the neko runtime: the Build Tools setting when set, else
   * the SDK's configured runtime, else a PATH lookup — the bare name only as
   * the last resort (run configurations validate the file exists, so a bare
   * name must have been genuinely unresolvable).
   */
  @NotNull
  public static String resolveNekoExecutable(@NotNull Project project, @Nullable String preferredSdkName) {
    String configured = HaxeBuildToolSettings.getInstance(project).getNekoPath();
    if (!configured.isEmpty()) {
      return configuredOverride(configured, "neko");
    }
    Sdk sdk = findSdk(project, preferredSdkName);
    if (sdk != null && sdk.getSdkAdditionalData() instanceof HaxeSdkData data) {
      Path fromSdk = executableOrInDirectory(data.getNekoBinPath(), "neko");
      if (fromSdk != null) {
        return fromSdk.toString();
      }
    }
    String onPath = pathDetectedExecutable("neko");
    return onPath != null ? onPath : HaxeSdkUtilBase.getExecutableName("neko");
  }

  /**
   * An explicit Build Tools override always WINS: a directory resolves to
   * the canonical executable inside it, and an unresolvable value comes back
   * verbatim — validation then fails on the configured value instead of a
   * typo silently running a different tool from the SDK or PATH.
   */
  @NotNull
  private static String configuredOverride(@NotNull String configured, @NotNull String executableName) {
    Path resolved = executableOrInDirectory(configured, executableName);
    return resolved != null ? resolved.toString() : configured;
  }

  /**
   * Path to the node runtime for js-target runs: the Build Tools override
   * when set (see {@link #configuredOverride}), else the SDK's configured
   * NodeJS, else a PATH lookup; null when nothing resolves (a js run then
   * reports the missing runtime instead of failing on a bare name).
   */
  @Nullable
  public static String resolveNodeExecutable(@NotNull Project project, @Nullable String preferredSdkName) {
    String configured = HaxeBuildToolSettings.getInstance(project).getNodePath();
    if (!configured.isEmpty()) {
      return configuredOverride(configured, "node");
    }
    Sdk sdk = findSdk(project, preferredSdkName);
    if (sdk != null && sdk.getSdkAdditionalData() instanceof HaxeSdkData data) {
      Path fromSdk = executableOrInDirectory(data.getNodeBinPath(), "node");
      if (fromSdk != null) {
        return fromSdk.toString();
      }
    }
    return pathDetectedExecutable("node");
  }

  /**
   * Absolute path to the standalone Flash player (projector) launching plain
   * swf runs and debug sessions: the Build Tools override when set, else the
   * SDK's configured player; null when neither resolves. The projector has
   * no canonical executable name, so there is no PATH fallback.
   */
  @Nullable
  public static String resolveFlashPlayerExecutable(@NotNull Project project, @Nullable String preferredSdkName) {
    String configured = HaxeBuildToolSettings.getInstance(project).getFlashPlayerPath();
    if (!configured.isEmpty()) {
      return configured;
    }
    Sdk sdk = findSdk(project, preferredSdkName);
    if (sdk != null && sdk.getSdkAdditionalData() instanceof HaxeSdkData data && !data.getFlashPlayerPath().isBlank()) {
      return data.getFlashPlayerPath();
    }
    return null;
  }

  /**
   * The Flex/AIR SDK name the flash-family lanes use: the Build Tools
   * override when set, else the Haxe SDK's runtimes reference, or null when
   * neither is set. The name points at an entry of the IDE's SDK table
   * (owned by the optional Flash plugin); callers resolve it there and
   * handle a vanished entry themselves.
   */
  @Nullable
  public static String resolveFlexSdkName(@NotNull Project project, @Nullable String preferredSdkName) {
    String configured = HaxeBuildToolSettings.getInstance(project).getFlexSdkName();
    if (!configured.isEmpty()) {
      return configured;
    }
    Sdk sdk = findSdk(project, preferredSdkName);
    if (sdk != null && sdk.getSdkAdditionalData() instanceof HaxeSdkData data && !data.getFlexSdkName().isEmpty()) {
      return data.getFlexSdkName();
    }
    return null;
  }

  /**
   * The effective Flex/AIR SDK name for a run configuration: the
   * configuration's own selection when non-blank, else
   * {@link #resolveFlexSdkName}; empty when neither resolves.
   */
  @NotNull
  public static String flexSdkNameOrEmpty(@NotNull Project project, @Nullable String override) {
    if (override != null && !override.isBlank()) {
      return override;
    }
    String resolved = resolveFlexSdkName(project, null);
    return resolved != null ? resolved : "";
  }

  /** What empty Build Tools overrides inherit, for display as grayed defaults. Null members mean nothing resolves. */
  public record InheritedRuntimeDefaults(@Nullable String haxelib,
                                         @Nullable String neko,
                                         @Nullable String hashlink,
                                         @Nullable String node,
                                         @Nullable String flashPlayer,
                                         @Nullable String flexSdkName) {
  }

  /**
   * The values empty Build Tools overrides would inherit from the named SDK
   * (or, with no name, the first registered Haxe SDK - the same fallback the
   * resolve methods use), with executables falling back to a PATH lookup.
   * Display-only: computed against the panel's CURRENT selection, which may
   * not be applied yet.
   */
  @NotNull
  public static InheritedRuntimeDefaults inheritedRuntimeDefaults(@Nullable String sdkName) {
    Sdk sdk = sdkName != null ? ProjectJdkTable.getInstance().findJdk(sdkName) : null;
    if (sdk == null) {
      List<Sdk> haxeSdks = ProjectJdkTable.getInstance().getSdksOfType(HaxeSdkType.getInstance());
      sdk = haxeSdks.isEmpty() ? null : haxeSdks.getFirst();
    }
    HaxeSdkData data = sdk != null && sdk.getSdkAdditionalData() instanceof HaxeSdkData sdkData ? sdkData : null;

    String haxelib = sdk != null && sdk.getHomePath() != null
                     ? HaxeSdkUtilBase.getHaxelibPathByFolderPath(sdk.getHomePath())
                     : null;
    if (haxelib == null) {
      haxelib = pathDetectedExecutable("haxelib");
    }
    String flashPlayer = data == null || data.getFlashPlayerPath().isBlank() ? null : data.getFlashPlayerPath();
    String flexSdkName = data == null || data.getFlexSdkName().isEmpty() ? null : data.getFlexSdkName();
    return new InheritedRuntimeDefaults(
      haxelib,
      inheritedExecutable(data == null ? null : data.getNekoBinPath(), "neko"),
      inheritedExecutable(data == null ? null : data.getHlBinPath(), "hl"),
      inheritedExecutable(data == null ? null : data.getNodeBinPath(), "node"),
      flashPlayer,
      flexSdkName);
  }

  @Nullable
  private static String inheritedExecutable(@Nullable String sdkConfiguredPath, @NotNull String executableName) {
    Path fromSdk = executableOrInDirectory(sdkConfiguredPath, executableName);
    return fromSdk != null ? fromSdk.toString() : pathDetectedExecutable(executableName);
  }

  /** The executable a PATH lookup picks (absolute), or null; also displayed as an inherited default in the settings panels. */
  @Nullable
  public static String pathDetectedExecutable(@NotNull String executableName) {
    File onPath = PathEnvironmentVariableUtil.findInPath(HaxeSdkUtilBase.getExecutableName(executableName));
    return onPath != null ? onPath.getAbsolutePath() : null;
  }

  /**
   * Absolute path to the Flex/AIR SDK's adl (the AIR debug launcher hosting
   * flash-family test runs): from the resolved Flex/AIR SDK entry's bin,
   * else an AIR_SDK environment variable's bin (the same variable the lime
   * air builds consume); null when neither yields an existing executable.
   */
  @Nullable
  public static String resolveAdlExecutable(@NotNull Project project) {
    String flexSdkName = resolveFlexSdkName(project, null);
    Sdk sdk = flexSdkName == null ? null : ProjectJdkTable.getInstance().findJdk(flexSdkName);
    if (sdk != null && sdk.getHomePath() != null) {
      Path adl = adlInSdk(sdk.getHomePath());
      if (Files.isRegularFile(adl)) {
        return adl.toString();
      }
    }
    String airSdkHome = System.getenv("AIR_SDK");
    if (airSdkHome != null && !airSdkHome.isBlank()) {
      Path adl = adlInSdk(airSdkHome);
      if (Files.isRegularFile(adl)) {
        return adl.toString();
      }
    }
    return null;
  }

  /** Where a Flex/AIR SDK keeps its adl, whether or not it exists. */
  @NotNull
  public static Path adlInSdk(@NotNull String sdkHome) {
    return Path.of(sdkHome, "bin", HaxeSdkUtilBase.getExecutableName("adl"));
  }

  /** The configured value may point at the executable itself or its directory. */
  @Nullable
  private static Path executableOrInDirectory(@Nullable String configuredPath, @NotNull String executableName) {
    if (configuredPath == null || configuredPath.isBlank()) return null;
    Path candidate = Path.of(configuredPath);
    if (Files.isRegularFile(candidate)) return candidate;
    Path inDirectory = candidate.resolve(HaxeSdkUtilBase.getExecutableName(executableName));
    return Files.isRegularFile(inDirectory) ? inDirectory : null;
  }

  @Nullable
  private static Sdk findSdk(@NotNull Project project, @Nullable String preferredSdkName) {
    if (preferredSdkName != null) {
      Sdk preferred = ProjectJdkTable.getInstance().findJdk(preferredSdkName);
      if (preferred != null) {
        return preferred;
      }
    }
    return findConfiguredSdk(project);
  }

  /**
   * The SDK the container's tool invocations run with: its Environment SDK when
   * registered, else the Build Tools SDK, else any registered Haxe SDK — the
   * same fallback chain the resolve*Executable methods use. Contrast
   * {@link #effectiveSdkName}, which reports what is CONFIGURED; this reports
   * what will actually be USED.
   */
  @Nullable
  public static Sdk resolveSdk(@NotNull Project project, @NotNull String containerId) {
    return findSdk(project, HaxeEnvironmentStore.getInstance(project).getSdkName(containerId));
  }

  /**
   * The SDK a module compiles and resolves against: its Environment SDK when
   * set, else the project-wide one from Build Tools | Haxe. Null when neither
   * is configured — the editor then asks the user to pick one. This is the
   * single authority; nothing consults the Project SDK. For the SDK a tool
   * process should actually run with (permissive fallbacks), use
   * {@link #resolveSdk}.
   */
  @Nullable
  public static String effectiveSdkName(@NotNull Project project, @NotNull String containerId) {
    String environmentSdk = HaxeEnvironmentStore.getInstance(project).getSdkName(containerId);
    if (environmentSdk != null && ProjectJdkTable.getInstance().findJdk(environmentSdk) != null) {
      return environmentSdk;
    }
    String settingsSdk = HaxeBuildToolSettings.getInstance(project).getSdkName();
    return settingsSdk != null && ProjectJdkTable.getInstance().findJdk(settingsSdk) != null ? settingsSdk : null;
  }

  /** The SDK selected in Build Tools | Haxe, or the first registered Haxe SDK, or null. */
  @Nullable
  public static Sdk findConfiguredSdk(@NotNull Project project) {
    String sdkName = HaxeBuildToolSettings.getInstance(project).getSdkName();
    if (sdkName != null) {
      Sdk sdk = ProjectJdkTable.getInstance().findJdk(sdkName);
      if (sdk != null) {
        return sdk;
      }
    }
    List<Sdk> haxeSdks = ProjectJdkTable.getInstance().getSdksOfType(HaxeSdkType.getInstance());
    return haxeSdks.isEmpty() ? null : haxeSdks.getFirst();
  }
}
