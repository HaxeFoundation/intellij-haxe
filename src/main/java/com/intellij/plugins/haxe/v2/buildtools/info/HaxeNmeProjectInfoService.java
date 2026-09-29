package com.intellij.plugins.haxe.v2.buildtools.info;

import com.intellij.plugins.haxe.v2.buildtools.HaxeProjectTrust;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlFileParser;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeProjectInfoCache.Key;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeProjectInfoCache.Outcome;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.intellij.plugins.haxe.v2.buildtools.info.HaxeProjectInfoCache.firstErrorLine;

/**
 * Resolves the effective compiler configuration of NME project files (nmml).
 * The nme tool has no side-effect-free display command, but its {@code prepare}
 * command runs the full project evaluation (conditionals, include.nmml
 * transitives, asset handlers) and generates the build hxml without compiling;
 * {@code -bin} redirects everything into a temp directory so the project tree
 * stays untouched. The generated hxml flattens haxelibs into classpaths and
 * carries no -lib lines, so library identities are recovered from the
 * classpaths that point into the haxelib repository.
 *
 * Results are cached per (file, target, toolchain) and refreshed in the
 * background via {@link HaxeProjectInfoCache} - callers get the cached value
 * (possibly stale, possibly null on first ask) immediately and a callback once
 * a refresh lands. A failed run retries a bounded number of times per file
 * revision, so a broken toolchain never causes refresh loops.
 */
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeNmeProjectInfoService implements Disposable {

  private static final int PREPARE_TIMEOUT_MS = 60_000;
  private static final String BUILD_FILE_MARKER = "PREPARE BUILD_FILE=";

  // a haxelib-repo path segment naming an installed version: haxelib stores
  // versions with commas (6,3,149) or as "git"; the segment BEFORE it is the
  // library name (<repo>/<name>/<version>/...)
  private static final Pattern HAXELIB_VERSION_SEGMENT = Pattern.compile("\\d+(?:,\\d+)+|git");

  /**
   * One landed evaluation: the parsed info for the tool window plus the
   * generated build hxml's content — the display service builds the
   * compilation-server context from it. The hxml references the RETAINED
   * prepared directory (generated ApplicationMain, boot classpath, asset
   * resources), which lives until the evaluation is replaced or invalidated.
   */
  public record Evaluation(@NotNull HaxeBuildFileInfo info, @NotNull String hxmlContent) {
  }

  private record PreparedRun(@NotNull Evaluation evaluation, @NotNull Path preparedDir) {
  }

  private final Project project;
  private final HaxeProjectInfoCache<PreparedRun> cache;

  public HaxeNmeProjectInfoService(@NotNull Project project) {
    this.project = project;
    this.cache = new HaxeProjectInfoCache<>(project, "Haxe nme prepare", HaxeNmeProjectInfoService::deletePreparedDir);
  }

  @NotNull
  public static HaxeNmeProjectInfoService getInstance(@NotNull Project project) {
    return project.getService(HaxeNmeProjectInfoService.class);
  }

  /**
   * The file's evaluation for the target, or null when not resolved (yet).
   * Schedules a background run when the cache is stale; {@code onUpdated}
   * fires on the EDT after the run completes. Safe to call from read actions.
   */
  @Nullable
  public Evaluation getCachedOrSchedule(@NotNull HaxeBuildFile buildFile,
                                        @NotNull String targetFlag,
                                        @Nullable String preferredSdkName,
                                        @NotNull Runnable onUpdated) {
    // prepare runs the nme tool from the project's local .haxelib and
    // evaluates the project build description - project code
    if (!HaxeProjectTrust.checkForBackgroundEvaluation(project)) {
      return null;
    }
    String haxelibPath = HaxeToolPathResolver.resolveHaxelibExecutable(project, preferredSdkName);
    Key key = new Key(buildFile.file().getPath(), targetFlag, haxelibPath);
    long stamp = buildFile.file().getModificationStamp();

    // captured here - the executor thread must not touch the VirtualFile
    VirtualFile parent = buildFile.file().getParent();
    String workDirectory = parent != null ? parent.getPath() : project.getBasePath();
    String fileName = buildFile.file().getName();
    PreparedRun run = cache.getCachedOrSchedule(key, stamp, () -> evaluate(key, fileName, workDirectory), onUpdated);
    return run != null ? run.evaluation() : null;
  }

  public void clearCache() {
    cache.clear();
  }

  /** Drops every cached evaluation of one build file (all targets/toolchains), forcing a re-run on the next ask. */
  public void invalidate(@NotNull String filePath) {
    cache.invalidate(filePath);
  }

  @NotNull
  private Outcome<PreparedRun> evaluate(@NotNull Key key, @NotNull String fileName, @Nullable String workDirectory) {
    PreparedRun run = runPrepare(key, fileName, workDirectory);
    return new Outcome<>(run, run != null);
  }

  /**
   * Runs one prepare into a fresh temp directory. On success the directory is
   * RETAINED (the returned evaluation's hxml references its generated sources
   * and resources — the compilation server reads them per request) and only
   * released when the cache entry is replaced or invalidated; a failed run
   * cleans up immediately.
   */
  @Nullable
  private PreparedRun runPrepare(@NotNull Key key,
                                 @NotNull String fileName,
                                 @Nullable String workDirectory) {
    Path tempBin = null;
    PreparedRun run = null;
    try {
      tempBin = Files.createTempDirectory("haxe-nme-prepare");
      List<String> command = List.of(key.haxelibPath(), "run", "nme", "prepare",
                                     fileName, key.targetFlag(), "-bin", tempBin.toString());
      GeneralCommandLine commandLine = new GeneralCommandLine(command).withWorkDirectory(workDirectory);
      ProcessOutput output = new CapturingProcessHandler(commandLine).runProcess(PREPARE_TIMEOUT_MS);
      if (output.isTimeout() || output.getExitCode() != 0) {
        log.warn("nme prepare failed for " + fileName + " (" + key.targetFlag() + "): " + firstErrorLine(output));
        return null;
      }
      Evaluation evaluation = parsePreparedBuildFile(output.getStdout(), workDirectory, fileName);
      if (evaluation != null) {
        run = new PreparedRun(evaluation, tempBin);
      }
      return run;
    }
    catch (Exception e) {
      log.warn("nme prepare could not run for " + fileName + ": " + e.getMessage());
      return null;
    }
    finally {
      if (tempBin != null && run == null) {
        FileUtil.delete(tempBin.toFile());
      }
    }
  }

  @Nullable
  private static Evaluation parsePreparedBuildFile(@NotNull String stdout,
                                                   @Nullable String workDirectory,
                                                   @NotNull String fileName) throws Exception {
    String buildFilePath = stdout.lines()
      .filter(line -> line.startsWith(BUILD_FILE_MARKER))
      .map(line -> line.substring(BUILD_FILE_MARKER.length()).trim())
      .findFirst()
      .orElse(null);
    if (buildFilePath == null) {
      log.warn("nme prepare printed no build file marker for " + fileName);
      return null;
    }

    Path hxml = Path.of(buildFilePath);
    if (!hxml.isAbsolute() && workDirectory != null) {
      hxml = Path.of(workDirectory).resolve(hxml);
    }
    String content = Files.readString(hxml);
    HaxeBuildFileInfo parsed = HxmlFileParser.parse(content);
    List<HaxeLibDependency> libraries = withDerivedLibraries(parsed.libraries(), parsed.classpaths());
    return new Evaluation(parsed.withLibraries(libraries), content);
  }

  private static void deletePreparedDir(@NotNull PreparedRun run) {
    FileUtil.delete(run.preparedDir().toFile());
  }

  /** The hxml's explicit -lib entries plus the identities its haxelib-repo classpaths encode. */
  @NotNull
  private static List<HaxeLibDependency> withDerivedLibraries(@NotNull List<HaxeLibDependency> declared,
                                                              @NotNull List<String> classpaths) {
    Map<String, HaxeLibDependency> byName = new LinkedHashMap<>();
    for (HaxeLibDependency library : declared) {
      byName.putIfAbsent(library.name(), library);
    }
    for (String classpath : classpaths) {
      HaxeLibDependency library = libraryOfRepoClasspath(classpath);
      if (library != null) {
        byName.putIfAbsent(library.name(), library);
      }
    }
    return List.copyOf(byName.values());
  }

  @Nullable
  private static HaxeLibDependency libraryOfRepoClasspath(@NotNull String classpath) {
    // either path separator
    String[] segments = classpath.split("[/\\\\]");
    for (int i = 1; i < segments.length; i++) {
      if (HAXELIB_VERSION_SEGMENT.matcher(segments[i]).matches()) {
        String version = "git".equals(segments[i]) ? null : segments[i].replace(',', '.');
        return new HaxeLibDependency(segments[i - 1], version);
      }
    }
    return null;
  }

  @Override
  public void dispose() {
    cache.shutdown();
  }
}
