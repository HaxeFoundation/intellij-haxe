package com.intellij.plugins.haxe.v2.buildtools.info;

import com.intellij.plugins.haxe.v2.buildtools.LimeProjects;
import com.intellij.plugins.haxe.v2.buildtools.HaxeProjectTrust;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeProjectInfoCache.Key;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeProjectInfoCache.Outcome;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.util.HaxePluginPaths;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.intellij.plugins.haxe.v2.buildtools.info.HaxeProjectInfoCache.firstErrorLine;

/**
 * Resolves the effective compiler configuration of Lime/OpenFL/HXP project files.
 * Primary path: the bundled LimeProjectParser jar (tools/LimeProjectParser.jar,
 * run on the IDE's own JRE) evaluates the project natively and returns structured
 * JSON — defines, haxelibs WITH their identity, sources and app data. Fallback
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
  private static final ObjectMapper PARSER_OUTPUT_MAPPER = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
    List<String> command = new ArrayList<>(List.of(
      javaExecutable(),
      "-jar", parserJar.toString(),
      key.filePath(),
      "--target", key.targetFlag(),
      "--haxe", haxePath,
      "--haxelib", key.haxelibPath()));

    for (String seed : seedDefines(key.targetFlag())) {
      command.add("-D");
      command.add(seed);
    }

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

      List<HaxeDefine> defines = parsed.defines().entrySet().stream()
        .map(entry -> new HaxeDefine(entry.getKey(), StringUtil.nullize(entry.getValue())))
        .toList();

      List<HaxeLibDependency> libraries = parsed.haxelibs().stream()
        .map(library -> new HaxeLibDependency(library.name(), StringUtil.nullize(library.version())))
        .toList();

      HaxeTarget target = LimeProjects.targetFor(targetFlag);
      String targetOutput = LimeProjects.relativeTargetOutput(targetFlag, parsed.app().path(), parsed.app().file());
      return new HaxeBuildFileInfo(target, targetOutput, defines, libraries, List.copyOf(parsed.sources()));
    }
    catch (Exception e) {
      log.warn("LimeProjectParser output was not parseable: " + e.getMessage());
      return null;
    }
  }

  /** Approximates the condition defines lime seeds before parsing: the target id plus its platform family. */
  @NotNull
  private static List<String> seedDefines(@NotNull String targetFlag) {
    // TODO: verify the seeds against real `lime display` output per target
    List<String> seeds = new ArrayList<>();
    seeds.add(targetFlag);
    switch (targetFlag) {
      case "html5" -> seeds.add("web");
      case "android", "ios" -> {
        seeds.add("mobile");
        seeds.add("native");
      }
      case "windows", "mac", "linux" -> {
        seeds.add("desktop");
        seeds.add("native");
      }
      case "hl", "neko", "java", "cs" -> {
        seeds.add("desktop");
        // pseudo-targets run on the host platform, which lime also defines
        seeds.add(LimeProjects.hostPlatformTarget());
      }
      default -> { }
    }
    return seeds;
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
      return HxmlFileParser.parse(output.getStdout());
    }
    catch (ExecutionException e) {
      log.warn(tool + " display could not run for " + fileName + ": " + e.getMessage());
      return null;
    }
  }

  @Override
  public void dispose() {
    cache.shutdown();
  }

  private record LimeParserOutput(Map<String, String> defines,
                                  List<ParserHaxelib> haxelibs,
                                  List<String> sources,
                                  ParserApp app) {

    private LimeParserOutput {
      defines = defines != null ? defines : Map.of();
      haxelibs = haxelibs != null ? haxelibs : List.of();
      sources = sources != null ? sources : List.of();
      app = app != null ? app : new ParserApp(null, null);
    }

  }

  private record ParserHaxelib(String name, String version) {
  }

  /// `path` defaults to lime's export root "bin" when the project sets none
  /// ("Export" is only an openfl-template convention, not the tool default).
  private record ParserApp(String path, String file) {
    private ParserApp {
      path = path != null ? path : "bin";
      file = file != null ? file : "";
    }
  }
}
