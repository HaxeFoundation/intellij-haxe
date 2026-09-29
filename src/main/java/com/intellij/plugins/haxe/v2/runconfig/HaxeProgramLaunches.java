package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration.Lane;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapRunConfigurationBase;
import com.intellij.plugins.haxe.runner.debugger.flash.*;
import com.intellij.plugins.haxe.runner.debugger.hashlink.HashLinkRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.hashlink.HashLinkConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij.HxcppIntellijConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij.HxcppIntellijRunConfiguration;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import com.intellij.plugins.haxe.runner.neko.NekoConfigurationFactory;
import com.intellij.plugins.haxe.runner.neko.NekoRunConfiguration;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildSystem;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildWorkDirectories;
import com.intellij.plugins.haxe.v2.buildtools.LimeProjects;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/// Maps a build's compilation target to the run configuration able to launch its
/// output (the tool window's Build & run): HL bytecode → HashLink Application,
/// JS → Browser, a swf → Flash or AIR, C++ → hxcpp, neko → Neko. For hxml files the target comes from the file itself;
/// for lime/openfl/hxp files from the selected target's `lime display` hxml. The
/// configuration is created once with a "Run Haxe action" build step attached and
/// matched by that step's build file afterwards, so tree launches and the
/// run-configuration dropdown stay in sync.
///
/// CPP is wired for lime-family and nmml files only (both tools name the
/// executable after `<app file>` regardless of -debug); for plain hxml a
/// -debug build renames the executable (Main-debug.exe), so run and debug launch
/// different artifacts — a single static executable path cannot serve both
/// executors yet.
public final class HaxeProgramLaunches {

  private HaxeProgramLaunches() {
  }

  /**
   * The configuration flavour serving one target's output, the factory that
   * creates it and the bundle key naming a created configuration.
   */
  private record LaunchSpec(@NotNull Class<? extends RunConfiguration> configurationClass,
                            @NotNull Class<? extends ConfigurationFactory> factoryClass,
                            @NotNull String nameKey) {
  }

  private static final LaunchSpec HASHLINK_APP = new LaunchSpec(
    HashLinkRunConfiguration.class, HashLinkConfigurationFactory.class, "haxe.toolwindow.program.configuration.name.hashlink");
  private static final LaunchSpec BROWSER_APP = new LaunchSpec(
    BrowserRunConfiguration.class, BrowserConfigurationFactory.class, "haxe.toolwindow.program.configuration.name.browser");
  private static final LaunchSpec FLASH_APP = new LaunchSpec(
    FlashRunConfiguration.class, FlashConfigurationFactory.class, "haxe.toolwindow.program.configuration.name.flash");
  private static final LaunchSpec AIR_APP = new LaunchSpec(
    AirRunConfiguration.class, AirConfigurationFactory.class, "haxe.toolwindow.program.configuration.name.air");
  private static final LaunchSpec HXCPP_APP = new LaunchSpec(
    HxcppIntellijRunConfiguration.class, HxcppIntellijConfigurationFactory.class, "haxe.toolwindow.program.configuration.name.hxcpp");
  private static final LaunchSpec NEKO_APP = new LaunchSpec(
    NekoRunConfiguration.class, NekoConfigurationFactory.class, "haxe.toolwindow.program.configuration.name.neko");

  /** The single authority on which run configuration launches which target output. */
  @Nullable
  private static LaunchSpec specFor(@NotNull HaxeTarget target,
                                    @NotNull String targetOutput,
                                    @NotNull HaxeBuildFileType type) {
    String output = targetOutput.toLowerCase(Locale.ROOT);
    return switch (target) {
      // HL/C output (-hl out/main.c) is a source directory, not runnable bytecode
      case HL -> output.endsWith(".hl") ? HASHLINK_APP : null;
      case JAVA_SCRIPT -> output.endsWith(".js") ? BROWSER_APP : null;
      case FLASH -> {
        if (!output.endsWith(".swf")) yield null;
        yield isAirOutput(output) ? AIR_APP : FLASH_APP;
      }
      case CPP -> type != HaxeBuildFileType.HXML ? HXCPP_APP : null;
      // hxml runs the .n through the neko runtime; lime/nme package a launcher
      case NEKO -> type != HaxeBuildFileType.HXML || output.endsWith(".n") ? NEKO_APP : null;
      default -> null;
    };
  }

