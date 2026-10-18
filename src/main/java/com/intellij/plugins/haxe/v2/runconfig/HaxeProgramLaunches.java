package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapRunConfigurationBase;
import com.intellij.plugins.haxe.runner.debugger.flash.AirRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.flash.FlashRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.hashlink.HashLinkRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij.HxcppIntellijRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.interp.InterpRunConfiguration;
import com.intellij.plugins.haxe.runner.HaxeRuntimeRunConfiguration;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildSystem;
import com.intellij.plugins.haxe.v2.buildtools.HxcppExecutables;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildWorkDirectories;
import com.intellij.plugins.haxe.v2.buildtools.LimeProjects;
import com.intellij.plugins.haxe.v2.runconfig.HaxeLaunchKind.Parameter;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/// Maps a build's compilation target to the [HaxeLaunchKind] able to launch its
/// output (the tool window's Build & run) and creates or finds that
/// configuration. For hxml files the target comes from the file itself; for
/// lime/openfl/hxp files from the selected target's evaluation. A
/// configuration is created once with a "Run Haxe action" build step attached
/// and matched by that step's build file and action afterwards, so tree
/// launches and the run-configuration dropdown stay in sync; a launching
/// custom action gets a configuration of its own, keyed by its name.
///
/// CPP launches the executable the build links. Lime-family and nmml files
/// name it after `<app file>` whatever the build type. A plain hxml build
/// names it after the main class, with `-debug` for a debug build, so its
/// configuration holds `${executable}` and resolves the name per launch
/// ([HxcppExecutables]); an hxml linking a library or building for another
/// platform gets no launch.
///
/// An interpreter build (`--interp`) writes no output to launch: compiling IS
/// running. Its Haxe Interpreter configuration runs the hxml the way the Build
/// action does (same working directory, same file argument), gets no build
/// step, and is matched by the hxml its arguments name.
public final class HaxeProgramLaunches {

  private static final List<HaxeLaunchKind> NODE_FIRST = List.of(HaxeLaunchKind.NODE, HaxeLaunchKind.BROWSER);
  private static final List<HaxeLaunchKind> BROWSER_FIRST = List.of(HaxeLaunchKind.BROWSER, HaxeLaunchKind.NODE);

  private HaxeProgramLaunches() {
  }

  /**
   * The single authority on which run configuration launches which target
   * output. {@code targetFlag} is the lime/nme target the file is set to
   * build (null for hxml): a swf launches as AIR only when that flag is
   * lime's air target - the output path says nothing reliable about it,
   * since the air export directory is configurable. {@code defines} decide
   * whether an hxml C++ build links an executable this machine runs.
   */
  @Nullable
  static HaxeLaunchKind kindFor(@NotNull HaxeTarget target,
                                @NotNull String targetOutput,
                                @NotNull HaxeBuildFileType type,
                                @Nullable String targetFlag,
                                @NotNull List<HaxeDefine> defines) {
    String output = targetOutput.toLowerCase(Locale.ROOT);
    return switch (target) {
      // HL/C output (-hl out/main.c) is a source directory, not runnable bytecode
      case HL -> output.endsWith(".hl") ? HaxeLaunchKind.HASHLINK : null;
      // lime's electron target runs in Electron, neither a browser nor plain node
      case JAVA_SCRIPT -> output.endsWith(".js") && !LimeProjects.isElectronTarget(targetFlag) ? HaxeLaunchKind.BROWSER : null;
      case FLASH -> {
        if (!output.endsWith(".swf")) yield null;
        yield LimeProjects.isAirTarget(targetFlag) ? HaxeLaunchKind.AIR : HaxeLaunchKind.FLASH;
      }
      case CPP -> type != HaxeBuildFileType.HXML || HxcppExecutables.buildsHostExecutable(defines) ? HaxeLaunchKind.HXCPP : null;
      // hxml runs the .n through the neko runtime; lime/nme package a launcher
      case NEKO -> type != HaxeBuildFileType.HXML || output.endsWith(".n") ? HaxeLaunchKind.NEKO : null;
      case INTERP -> HaxeLaunchKind.INTERP;
      default -> null;
    };
  }

