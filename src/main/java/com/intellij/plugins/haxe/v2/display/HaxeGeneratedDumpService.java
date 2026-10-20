package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.display.protocol.DisplayMethods;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.DisplayResponse;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlArguments;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlFileParser;
import com.intellij.psi.util.PsiModificationTracker;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/// Produces `-D dump=pretty` typed-AST dumps of a build context, which the
/// generated-code preview renders as the post-macro program. A dump needs a
/// real generation pass through the compilation server, because a
/// `--no-output` compile writes no dumps. The build's output is redirected
/// into the same IDE-owned directory as the dumps, so a preview build never
/// touches the user's output or project tree.
///
/// One dump pass writes every module of the compilation, so the first
/// navigation pays for all later ones. Results are cached per context and
/// reused until the next code change. Background threads only.
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeGeneratedDumpService {

  private record DumpState(long psiModCount, long sourcesStamp, @NotNull Path targetDumpDir) {
  }

  /** What a dump build came to: the per-target dump directory, or the reason there is none, as shown to the user. */
  public sealed interface Outcome permits Dumped, Failed {
  }

  /** The per-target dump directory, and the {@link HaxeSavedSourcesStamp} value its dump build started from. */
  public record Dumped(@NotNull Path targetDumpDir, long sourcesStamp) implements Outcome {
  }

  public record Failed(@NotNull String message) implements Outcome {
  }

  /** A dump-ready argument list plus the target it compiles, which names the dump subdirectory. */
  record DumpBuild(@NotNull List<String> args, @NotNull HaxeTarget target) {
  }

  private static final int WINDOWS_MAX_PATH = 260;
  /** Room reserved below the dump root for {@code dump/<target>/<package path>/<Module>.dump}. */
  private static final int DUMP_TREE_HEADROOM = 120;

  private final Project project;
  private final Map<String, DumpState> states = new ConcurrentHashMap<>();

  public HaxeGeneratedDumpService(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  public static HaxeGeneratedDumpService getInstance(@NotNull Project project) {
    return project.getService(HaxeGeneratedDumpService.class);
  }

  /**
   * The context's dump directory (the per-target root holding
   * {@code pack/Module.dump} files), running a dump build if the cached one
   * is stale, or why the context cannot dump (no server, unsupported
   * argument shape, failed compile). Blocking, so background threads only;
   * callers wrap this in a cancelable progress task.
   */
  @NotNull
  public synchronized Outcome ensureDumps(@NotNull HaxeCompilerDisplayService.DisplayContext context) {
    String contextKey = HaxeCompilerDisplayService.contextKey(context);
    long modCount = PsiModificationTracker.getInstance(project).getModificationCount();
    long sourcesStamp = HaxeSavedSourcesStamp.getInstance(project).get();
    DumpState cached = states.get(contextKey);
    if (cached != null && cached.psiModCount() == modCount && Files.isDirectory(cached.targetDumpDir())) {
      return new Dumped(cached.targetDumpDir(), cached.sourcesStamp());
    }

    HaxeCompilerDisplayService displayService = HaxeCompilerDisplayService.getInstance(project);
    HaxeCompilerDisplayService.Connected connected = displayService.connectFor(context, DisplayMethods.SERVER_TYPE);
    if (connected == null) return new Failed(HaxeBundle.message("haxe.generated.preview.failed.no.server"));

    Path dumpRoot = dumpRootFor(contextKey);
    DumpBuild dumpBuild = dumpArgs(HxmlArguments.expandReferences(connected.args()), dumpRoot);
    if (dumpBuild == null) {
      log.info("dump build unsupported for this argument shape: " + connected.args());
      return new Failed(HaxeBundle.message("haxe.generated.preview.failed.arguments"));
    }

    // the compiler's dump writer (and some generators) create only the last
    // path segment, and a missing parent is fatal, so the tree must exist
    // before the build
    try {
      Files.createDirectories(dumpRoot.resolve("dump"));
      Files.createDirectories(dumpRoot.resolve("out"));
    } catch (IOException e) {
      log.info("cannot create dump directories under " + dumpRoot + ": " + e.getMessage());
      return new Failed(HaxeBundle.message("haxe.generated.preview.failed.directories"));
    }

    Path hlScratch = prepareHlScratchDir(dumpBuild);
    try {
      DisplayResponse response = connected.client().exchange(dumpBuild.args(), HaxeCompilerDisplayService.CONTEXT_COMPILE_TIMEOUT_MS);
      if (response.hasError()) {
        log.info("dump build reported errors: " + response.payload());
        return new Failed(HaxeBundle.message("haxe.generated.preview.failed.compile"));
      }

      Path targetDir = targetDumpDir(dumpRoot.resolve("dump"), dumpBuild.target());
      if (targetDir == null) {
        log.info("dump build produced no dump directory under " + dumpRoot);
        return new Failed(HaxeBundle.message("haxe.generated.preview.failed.no.dump.dir"));
      }
      // both stamps sampled BEFORE the compile: an edit made while dumping
      // invalidates this state on the next lookup instead of being missed
      states.put(contextKey, new DumpState(modCount, sourcesStamp, targetDir));
      return new Dumped(targetDir, sourcesStamp);
    } catch (DisplayRequestException e) {
      log.info("dump build failed: " + e.getMessage());
      return new Failed(HaxeBundle.message("haxe.generated.preview.failed.request", e.getMessage()));
    } finally {
      removeHlScratchDir(hlScratch);
    }
  }

  /**
   * The HashLink generator writes two extra dump files
   * ({@code dump/hlopt.txt}, {@code dump/hlcode.txt}) RELATIVE to the
   * compile's working directory, ignoring dump-path. The whole build fails
   * when that directory is missing, so for HL dump builds it is created up
   * front and removed afterwards. Returns the created directory, or null when
   * nothing was created. A PRE-EXISTING {@code dump/} belongs to the user and
   * keeps whatever the compiler writes into it.
   */
  @Nullable
  private static Path prepareHlScratchDir(@NotNull DumpBuild dumpBuild) {
    if (dumpBuild.target() != HaxeTarget.HL) return null;
    Path cwd = HxmlArguments.workDirectory(dumpBuild.args());
    if (cwd == null) return null;
    Path scratch = cwd.resolve("dump");
    if (Files.isDirectory(scratch)) return null;
    try {
      Files.createDirectories(scratch);
      return scratch;
    } catch (IOException e) {
      log.info("cannot create hl dump scratch dir " + scratch + ": " + e.getMessage());
      return null;
    }
  }

  private static void removeHlScratchDir(@Nullable Path scratch) {
    if (scratch == null) return;
    try {
      Files.deleteIfExists(scratch.resolve("hlopt.txt"));
      Files.deleteIfExists(scratch.resolve("hlcode.txt"));
      Files.deleteIfExists(scratch);
    } catch (IOException e) {
      log.info("cannot clean hl dump scratch dir " + scratch + ": " + e.getMessage());
    }
  }

  /**
   * The dump file of a module by dot path ({@code pack.Module} →
   * {@code pack/Module.dump}). A type generated by {@code Context.defineType}
   * may live in a module whose path differs from the type's dot path, so a
   * miss falls back to scanning for a file named after the LAST segment.
   */
  @Nullable
  public Path findModuleDump(@NotNull Path targetDumpDir, @NotNull String dotPath) {
    Path direct = targetDumpDir.resolve(dotPath.replace('.', '/') + ".dump");
    if (Files.isRegularFile(direct)) return direct;

    String fileName = StringUtil.getShortName(dotPath) + ".dump";
    try (Stream<Path> files = Files.walk(targetDumpDir)) {
      return files.filter(f -> f.getFileName().toString().equals(fileName)).findFirst().orElse(null);
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * The dump root: under the IDE system directory, or under the OS temp
   * directory when the system directory is too deep for the 260-character
   * path limit of Windows (a development sandbox nests it inside the
   * project). The compiler's dump writer fails midway once the root plus the
   * package tree exceed the limit. If the temp directory gets cleaned, the
   * next navigation simply dumps again.
   */
  @NotNull
  private Path dumpRootFor(@NotNull String contextKey) {
    String key = project.getLocationHash() + "|" + contextKey;
    String hash = Integer.toHexString(key.hashCode());
    Path systemRoot = PathManager.getSystemDir().resolve("haxe-dump").resolve(hash);
    boolean fitsWindowsCap = !SystemInfo.isWindows
                             || systemRoot.toString().length() + DUMP_TREE_HEADROOM <= WINDOWS_MAX_PATH;
    if (fitsWindowsCap) return systemRoot;
    return Path.of(FileUtil.getTempDirectory(), "haxe-dump", hash);
  }

  /// The build's argument list rewritten for a side-effect-free dump pass:
  /// output redirected into the dump root, native compilation and `-cmd`
  /// post-build steps dropped, dump defines appended, and the entry point of
  /// an `--interp` build restated so that eval dumps without running it
  /// ([#entryPointArgs]). The target and its defines stay the user's. Null
  /// when the argument shape cannot be made safe (`-x`/`--run` execute the
  /// program) or no target flag is visible.
  /// TODO: `--next` sections get the dump defines appended only after
  ///  the last section; multi-build hxml dumps only that section's modules.
  @Nullable
  static DumpBuild dumpArgs(@NotNull List<String> buildArgs, @NotNull Path dumpRoot) {
    List<String> args = new ArrayList<>(buildArgs.size() + 8);
    HaxeTarget dumpTarget = null;
    String mainClass = null;
    for (int i = 0; i < buildArgs.size(); i++) {
      String arg = buildArgs.get(i);
      if (HxmlFileParser.isRunFlag(arg)) return null;
      if (HxmlFileParser.COMMAND_FLAGS.contains(arg)) {
        i++;
        continue;
      }
      // lime's display hxml ends with --no-output (it exists for IDE
      // completion), but without a generation pass there are no dumps; the
      // output is redirected into the dump root anyway
      if (arg.equals(HxmlFileParser.NO_OUTPUT_FLAG)) continue;
      if (HxmlFileParser.isMainFlag(arg) && i + 1 < buildArgs.size()) {
        mainClass = buildArgs.get(i + 1);
        i++;
        continue;
      }
      HaxeTarget target = HxmlFileParser.targetForFlag(arg);
      if (target == HaxeTarget.INTERP) dumpTarget = target;
      // --interp is a target flag but takes no output argument, so it must
      // not swallow the next argument
      boolean producesOutput = target != null && (target.isOutputToDirectory() || target.isOutputToSingleFile());
      boolean redirectableOutput = producesOutput && i + 1 < buildArgs.size();
      if (redirectableOutput) {
        args.add(arg);
        args.add(redirectedOutput(dumpRoot, target));
        dumpTarget = target;
        i++;
        continue;
      }
      args.add(arg);
    }
    // nothing generates, so nothing is dumped
    if (dumpTarget == null) return null;

    args.addAll(entryPointArgs(mainClass, dumpTarget));
    String dumpPath = "dump-path=" + dumpRoot.resolve("dump");
    args.addAll(List.of("-D", "no-compilation", "-D", "dump=pretty", "-D", dumpPath));
    return new DumpBuild(args, dumpTarget);
  }

  /**
   * The entry point as the dump build states it. Eval runs main right after
   * dumping, so an interp build lists the main class without an entry point
   * and keeps it for dead-code elimination instead: the same modules and
   * fields survive as in the real build, and nothing runs.
   */
  @NotNull
  private static List<String> entryPointArgs(@Nullable String mainClass, @NotNull HaxeTarget dumpTarget) {
    if (mainClass == null) return List.of();
    if (dumpTarget != HaxeTarget.INTERP) return List.of("-main", mainClass);
    return List.of(mainClass, "--macro", "keep('" + mainClass + "')");
  }

  @NotNull
  private static String redirectedOutput(@NotNull Path dumpRoot, @NotNull HaxeTarget target) {
    Path out = dumpRoot.resolve("out");
    if (target.isOutputToDirectory()) return out.toString();
    return out.resolve(target.getTargetFileNameWithExtension("dumpbuild")).toString();
  }

  /**
   * The target's dump directory: {@code <dump-path>/<target>/} on Haxe 4,
   * nested under a stage directory ({@code AfterDce/}) on Haxe 5. A build
   * that runs macros ALSO writes a {@code macro/} sibling with the macro
   * interpreter's modules, so the directory is found by the target's name,
   * never guessed.
   */
  @Nullable
  static Path targetDumpDir(@NotNull Path dumpParent, @NotNull HaxeTarget target) {
    String directoryName = dumpDirectoryNameOf(target);
    Path direct = dumpParent.resolve(directoryName);
    if (Files.isDirectory(direct)) return direct;
    try (Stream<Path> stages = Files.list(dumpParent)) {
      return stages.filter(Files::isDirectory)
        .map(stage -> stage.resolve(directoryName))
        .filter(Files::isDirectory)
        .findFirst()
        .orElse(null);
    } catch (IOException e) {
      return null;
    }
  }

  /** The interpreter dumps under its platform name; every other target under its output directory name. */
  @NotNull
  private static String dumpDirectoryNameOf(@NotNull HaxeTarget target) {
    return target == HaxeTarget.INTERP ? "eval" : target.getDefaultOutputSubdirectory();
  }

  public void clearCaches() {
    states.clear();
  }
}