  /**
   * The profiler lane the target's launch configuration profiles under,
   * null for targets without one. Flash profiles only as AIR: adl's
   * debugger runtime carries the telemetry sampler; the standalone player
   * launch does not.
   */
  @Nullable
  public static Lane profilingLaneFor(@NotNull HaxeTarget target, @NotNull String targetOutput) {
    return switch (target) {
      case HL -> Lane.HASHLINK;
      case CPP -> Lane.HXCPP;
      case JAVA_SCRIPT -> Lane.JS;
      case FLASH -> isAirOutput(targetOutput.toLowerCase(Locale.ROOT)) ? Lane.FLASH : null;
      default -> null;
    };
  }

  /** True when the target's launch configuration can run under a Haxe profiler entry. */
  public static boolean supportsProgramProfiling(@NotNull HaxeTarget target, @NotNull String targetOutput) {
    return profilingLaneFor(target, targetOutput) != null;
  }

  /** Display name of the configuration kind that launches this build ("HashLink Application", …), or null when unsupported. */
  @Nullable
  public static String launchKind(@NotNull HaxeBuildFileInfo info, @NotNull HaxeBuildFileType type) {
    if (info.target() == null || info.targetOutput() == null) return null;
    LaunchSpec spec = specFor(info.target(), info.targetOutput(), type);
    return spec == null ? null : factoryOf(spec).getName();
  }

  /**
   * The build's launch configuration: the registered one when present (matched by
   * target class + the build step's build file, so renames don't duplicate it),
   * else a new one added to the RunManager.
   */
  @Nullable
  public static RunnerAndConfigurationSettings findOrCreate(@NotNull Project project,
                                                            @NotNull Module module,
                                                            @NotNull HaxeBuildFile buildFile,
                                                            @NotNull HaxeTarget target,
                                                            @NotNull String targetOutput) {
    LaunchSpec spec = specFor(target, targetOutput, buildFile.type());
    if (spec == null) return null;
    VirtualFile file = buildFile.file();

    RunnerAndConfigurationSettings existing = RunManager.getInstance(project).getAllSettings().stream()
      .filter(candidate -> matches(candidate, spec.configurationClass(), file.getPath()))
      .findFirst()
      .orElse(null);

    if (existing != null) {
      // the DERIVED fields (artifact/launcher paths) follow the build file's
      // current layout - a reused configuration must not keep values baked
      // when the export layout was different
      configure(existing, buildFile, targetOutput);
      return existing;
    }

    String name = HaxeBundle.message(spec.nameKey(), file.getName());
    RunnerAndConfigurationSettings settings = RunManager.getInstance(project).createConfiguration(name, factoryOf(spec));
    configure(settings, buildFile, targetOutput);
    DapRunConfigurationBase configuration = (DapRunConfigurationBase)settings.getConfiguration();
    configuration.setModule(module);

    HaxeActionBeforeRunTaskProvider.Task buildTask = new HaxeActionBeforeRunTaskProvider.Task();
    buildTask.setBuildFilePath(file.getPath());
    buildTask.setActionName(HaxeBuildSystem.of(buildFile.type()).defaultBuildActionName());
    configuration.setBeforeRunTasks(List.of(buildTask));

    RunManager.getInstance(project).addConfiguration(settings);
    return settings;
  }

  @NotNull
  private static ConfigurationFactory factoryOf(@NotNull LaunchSpec spec) {
    return HaxeRunConfigurationType.getInstance().getFactory(spec.factoryClass());
  }