  /**
   * Every kind launching this build's output, the likelier first. JS from an
   * hxml may be browser code or a Node.js program, and nothing in the build
   * proves which (a shared hxml, a bundler in between), so it gets both:
   * hxnodejs or {@code -D nodejs} puts Node.js first. The lime family's html5
   * is a browser build.
   */
  @NotNull
  public static List<HaxeLaunchKind> launchKinds(@NotNull Project project,
                                                 @NotNull HaxeBuildFileInfo info,
                                                 @NotNull HaxeBuildFile buildFile) {
    HaxeLaunchKind kind = launchKind(project, info, buildFile);
    if (kind == null) return List.of();
    if (kind != HaxeLaunchKind.BROWSER || buildFile.type() != HaxeBuildFileType.HXML) return List.of(kind);
    return info.hasNodeSignal() ? NODE_FIRST : BROWSER_FIRST;
  }

  /** The likeliest kind launching this build's output, or null when nothing can. */
  @Nullable
  private static HaxeLaunchKind launchKind(@NotNull Project project, @NotNull HaxeBuildFileInfo info, @NotNull HaxeBuildFile buildFile) {
    String output = launchOutput(info);
    if (info.target() == null || output == null) return null;
    return kindFor(info.target(), output, buildFile.type(), selectedTargetFlag(project, buildFile), info.defines());
  }

  /** The lime/nme target flag the file is currently set to build; null for hxml, whose target is in the file itself. */
  @Nullable
  private static String selectedTargetFlag(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    return HaxeBuildSystem.of(buildFile.type()).selectedTargetFlag(project, buildFile);
  }

  /**
   * The output a launch starts from: the build's target output, or "" for an
   * interpreter build, which writes none. Null when the build has no output.
   */
  @Nullable
  public static String launchOutput(@NotNull HaxeBuildFileInfo info) {
    return info.target() == HaxeTarget.INTERP ? "" : info.targetOutput();
  }

  /**
   * The configuration launching {@code buildFile}'s output through {@code kind}
   * after the action named {@code buildAction} compiled it: the registered one
   * when present (matched by kind + build file + action, so renaming the
   * configuration does not duplicate it), else a new one added to the
   * RunManager. Parameters: the
   * kind's defaults derived from the build file, overlaid with the non-blank
   * {@code overrides} - a blank override keeps the default live.
   */
  @NotNull
  public static RunnerAndConfigurationSettings findOrCreate(@NotNull Project project,
                                                            @NotNull Module module,
                                                            @NotNull HaxeBuildFile buildFile,
                                                            @NotNull HaxeLaunchKind kind,
                                                            @NotNull String targetOutput,
                                                            @NotNull String buildAction,
                                                            @NotNull Map<Parameter, String> overrides,
                                                            boolean injectDebugArguments) {
    VirtualFile file = buildFile.file();
    Map<Parameter, String> parameters = defaultParameters(project, buildFile, kind, targetOutput);
    overrides.forEach((parameter, value) -> {
      if (!StringUtil.isEmptyOrSpaces(value)) parameters.put(parameter, value.trim());
    });

    RunnerAndConfigurationSettings existing = RunManager.getInstance(project).getAllSettings().stream()
      .filter(candidate -> matches(candidate, kind, file.getPath(), buildAction))
      .findFirst()
      .orElse(null);
    if (existing != null) {
      // the DERIVED fields (artifact/launcher paths) follow the build file's
      // current layout - a reused configuration must not keep values baked
      // when the export layout was different
      apply(existing.getConfiguration(), parameters);
      return existing;
    }

    boolean defaultBuild = buildAction.equals(HaxeBuildSystem.of(buildFile.type()).defaultBuildActionName());
    String name = kind.configurationName(defaultBuild ? file.getName() : file.getName() + " " + buildAction);
    RunnerAndConfigurationSettings settings = RunManager.getInstance(project).createConfiguration(name, kind.factory());
    apply(settings.getConfiguration(), parameters);
    DapRunConfigurationBase configuration = (DapRunConfigurationBase)settings.getConfiguration();
    configuration.setModule(module);

    // an empty list also drops the platform's default Build step
    List<BeforeRunTask<?>> beforeLaunch = kind.compilesBeforeLaunch()
                                          ? List.of(buildStep(buildFile, buildAction, injectDebugArguments))
                                          : List.of();
    configuration.setBeforeRunTasks(beforeLaunch);

    RunManager.getInstance(project).addConfiguration(settings);
    return settings;
  }

