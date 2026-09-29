package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlArguments;
import com.intellij.plugins.haxe.v2.buildsystem.ProjectXmlParser;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetOptions;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetSelectionStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facts about lime-family projects (lime/openfl xml and lime HXP scripts):
 * which build-file types the lime tool owns, which CLI tool builds them and
 * the commands it accepts. The hxml counterpart is {@link HxmlProjects}.
 */
public final class LimeProjects {

  private static final Logger LOG = Logger.getInstance(LimeProjects.class);

  /** The lime tool's built-in actions, offered for every lime-family build file. */
  public static final List<String> DEFAULT_ACTIONS = List.of("test", "run", "build", "clean");

  /** The default action a lime-family file compiles with. */
  public static final String BUILD_ACTION = "build";

  /** The target flags whose packaged app is a host-launchable binary (see {@link #packagedBinary}). */
  public static final Set<String> HOST_LAUNCHABLE_TARGETS = Set.of("neko", "hl", "cpp", "windows", "linux", "mac");

  /** The target flags whose packaged artifact is a swf, hosted under adl for test runs. */
  public static final Set<String> FLASH_FAMILY_TARGETS = Set.of("flash", "air");

  /** The target flags whose packaged app runs in a BROWSER page, served and console-captured for test runs. */
  public static final Set<String> BROWSER_TARGETS = Set.of("html5");

  /** The app file every lime platform falls back to when the project xml declares none. */
  private static final String DEFAULT_APP_FILE = "MyApplication";

  private static final String AIR_SDK_DEFINE_PREFIX = "-DAIR_SDK=";

  private LimeProjects() {
  }

  /** The {@code <app file>} name the tool packages under: the declared one, else lime's default. */
  @NotNull
  public static String appFile(@NotNull String content) {
    return appFileOrDefault(ProjectXmlParser.parseAppFile(content));
  }

  /** The declared {@code <app file>} name, or lime's default when the project xml declares none. */
  @NotNull
  private static String appFileOrDefault(@Nullable String declaredAppFile) {
    return StringUtil.defaultIfEmpty(declaredAppFile, DEFAULT_APP_FILE);
  }

  /** True for the types the lime tool can build; a plain hxp SCRIPT is not one of them. */
  public static boolean isLimeFamily(@Nullable HaxeBuildFileType type) {
    return type == HaxeBuildFileType.OPENFL || type == HaxeBuildFileType.LIME || type == HaxeBuildFileType.HXP_PROJECT;
  }

  /** The CLI tool that builds the file: openfl projects go through the openfl wrapper, everything else through lime. */
  @NotNull
  public static String toolFor(@NotNull HaxeBuildFileType type) {
    return type == HaxeBuildFileType.OPENFL ? "openfl" : "lime";
  }

  /** The file's currently selected lime target as the tool's target flag (e.g. "html5", "windows"). */
  @NotNull
  public static String selectedTargetFlag(@NotNull Project project,
                                          @NotNull HaxeBuildFileType type,
                                          @NotNull VirtualFile file) {
    return HaxeTargetOptions.targetFlagFor(type, HaxeTargetSelectionStore.getInstance(project).getSelectedTargetId(file));
  }

  /** The selected target's full flag list — the target word plus configured extras (e.g. "-64"). */
  @NotNull
  public static List<String> selectedTargetFlags(@NotNull Project project,
                                                 @NotNull HaxeBuildFileType type,
                                                 @NotNull VirtualFile file) {
    return HaxeTargetOptions.targetFlagsFor(type, HaxeTargetSelectionStore.getInstance(project).getSelectedTargetId(file));
  }

  /// Whether the command is a `haxelib run lime|openfl …` invocation. The lime
  /// tool treats `--connect` as a haxe flag and forwards it together with the
  /// port after it into the haxe builds it generates, so these commands can
  /// use the compilation server too.
  public static boolean isToolCommand(@NotNull List<String> command) {
    if (command.size() < 3 || !"run".equals(command.get(1))) return false;
    String tool = command.get(2);
    return tool.equals("lime") || tool.equals("openfl");
  }

  /** One of the tool's actions as a full command line, using the file's selected target (all its flags). */
  @NotNull
  public static List<String> actionCommand(@NotNull Project project,
                                           @Nullable String environmentSdk,
                                           @NotNull VirtualFile file,
                                           @NotNull HaxeBuildFileType type,
                                           @NotNull String actionName) {
    String haxelib = HaxeToolPathResolver.resolveHaxelibExecutable(project, environmentSdk);
    List<String> command = new ArrayList<>(List.of(haxelib, "run", toolFor(type), actionName, file.getName()));
    List<String> targetFlags = selectedTargetFlags(project, type, file);
    command.addAll(targetFlags);
    command.addAll(airSdkDefine(project, targetFlags));
    return command;
  }