  /**
   * hxml: the neko runtime executes the {@code .n} bytecode (the configured
   * Build Tools neko, else the bare name from PATH). lime and nme both wrap
   * the bytecode in a launcher executable, which runs directly - lime's
   * target output points inside obj with the launcher in bin beside it
   * (same layout the hxcpp and HL configs navigate); nme's target output IS
   * the launcher.
   */
  private static void configureNeko(@NotNull Project project,
                                    @NotNull NekoRunConfiguration configuration,
                                    @NotNull HaxeBuildFile buildFile,
                                    @NotNull String targetOutput) {
    Path output = resolvedOutput(project, buildFile.file(), targetOutput);
    if (buildFile.type() == HaxeBuildFileType.HXML) {
      // empty executable = the neko RUNTIME, resolved at launch (settings/SDK/PATH)
      configuration.setExecutablePath("");
      configuration.setProgramArguments(output.toString());
      configuration.setWorkingDirectory(String.valueOf(output.getParent()));
      return;
    }

    Path launcher = output;
    if (LimeProjects.isLimeFamily(buildFile.type())) {
      // lime's target output points inside obj; the launcher is bin/<app file> beside it
      Path binDir = binDirBesideObj(output.getParent());
      String appFile = appFileName(buildFile);
      if (binDir != null && appFile != null) {
        launcher = binDir.resolve(HaxeSdkUtilBase.getExecutableName(appFile));
      }
    }
    configuration.setExecutablePath(launcher.toString());
  }

  /** Applies the derived fields (artifact/launcher paths) onto a configuration; user-owned fields (SDK, browser choice...) stay. */
  private static void configure(@NotNull RunnerAndConfigurationSettings settings,
                                @NotNull HaxeBuildFile buildFile,
                                @NotNull String targetOutput) {
    Project project = settings.getConfiguration().getProject();
    switch (settings.getConfiguration()) {
      case HashLinkRunConfiguration configuration -> configureHashLink(project, configuration, buildFile, targetOutput);
      case BrowserRunConfiguration configuration -> configureBrowser(project, configuration, buildFile, targetOutput);
      case FlashRunConfiguration configuration -> configureFlash(project, configuration, buildFile, targetOutput);
      case AirRunConfiguration configuration -> configureAir(project, configuration, buildFile, targetOutput);
      case HxcppIntellijRunConfiguration configuration ->
        configureHxcppExecutable(configuration, buildFile, resolvedOutput(project, buildFile.file(), targetOutput));
      case NekoRunConfiguration configuration -> configureNeko(project, configuration, buildFile, targetOutput);
      default -> { }
    }
  }

  private static void configureHashLink(@NotNull Project project,
                                        @NotNull HashLinkRunConfiguration configuration,
                                        @NotNull HaxeBuildFile buildFile,
                                        @NotNull String targetOutput) {
    Path output = resolvedOutput(project, buildFile.file(), targetOutput);
    if (buildFile.type() == HaxeBuildFileType.HXML) {
      configuration.setHlFilePath(output.toString());
    }
    else {
      configureLimeHashLink(configuration, buildFile, output);
    }
  }

  private static void configureBrowser(@NotNull Project project,
                                       @NotNull BrowserRunConfiguration configuration,
                                       @NotNull HaxeBuildFile buildFile,
                                       @NotNull String targetOutput) {
    // serve mode hosts the directory containing the compiled .js (plus its
    // source map and index.html); the browser is picked in the editor
    Path outputDirectory = resolvedOutput(project, buildFile.file(), targetOutput).getParent();
    if (outputDirectory != null) {
      configuration.setContentRoot(outputDirectory.toString());
    }
  }

  private static void configureFlash(@NotNull Project project,
                                     @NotNull FlashRunConfiguration configuration,
                                     @NotNull HaxeBuildFile buildFile,
                                     @NotNull String targetOutput) {
    configuration.setSwfFilePath(resolvedOutput(project, buildFile.file(), targetOutput).toString());
    // the flex SDK and player cannot be guessed - the visible config prompts for them
  }

  /**
   * lime's air target exports into {@code <app path>/air/}: the descriptor
   * (application.xml) at the export root, the content (swf) in {@code bin/}
   * beside it. A plain flash swf never lives in that layout.
   * TODO honor a custom air.output-directory from project.xml (the "air"
   *  segment is that setting's default)
   */
  private static boolean isAirOutput(@NotNull String lowerCaseOutput) {
    return lowerCaseOutput.replace('\\', '/').contains("/air/bin/");
  }