  /**
   * The kind's parameters as the build file's layout dictates: the artifact
   * the selected target writes and, for the lime family, the launcher packaged
   * beside it. A parameter the file gives no value for is absent, and the
   * visible configuration prompts for it.
   */
  @NotNull
  public static Map<Parameter, String> defaultParameters(@NotNull Project project,
                                                         @NotNull HaxeBuildFile buildFile,
                                                         @NotNull HaxeLaunchKind kind,
                                                         @NotNull String targetOutput) {
    Map<Parameter, String> parameters = new EnumMap<>(Parameter.class);
    switch (kind) {
      case HASHLINK -> hashLinkDefaults(project, buildFile, targetOutput, parameters);
      case BROWSER -> browserDefaults(project, buildFile, targetOutput, parameters);
      case NODE -> runtimeDefaults(HaxeBuildWorkDirectories.resolve(project, buildFile.file(), targetOutput), parameters);
      case FLASH -> parameters.put(Parameter.SWF, HaxeBuildWorkDirectories.resolve(project, buildFile.file(), targetOutput).toString());
      case AIR -> airDefaults(project, buildFile, targetOutput, parameters);
      case HXCPP -> hxcppDefaults(project, buildFile, targetOutput, parameters);
      case NEKO -> nekoDefaults(project, buildFile, targetOutput, parameters);
      case INTERP -> interpDefaults(project, buildFile, parameters);
    }
    return parameters;
  }

  /** Writes the parameters onto a configuration of the matching kind; fields without a parameter (SDK, browser choice...) stay. */
  public static void apply(@NotNull RunConfiguration configuration, @NotNull Map<Parameter, String> parameters) {
    switch (configuration) {
      case HashLinkRunConfiguration hashLink -> applyHashLink(hashLink, parameters);
      case BrowserRunConfiguration browser -> ifPresent(parameters, Parameter.CONTENT_ROOT, browser::setContentRoot);
      case FlashRunConfiguration flash -> ifPresent(parameters, Parameter.SWF, flash::setSwfFilePath);
      case AirRunConfiguration air -> applyAir(air, parameters);
      case HxcppIntellijRunConfiguration hxcpp -> ifPresent(parameters, Parameter.EXECUTABLE, hxcpp::setExecutablePath);
      case HaxeRuntimeRunConfiguration runtime -> applyRuntime(runtime, parameters);
      case InterpRunConfiguration interp -> applyInterp(interp, parameters);
      default -> { }
    }
  }

  private static void ifPresent(@NotNull Map<Parameter, String> parameters,
                                @NotNull Parameter parameter,
                                @NotNull Consumer<String> setter) {
    String value = parameters.get(parameter);
    if (value != null) setter.accept(value);
  }

  /// lime packages an HL build into `<export>/hl/bin`: the obj bytecode
  /// becomes `bin/hlboot.dat`, next to a renamed copy of the hl runtime
  /// (`<app file>[.exe]`) carrying the game's .hdll libraries. The SDK's
  /// plain hl executable cannot run that game, so the launch points at
  /// hlboot.dat and overrides the runtime with the bundled executable. An
  /// hxml's `.hl` runs on the SDK's hl.
  private static void hashLinkDefaults(@NotNull Project project,
                                       @NotNull HaxeBuildFile buildFile,
                                       @NotNull String targetOutput,
                                       @NotNull Map<Parameter, String> parameters) {
    Path output = HaxeBuildWorkDirectories.resolve(project, buildFile.file(), targetOutput);
    if (buildFile.type() == HaxeBuildFileType.HXML) {
      parameters.put(Parameter.HL_FILE, output.toString());
      return;
    }
    Path beside = LimeProjects.binDirBesideObj(output);
    Path binDir = beside != null ? beside : output.getParent();
    if (binDir == null) return;
    parameters.put(Parameter.HL_FILE, binDir.resolve(LimeProjects.HL_BOOT_FILE).toString());
    String appFile = appFileName(buildFile);
    if (appFile != null) {
      parameters.put(Parameter.HL_BINARY, binDir.resolve(HaxeSdkUtilBase.getExecutableName(appFile)).toString());
    }
  }

