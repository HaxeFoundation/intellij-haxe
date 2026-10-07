package com.intellij.plugins.haxe.v2.buildtools.info;

import com.intellij.plugins.haxe.v2.buildtools.LimeProjects;
import com.intellij.plugins.haxe.v2.buildtools.HaxeProjectTrust;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlFileParser;
import com.intellij.plugins.haxe.v2.buildsystem.ProjectXmlParser;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeProjectInfoCache.Key;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeProjectInfoCache.Outcome;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.util.HaxePluginPaths;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.intellij.plugins.haxe.v2.buildtools.info.HaxeProjectInfoCache.firstErrorLine;

/**
 * Resolves the effective compiler configuration of Lime/OpenFL/HXP project files.
 * Primary path: the bundled LimeProjectParser jar (tools/LimeProjectParser.jar,
 * run on the IDE's own JRE) evaluates the project natively and returns structured
 * JSON — the compiler's -D set, haxelibs WITH their identity, sources and app data. Fallback
 * when the jar is missing or fails: {@code haxelib run lime|openfl display},
 * whose hxml output flattens haxelibs into classpaths.
 *
 * Results are cached per (file, target, toolchain) and refreshed in the
 * background via {@link HaxeProjectInfoCache} - callers get the cached value
 * (possibly stale, possibly null on first ask) immediately and a callback once
 * a refresh lands. A failed parser-tool run retries a bounded number of times
 * per file revision, so a broken toolchain never causes refresh loops.
 */
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeLimeProjectInfoService implements Disposable {

  private static final int DISPLAY_TIMEOUT_MS = 60_000;
  private static final String PARSER_RELATIVE_PATH = "tools/LimeProjectParser.jar";
  // unknown members ignored so a newer bundled tool may add output fields freely
  private static final ObjectMapper PARSER_OUTPUT_MAPPER = JsonMapper.builder()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build();

  private final Project project;
  private final HaxeProjectInfoCache<HaxeBuildFileInfo> cache;

  public HaxeLimeProjectInfoService(@NotNull Project project) {
    this.project = project;
    this.cache = new HaxeProjectInfoCache<>(project, "Haxe lime display", null);
  }

  @NotNull
  public static HaxeLimeProjectInfoService getInstance(@NotNull Project project) {
    return project.getService(HaxeLimeProjectInfoService.class);
  }

  /**
   * The file's effective configuration for the target, or null when not resolved
   * (yet). Schedules a background run when the cache is stale; {@code onUpdated}
   * fires on the EDT after the run completes. Safe to call from read actions.
   */
  @Nullable
  public HaxeBuildFileInfo getCachedOrSchedule(@NotNull HaxeBuildFile buildFile,
                                               @NotNull String targetFlag,
                                               @Nullable String preferredSdkName,
                                               @NotNull Runnable onUpdated) {
    // evaluation executes project code: hxp scripts run via haxe --run, and
    // the lime/openfl display fallback runs the tool from the project's
    // local .haxelib - consumers fall back to declared XML values
    if (!HaxeProjectTrust.checkForBackgroundEvaluation(project)) {
      return null;
    }
    String haxelibPath = HaxeToolPathResolver.resolveHaxelibExecutable(project, preferredSdkName);
    String haxePath = HaxeToolPathResolver.resolveHaxeExecutable(project, preferredSdkName);
    Key key = new Key(buildFile.file().getPath(), targetFlag, haxelibPath);
    long stamp = buildFile.file().getModificationStamp();

    // read here: the executor thread must not touch the VirtualFile
    String tool = LimeProjects.toolFor(buildFile.type());
    VirtualFile parent = buildFile.file().getParent();

    String workDirectory = parent != null ? parent.getPath() : project.getBasePath();
    String fileName = buildFile.file().getName();

    return cache.getCachedOrSchedule(key, stamp, () -> evaluate(key, haxePath, tool, fileName, workDirectory), onUpdated);
  }

  public void clearCache() {
    cache.clear();
  }

  /** Drops every cached evaluation of one build file (all targets/toolchains), forcing a re-run on the next ask. */
  public void invalidate(@NotNull String filePath) {
    cache.invalidate(filePath);
  }

  /** {@code settled} = the parser tool succeeded, or no tool is bundled (legacy is the best available). */
  @NotNull
  private Outcome<HaxeBuildFileInfo> evaluate(@NotNull Key key,
                                              @NotNull String haxePath,
                                              @NotNull String tool,
                                              @NotNull String fileName,
                                              @Nullable String workDirectory) {
    Path parserJar = bundledParserJar();
    HaxeBuildFileInfo info = null;
    boolean settled = parserJar == null;

    if (parserJar != null) {
      info = runParserTool(parserJar, key, workDirectory, haxePath);
      settled = info != null;
    }

    if (info == null) {
      // resilience over purity: a tool failure falls back to lime display
      info = runDisplay(key, tool, fileName, workDirectory);
    }

    return new Outcome<>(info, settled);
  }

  // --- bundled parser tool path ---

  @Nullable
  private HaxeBuildFileInfo runParserTool(@NotNull Path parserJar,
                                          @NotNull Key key,
                                          @Nullable String workDirectory,
                                          @NotNull String haxePath) {
    // the tool derives the target's own defines from --target, as lime does
    List<String> command = List.of(
      javaExecutable(),
      "-jar", parserJar.toString(),
      key.filePath(),
      "--target", key.targetFlag(),
      "--haxe", haxePath,
      "--haxelib", key.haxelibPath());
    GeneralCommandLine commandLine = new GeneralCommandLine(command).withWorkDirectory(workDirectory);

    try {
      ProcessOutput output = new CapturingProcessHandler(commandLine).runProcess(DISPLAY_TIMEOUT_MS);
      if (output.isTimeout() || output.getExitCode() != 0) {
        log.warn("LimeProjectParser failed for " + key.filePath() + " (" + key.targetFlag() + "): " + firstErrorLine(output));
        return null;
      }

      return parseToolOutput(output.getStdout(), key.targetFlag());
    }
    catch (ExecutionException e) {
      log.warn("LimeProjectParser could not run for " + key.filePath() + ": " + e.getMessage());
      return null;
    }
  }

  @Nullable
  static HaxeBuildFileInfo parseToolOutput(@NotNull String stdout, @NotNull String targetFlag) {
    try {
      LimeParserOutput parsed = PARSER_OUTPUT_MAPPER.readValue(StringUtil.trimTrailing(stdout), LimeParserOutput.class);

      List<HaxeDefine> defines = parsed.haxedefs().entrySet().stream()
        .map(entry -> new HaxeDefine(entry.getKey(), StringUtil.nullize(entry.getValue())))
        .toList();

      List<HaxeLibDependency> libraries = parsed.haxelibs().stream()
        .map(library -> new HaxeLibDependency(library.name(), StringUtil.nullize(library.version())))
        .toList();

      HaxeTarget target = LimeProjects.targetFor(targetFlag);
      String outputDirectory = parsed.config().get(LimeProjects.outputDirectoryConfigKey(targetFlag));
      String targetOutput = LimeProjects.relativeTargetOutput(targetFlag, parsed.app().path(), parsed.app().file(), outputDirectory);
      return new HaxeBuildFileInfo(target, targetOutput, defines, libraries, List.copyOf(parsed.sources()));
    }
    catch (Exception e) {
      log.warn("LimeProjectParser output was not parseable: " + e.getMessage());
      return null;
    }
  }

  @NotNull
  private static String javaExecutable() {
    return Path.of(System.getProperty("java.home"), "bin", SystemInfo.isWindows ? "java.exe" : "java").toString();
  }

  /** The evaluator jar shipped inside the plugin directory, or null when absent (fallback applies). */
  @Nullable
  private static Path bundledParserJar() {
    return HaxePluginPaths.bundledFile(PARSER_RELATIVE_PATH);
  }

  // --- legacy lime display fallback ---

  // TODO: point the display at an export root no build uses (LimeProjects.displayCommandLine) - lime prints the
  //  cached <app path>/<target>/haxe/<build type>.hxml whenever it is newer than the project file
  @Nullable
  private HaxeBuildFileInfo runDisplay(@NotNull Key key,
                                       @NotNull String tool,
                                       @NotNull String fileName,
                                       @Nullable String workDirectory) {
    GeneralCommandLine commandLine =
      new GeneralCommandLine(key.haxelibPath(), "run", tool, "display", fileName, key.targetFlag())
        .withWorkDirectory(workDirectory);
    try {
      ProcessOutput output = new CapturingProcessHandler(commandLine).runProcess(DISPLAY_TIMEOUT_MS);
      if (output.isTimeout() || output.getExitCode() != 0) {
        log.warn(tool + " display failed for " + fileName + " (" + key.targetFlag() + "): " + firstErrorLine(output));
        return null;
      }
      return parseDisplayOutput(output.getStdout(), key.targetFlag(), projectFileContent(key.filePath()));
    }
    catch (ExecutionException e) {
      log.warn(tool + " display could not run for " + fileName + ": " + e.getMessage());
      return null;
    }
  }

  /// The `lime display` output read as hxml: defines, libraries and class paths
  /// come from it, but target and output follow Lime's export layout, as with
  /// the bundled project parser. The hxml's own output flag
  /// (`-cpp bin/android/obj`) names the intermediate compile folder, which would
  /// make a mobile build look like a C++ executable that can be launched.
  @NotNull
  static HaxeBuildFileInfo parseDisplayOutput(@NotNull String stdout, @NotNull String targetFlag, @NotNull String projectXml) {
    HaxeTarget target = LimeProjects.targetFor(targetFlag);
    String appPath = LimeProjects.appPath(projectXml);
    String declaredAppFile = StringUtil.notNullize(ProjectXmlParser.parseAppFile(projectXml));
    // TODO: the display output carries no <config> values, so a custom
    //  <platform>.output-directory is only honoured on the parser-tool path
    String targetOutput = LimeProjects.relativeTargetOutput(targetFlag, appPath, declaredAppFile, null);
    return HxmlFileParser.parse(stdout).withTarget(target, targetOutput);
  }

  /** The project file's text read from disk (the executor thread must not touch the VirtualFile); empty when unreadable, so lime's defaults apply. */
  @NotNull
  private static String projectFileContent(@NotNull String filePath) {
    try {
      return Files.readString(Path.of(filePath));
    }
    catch (IOException e) {
      log.warn("Could not read " + filePath + " for its app path and file: " + e.getMessage());
      return "";
    }
  }

  @Override
  public void dispose() {
    cache.shutdown();
  }

  /// `haxedefs` is every -D the lime build passes to haxe for the target. The
  /// tool's `defines` member (lime's own condition defines: `<set>` names,
  /// target seeds) is not mapped - the IDE's define set is the compiler's.
  /// `config` holds the project's `<config>` values flattened to lime's dot
  /// keys (`air.output-directory`), as the tool reports them.
  private record LimeParserOutput(Map<String, String> haxedefs,
                                  List<ParserHaxelib> haxelibs,
                                  List<String> sources,
                                  ParserApp app,
                                  Map<String, String> config) {

    private LimeParserOutput {
      haxedefs = haxedefs != null ? haxedefs : Map.of();
      haxelibs = haxelibs != null ? haxelibs : List.of();
      sources = sources != null ? sources : List.of();
      app = app != null ? app : new ParserApp(null, null);
      config = config != null ? config : Map.of();
    }

  }

  private record ParserHaxelib(String name, String version) {
  }

  /// `path` defaults to lime's export root when the project sets none
  /// ("Export" is only an openfl-template convention, not the tool default).
  private record ParserApp(String path, String file) {
    private ParserApp {
      path = path != null ? path : LimeProjects.DEFAULT_APP_PATH;
      file = file != null ? file : "";
    }
  }
}