  /**
   * The air target's packaging and launch tools live in the AIR SDK, which
   * the lime tool locates through the AIR_SDK define - without it every air
   * action fails with "You must define AIR_SDK". The configured Flex/AIR SDK
   * entry's home supplies it; a CLI define lands after the project xml's, so
   * the IDE's SDK selection wins over a value hardcoded there. Attached -D
   * spelling: the two-word form trips a lime bug duplicating the value.
   */
  @NotNull
  private static List<String> airSdkDefine(@NotNull Project project, @NotNull List<String> targetFlags) {
    if (!targetFlags.contains("air")) return List.of();
    String flexSdkName = HaxeToolPathResolver.resolveFlexSdkName(project, null);
    Sdk sdk = flexSdkName == null ? null : ProjectJdkTable.getInstance().findJdk(flexSdkName);
    if (sdk == null || sdk.getHomePath() == null) return List.of();
    return List.of(AIR_SDK_DEFINE_PREFIX + sdk.getHomePath());
  }

  /**
   * The file's effective haxe arguments from the tool's display mode
   * ({@code haxelib run lime|openfl display <file> <target>}), prefixed with
   * {@code --cwd <project dir>} so relative paths resolve against the
   * project. Null when the tool fails. Spawns a process - never call under
   * the read lock. The output can ECHO arguments injected into earlier
   * builds (the tool persists CLI extras in its export state) - consumers
   * appending their own arguments must skip verbatim duplicates.
   */
  @Nullable
  public static List<String> displayArguments(@NotNull String haxelibExecutable,
                                              @NotNull String tool,
                                              @NotNull String directory,
                                              @NotNull String fileName,
                                              @NotNull String targetFlag,
                                              int timeoutMs) {
    GeneralCommandLine commandLine = new GeneralCommandLine()
      .withExePath(haxelibExecutable)
      .withParameters("run", tool, "display", fileName, targetFlag)
      .withWorkDirectory(directory);
    try {
      ProcessOutput output = new CapturingProcessHandler(commandLine).runProcess(timeoutMs);
      if (output.isTimeout()) {
        LOG.warn(tool + " display timed out after " + timeoutMs + "ms for " + fileName + " " + targetFlag);
        return null;
      }
      if (output.getExitCode() != 0) {
        String stderr = StringUtil.trimLog(output.getStderr(), 500);
        LOG.warn(tool + " display failed (exit " + output.getExitCode() + ") for " + fileName + " " + targetFlag + ": " + stderr);
        return null;
      }
      List<String> arguments = HxmlArguments.parseLines(output.getStdoutLines());
      if (arguments.isEmpty()) {
        LOG.warn(tool + " display produced no haxe arguments for " + fileName + " " + targetFlag);
        return null;
      }
      List<String> withCwd = new ArrayList<>(List.of("--cwd", directory));
      withCwd.addAll(arguments);
      return List.copyOf(withCwd);
    }
    catch (ExecutionException e) {
      LOG.warn(tool + " display could not be spawned for " + fileName + ": " + e.getMessage());
      return null;
    }
  }

  /**
   * Environment additions a resolved tool command needs when spawned: lime
   * 8.3.2's air packaging GUARDS on the AIR_SDK define but READS the value
   * from project.environment - only a real environment variable lands in
   * both maps, so the CLI define alone crashes the packaging step on a null
   * path. The injected define (see {@link #airSdkDefine}) is mirrored into
   * the process environment; commands without it get no additions.
   */
  @NotNull
  public static Map<String, String> commandEnvironment(@NotNull List<String> command) {
    for (String argument : command) {
      if (argument.startsWith(AIR_SDK_DEFINE_PREFIX)) {
        return Map.of("AIR_SDK", argument.substring(AIR_SDK_DEFINE_PREFIX.length()));
      }
    }
    return Map.of();
  }

  /** Whether the flag's tests run under an IDE-provided host (adl for the flash family, a served browser page for html5). */
  public static boolean isHostedTarget(@NotNull String targetFlag) {
    return FLASH_FAMILY_TARGETS.contains(targetFlag) || BROWSER_TARGETS.contains(targetFlag);
  }

  /**
   * The html5 build's packaged web root ({@code <app path>/html5/bin}, lime's
   * own index.html inside), or null for other targets or without an app path.
   */
  @Nullable
  public static Path packagedWebRoot(@NotNull VirtualFile projectFile, @NotNull String content, @NotNull String targetFlag) {
    if (!BROWSER_TARGETS.contains(targetFlag)) return null;
    return exportBinDirectory(projectFile, content, targetFlag);
  }