  private static void applyHashLink(@NotNull HashLinkRunConfiguration configuration, @NotNull Map<Parameter, String> parameters) {
    ifPresent(parameters, Parameter.HL_FILE, configuration::setHlFilePath);
    String binary = parameters.get(Parameter.HL_BINARY);
    if (binary != null) {
      configuration.setUseCustomHlBinary(true);
      configuration.setCustomHlBinaryPath(binary);
    }
  }

  /// Serve mode hosts the directory containing the compiled .js (plus its
  /// source map and index.html); the browser is picked in the editor.
  private static void browserDefaults(@NotNull Project project,
                                      @NotNull HaxeBuildFile buildFile,
                                      @NotNull String targetOutput,
                                      @NotNull Map<Parameter, String> parameters) {
    Path outputDirectory = HaxeBuildWorkDirectories.resolve(project, buildFile.file(), targetOutput).getParent();
    if (outputDirectory != null) {
      parameters.put(Parameter.CONTENT_ROOT, outputDirectory.toString());
    }
  }

  /// lime's air target exports the descriptor (application.xml) at the
  /// target root with the content (swf) in `bin/` beside it, whatever the
  /// configured output directory - the target output names the swf. The
  /// flex/AIR SDK cannot be guessed; the visible configuration prompts for it.
  private static void airDefaults(@NotNull Project project,
                                  @NotNull HaxeBuildFile buildFile,
                                  @NotNull String targetOutput,
                                  @NotNull Map<Parameter, String> parameters) {
    Path binDir = HaxeBuildWorkDirectories.resolve(project, buildFile.file(), targetOutput).getParent();
    if (binDir == null || binDir.getParent() == null) return;
    parameters.put(Parameter.DESCRIPTOR, binDir.getParent().resolve("application.xml").toString());
    parameters.put(Parameter.CONTENT_ROOT, binDir.toString());
  }

  private static void applyAir(@NotNull AirRunConfiguration configuration, @NotNull Map<Parameter, String> parameters) {
    ifPresent(parameters, Parameter.DESCRIPTOR, configuration::setDescriptorPath);
    ifPresent(parameters, Parameter.CONTENT_ROOT, configuration::setContentRootPath);
  }

  /// An nmml target output is the executable path already and passes through.
  /// lime places the desktop executable at `<export>/<target>/bin/<app file>[.exe]`;
  /// its display pipeline reports that path directly, but the legacy `lime display`
  /// fallback yields the C++ obj directory instead — derive bin from it the way
  /// the HL flavor does. Unresolvable (hxp, or an nmml without an app file):
  /// the visible configuration prompts for the executable.
  private static void hxcppDefaults(@NotNull Project project,
                                    @NotNull HaxeBuildFile buildFile,
                                    @NotNull String targetOutput,
                                    @NotNull Map<Parameter, String> parameters) {
    if (buildFile.type() == HaxeBuildFileType.HXML) {
      // the name follows the launch's debug flags: resolved per launch
      parameters.put(Parameter.EXECUTABLE, HxcppExecutables.EXECUTABLE_VARIABLE);
      return;
    }
    Path output = HaxeBuildWorkDirectories.resolve(project, buildFile.file(), targetOutput);
    Path launcher = packagedLauncher(project, buildFile, output);
    if (launcher != null) parameters.put(Parameter.EXECUTABLE, launcher.toString());
  }

  /**
   * The launcher an evaluated output stands for: the output itself when the
   * evaluation names the launcher (every nmml output, lime's desktop and neko
   * outputs); for an output under obj (lime's HL bytecode, or the obj
   * directory itself) the launcher in bin beside it, per
   * {@link LimeProjects#packagedBinary(Path, String, String)}. Null for an
   * obj output whose launcher cannot be named (an hxp, an undeclared app
   * file): the visible configuration then prompts for the executable.
   */
  @Nullable
  private static Path packagedLauncher(@NotNull Project project, @NotNull HaxeBuildFile buildFile, @NotNull Path output) {
    if (LimeProjects.binDirBesideObj(output) == null) return output;
    String appFile = appFileName(buildFile);
    String targetFlag = selectedTargetFlag(project, buildFile);
    if (appFile == null || targetFlag == null) return null;
    return LimeProjects.packagedBinary(output, targetFlag, appFile);
  }