  private static void configureAir(@NotNull Project project,
                                   @NotNull AirRunConfiguration configuration,
                                   @NotNull HaxeBuildFile buildFile,
                                   @NotNull String targetOutput) {
    Path binDir = resolvedOutput(project, buildFile.file(), targetOutput).getParent();
    if (binDir == null || binDir.getParent() == null) return;
    configuration.setDescriptorPath(binDir.getParent().resolve("application.xml").toString());
    configuration.setContentRootPath(binDir.toString());
    // the flex/AIR SDK cannot be guessed - the visible config prompts for it
  }

  /// lime packages an HL build into `<export>/hl/bin`: the obj bytecode
  /// becomes `bin/hlboot.dat`, next to a renamed copy of the hl runtime
  /// (`<app file>[.exe]`) carrying the game's .hdll libraries. The SDK's
  /// plain hl executable cannot run that game, so the configuration points at
  /// hlboot.dat and overrides the runtime with the bundled executable.
  private static void configureLimeHashLink(@NotNull HashLinkRunConfiguration configuration,
                                            @NotNull HaxeBuildFile buildFile,
                                            @NotNull Path objOutput) {
    // the HL output path points at a file INSIDE obj - its parent is the obj dir
    Path binDir = binDirBesideObj(objOutput.getParent());
    if (binDir == null) return;
    configuration.setHlFilePath(binDir.resolve("hlboot.dat").toString());

    String appFile = appFileName(buildFile);
    if (appFile != null) {
      configuration.setUseCustomHlBinary(true);
      configuration.setCustomHlBinaryPath(binDir.resolve(HaxeSdkUtilBase.getExecutableName(appFile)).toString());
    }
  }

  /// An nmml target output is the executable path already and passes through.
  /// lime places the desktop executable at `<export>/<target>/bin/<app file>[.exe]`;
  /// its display pipeline reports that path directly, but the legacy `lime display`
  /// fallback yields the C++ obj directory instead — derive bin from it the way
  /// the HL flavor does. Unresolvable (hxp, or an nmml without an app file):
  /// the visible configuration prompts for the executable.
  private static void configureHxcppExecutable(@NotNull HxcppIntellijRunConfiguration configuration,
                                               @NotNull HaxeBuildFile buildFile,
                                               @NotNull Path output) {
    if ("obj".equals(String.valueOf(output.getFileName()))) {
      // the CPP output path IS the obj dir; the executable sits in bin beside it
      Path binDir = binDirBesideObj(output);
      String appFile = appFileName(buildFile);
      if (binDir == null || appFile == null) return;
      output = binDir.resolve(HaxeSdkUtilBase.getExecutableName(appFile));
    }
    configuration.setExecutablePath(output.toString());
  }

  /// lime's runnable artifacts live in `<export>/<target>/bin`, beside the obj dir the legacy display path reports.
  @Nullable
  private static Path binDirBesideObj(@Nullable Path objDir) {
    if (objDir == null || !"obj".equals(String.valueOf(objDir.getFileName()))) return objDir;
    Path targetDir = objDir.getParent();
    return targetDir == null ? null : targetDir.resolve("bin");
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

  /**
   * The target output resolved the way the compiler would: against the build
   * file's work directory - the file's own folder for the lime family (lime
   * display runs there, so its paths resolve the same), the hxml anchor from
   * {@link HaxeBuildWorkDirectories} otherwise.
   */
  @NotNull
  private static Path resolvedOutput(@NotNull Project project, @NotNull VirtualFile buildFile, @NotNull String targetOutput) {
    String anchor = HaxeBuildWorkDirectories.workDirectory(project, buildFile);
    return Path.of(anchor != null ? anchor : buildFile.getParent().getPath())
      .resolve(targetOutput)
      .normalize();
  }

  private static boolean matches(@NotNull RunnerAndConfigurationSettings candidate,
                                 @NotNull Class<? extends RunConfiguration> configurationClass,
                                 @NotNull String buildFilePath) {
    RunConfiguration configuration = candidate.getConfiguration();
    if (!configurationClass.isInstance(configuration)) return false;
    return configuration.getBeforeRunTasks().stream()
      .anyMatch(task -> task instanceof HaxeActionBeforeRunTaskProvider.Task build
                        && build.getBuildFilePath().equals(buildFilePath));
  }
}