  /** The packaged swf a flash/air build exports ({@code <app path>/<target>/bin/<app file>.swf}), or null for another target. */
  @Nullable
  public static Path packagedSwf(@NotNull VirtualFile projectFile, @NotNull String content, @NotNull String targetFlag) {
    if (!FLASH_FAMILY_TARGETS.contains(targetFlag)) return null;
    return exportBinDirectory(projectFile, content, targetFlag)
      .resolve(appFile(content) + ".swf")
      .normalize();
  }

  /** The haxe compilation target behind a lime CLI target id, or null for an unknown id. */
  @Nullable
  public static HaxeTarget targetFor(@NotNull String targetFlag) {
    return switch (targetFlag) {
      case "hl" -> HaxeTarget.HL;
      case "html5" -> HaxeTarget.JAVA_SCRIPT;
      case "flash", "air" -> HaxeTarget.FLASH;
      case "neko" -> HaxeTarget.NEKO;
      case "java" -> HaxeTarget.JAVA;
      case "cppia" -> HaxeTarget.CPPIA;
      case "cs" -> HaxeTarget.CSHARP;
      case "cpp", "windows", "mac", "linux", "android", "ios" -> HaxeTarget.CPP;
      default -> null;
    };
  }

  /**
   * The launchable binary a lime build packages for a host target:
   * {@code <app path>/<target dir>/bin/<app file>[.exe]}, relative to the
   * project file. Neko output is wrapped in a launcher executable and an HL
   * build ships a renamed copy of the hl runtime beside its hlboot.dat — for
   * all host targets the packaged binary itself is what runs. Null when the
   * target is not host-launchable.
   */
  @Nullable
  public static Path packagedBinary(@NotNull VirtualFile projectFile, @NotNull String content, @NotNull String targetFlag) {
    if (!HOST_LAUNCHABLE_TARGETS.contains(targetFlag)) return null;
    return exportBinDirectory(projectFile, content, targetDirectory(targetFlag))
      .resolve(HaxeSdkUtilBase.getExecutableName(appFile(content)))
      .normalize();
  }

  /**
   * Lime's export layout, shared by every packaged-artifact lookup:
   * {@code <app path>/<target dir>/bin} relative to the project file, the app
   * path defaulting to {@code bin}.
   */
  @NotNull
  private static Path exportBinDirectory(@NotNull VirtualFile projectFile, @NotNull String content, @NotNull String targetDirectory) {
    String appPath = StringUtil.defaultIfEmpty(ProjectXmlParser.parseAppPath(content), "bin");
    return Path.of(projectFile.getParent().getPath())
      .resolve(appPath)
      .resolve(targetDirectory)
      .resolve("bin")
      .normalize();
  }

  /** The export subdirectory a target builds into; "cpp" is the tool's alias for the host platform. */
  @NotNull
  private static String targetDirectory(@NotNull String targetFlag) {
    return targetFlag.equals("cpp") ? hostPlatformTarget() : targetFlag;
  }

  /** The lime target naming the host desktop platform. */
  @NotNull
  public static String hostPlatformTarget() {
    if (SystemInfo.isWindows) return "windows";
    return SystemInfo.isMac ? "mac" : "linux";
  }

  /// The compile artifact per lime's export layout (`<app path>/<target>/...`),
  /// RELATIVE to the project file - the shape the parser-tool evaluation
  /// reports. Only the targets Build & run can launch need one; the packaged*
  /// lookups above answer the same layout as absolute paths at launch time.
  @Nullable
  public static String relativeTargetOutput(@NotNull String targetFlag, @NotNull String appPath, @NotNull String declaredAppFile) {
    String appFile = appFileOrDefault(declaredAppFile);
    return switch (targetFlag) {
      case "hl" -> appPath + "/hl/obj/ApplicationMain.hl";
      case "html5" -> appPath + "/html5/bin/" + appFile + ".js";
      case "flash" -> appPath + "/flash/bin/" + appFile + ".swf";
      // air: the descriptor (application.xml) sits at <app path>/air with the content swf in bin beside it
      case "air" -> appPath + "/air/bin/" + appFile + ".swf";
      // desktop cpp: lime copies the built executable into bin, named after
      // <app file>, independent of -debug (unlike raw hxcpp's Main-debug.exe)
      case "windows" -> appPath + "/windows/bin/" + appFile + ".exe";
      case "linux" -> appPath + "/linux/bin/" + appFile;
      // neko is wrapped in a launcher executable named after the app, host-suffixed
      case "neko" -> appPath + "/neko/bin/" + HaxeSdkUtilBase.getExecutableName(appFile);
      // TODO mac: the artifact is a .app bundle (Contents/MacOS/<app file>) - needs bundle-aware launch
      default -> null;
    };
  }
}