  /**
   * hxml: the neko runtime executes the {@code .n} bytecode (the configured
   * Build Tools neko, else the bare name from PATH). lime and nme both wrap
   * the bytecode in a launcher executable, which runs directly; both
   * evaluations name the launcher (see {@link #packagedLauncher}).
   */
  private static void nekoDefaults(@NotNull Project project,
                                   @NotNull HaxeBuildFile buildFile,
                                   @NotNull String targetOutput,
                                   @NotNull Map<Parameter, String> parameters) {
    Path output = HaxeBuildWorkDirectories.resolve(project, buildFile.file(), targetOutput);
    if (buildFile.type() == HaxeBuildFileType.HXML) {
      runtimeDefaults(output, parameters);
      return;
    }
    Path launcher = packagedLauncher(project, buildFile, output);
    if (launcher != null) parameters.put(Parameter.EXECUTABLE, launcher.toString());
  }

  /** The runtime runs the output from the output's folder; an empty executable is the RUNTIME, resolved at launch. */
  private static void runtimeDefaults(@NotNull Path output, @NotNull Map<Parameter, String> parameters) {
    parameters.put(Parameter.EXECUTABLE, "");
    parameters.put(Parameter.PROGRAM_ARGUMENTS, ParametersListUtil.join(List.of(output.toString())));
    parameters.put(Parameter.WORKING_DIRECTORY, String.valueOf(output.getParent()));
  }

  private static void applyRuntime(@NotNull HaxeRuntimeRunConfiguration configuration, @NotNull Map<Parameter, String> parameters) {
    ifPresent(parameters, Parameter.EXECUTABLE, configuration::setExecutablePath);
    ifPresent(parameters, Parameter.PROGRAM_ARGUMENTS, configuration::setProgramArguments);
    ifPresent(parameters, Parameter.WORKING_DIRECTORY, configuration::setWorkingDirectory);
  }

  /**
   * The hxml as the Build action runs it: its working directory (override or
   * sniffed) and the file argument relative to it. Extra arguments and the
   * interpreter checkbox stay the user's.
   */
  private static void interpDefaults(@NotNull Project project,
                                     @NotNull HaxeBuildFile buildFile,
                                     @NotNull Map<Parameter, String> parameters) {
    String workDirectory = HaxeBuildWorkDirectories.workDirectory(project, buildFile.file());
    parameters.put(Parameter.WORKING_DIRECTORY, StringUtil.notNullize(workDirectory));
    parameters.put(Parameter.COMPILER_ARGUMENTS, HaxeBuildWorkDirectories.fileArgument(buildFile.file(), workDirectory));
  }

  private static void applyInterp(@NotNull InterpRunConfiguration configuration, @NotNull Map<Parameter, String> parameters) {
    ifPresent(parameters, Parameter.WORKING_DIRECTORY, configuration::setWorkingDirectory);
    ifPresent(parameters, Parameter.COMPILER_ARGUMENTS, configuration::setCompilerArguments);
  }

  /// The `<app file>` name the tool packages under: lime's default when a lime-family
  /// xml declares none; null for hxp (a script, not xml) or an undeclared nmml one.
  @Nullable
  private static String appFileName(@NotNull HaxeBuildFile buildFile) {
    if (buildFile.type() == HaxeBuildFileType.HXP_PROJECT) return null;
    String content = HaxeReadActions.compute(() -> HaxeBuildFileInspector.loadText(buildFile.file()));
    if (content == null) return null;
    return LimeProjects.isLimeFamily(buildFile.type()) ? LimeProjects.appFile(content) : ProjectXmlParser.parseAppFile(content);
  }

  private static boolean matches(@NotNull RunnerAndConfigurationSettings candidate,
                                 @NotNull HaxeLaunchKind kind,
                                 @NotNull String buildFilePath,
                                 @NotNull String buildAction) {
    RunConfiguration configuration = candidate.getConfiguration();
    if (!kind.configurationClass().isInstance(configuration)) return false;
    return configuration instanceof InterpRunConfiguration interp
           ? namesBuildFile(interp, buildFilePath)
           : configuration.getBeforeRunTasks().stream().anyMatch(task -> buildsFile(task, buildFilePath, buildAction));
  }

  private static boolean buildsFile(@NotNull BeforeRunTask<?> task, @NotNull String buildFilePath, @NotNull String buildAction) {
    return task instanceof HaxeActionBeforeRunTaskProvider.Task build
           && build.getBuildFilePath().equals(buildFilePath)
           && build.getActionName().equals(buildAction);
  }

  /** Whether the interpreter configuration's arguments are exactly the hxml, resolved against its working directory. */
  private static boolean namesBuildFile(@NotNull InterpRunConfiguration configuration, @NotNull String buildFilePath) {
    Path workDirectory = configuration.resolveWorkingDirectory();
    String arguments = configuration.getCompilerArguments().trim();
    if (workDirectory == null || arguments.isEmpty()) return false;
    try {
      return workDirectory.resolve(arguments).normalize().equals(Path.of(buildFilePath).normalize());
    }
    catch (InvalidPathException e) {
      return false;
    }
  }

  /**
   * Gives an existing configuration of any type the "Run Haxe action" step
   * that compiles {@code buildFile} with {@code buildAction} before it runs,
   * unless it carries that step already. The configuration's other steps stay.
   */
  public static void ensureBuildStep(@NotNull RunConfiguration configuration,
                                     @NotNull HaxeBuildFile buildFile,
                                     @NotNull String buildAction,
                                     boolean injectDebugArguments) {
    String path = buildFile.file().getPath();
    List<BeforeRunTask<?>> steps = configuration.getBeforeRunTasks();
    if (steps.stream().anyMatch(task -> buildsFile(task, path, buildAction))) return;
    List<BeforeRunTask<?>> withBuild = new ArrayList<>(steps);
    withBuild.addFirst(buildStep(buildFile, buildAction, injectDebugArguments));
    configuration.setBeforeRunTasks(withBuild);
  }

  /**
   * Points the "Run Haxe action" steps that compile {@code buildFilePath} with
   * the action {@code oldName} at {@code newName}, or drops them when
   * {@code newName} is null. A step naming an action that no longer exists
   * fails every launch of the configuration carrying it.
   */
  public static void retargetBuildSteps(@NotNull Project project,
                                        @NotNull String buildFilePath,
                                        @NotNull String oldName,
                                        @Nullable String newName) {
    for (RunnerAndConfigurationSettings settings : RunManager.getInstance(project).getAllSettings()) {
      RunConfiguration configuration = settings.getConfiguration();
      List<BeforeRunTask<?>> steps = configuration.getBeforeRunTasks();
      boolean affected = steps.stream().anyMatch(task -> buildsFile(task, buildFilePath, oldName));
      if (affected) {
        configuration.setBeforeRunTasks(retargeted(steps, buildFilePath, oldName, newName));
      }
    }
  }

  @NotNull
  private static List<BeforeRunTask<?>> retargeted(@NotNull List<BeforeRunTask<?>> steps,
                                                   @NotNull String buildFilePath,
                                                   @NotNull String oldName,
                                                   @Nullable String newName) {
    List<BeforeRunTask<?>> result = new ArrayList<>();
    for (BeforeRunTask<?> task : steps) {
      if (!buildsFile(task, buildFilePath, oldName)) {
        result.add(task);
      }
      else if (newName != null) {
        HaxeActionBeforeRunTaskProvider.Task build = (HaxeActionBeforeRunTaskProvider.Task)task;
        build.setActionName(newName);
        result.add(build);
      }
    }
    return result;
  }

  /** The "Run Haxe action" step compiling the build file before the program launches. */
  @NotNull
  private static HaxeActionBeforeRunTaskProvider.Task buildStep(@NotNull HaxeBuildFile buildFile,
                                                                @NotNull String buildAction,
                                                                boolean injectDebugArguments) {
    HaxeActionBeforeRunTaskProvider.Task buildTask = new HaxeActionBeforeRunTaskProvider.Task();
    buildTask.setBuildFilePath(buildFile.file().getPath());
    buildTask.setActionName(buildAction);
    buildTask.setInjectDebugArguments(injectDebugArguments);
    return buildTask;
  }
}
