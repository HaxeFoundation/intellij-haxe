package com.intellij.plugins.haxe.matrix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Debugger compatibility matrix: provisions the haxe/HashLink toolchains
 * into {@code <repo>/debuggerResources}, runs each debugger lane's test
 * suite per toolchain, and writes the HTML report. Entry point of
 * {@code gradlew debuggerCompatibilityReport}; see README.md.
 *
 * The lane mechanics encode hard-won lessons from the runs that certified
 * the version support policy — see the comments at the decision points and
 * in {@link GradleRunner} before "simplifying" any of them.
 */
public final class MatrixMain {
  private final Path root;
  private final Path resources;
  private final Path out;
  private final List<String> lanes;
  private final List<String> haxeFilter;
  private final List<String> hlFilter;
  private final boolean full;
  private final boolean parallelLanes;
  // -PdapTestForks the HL lane passes to its child builds (test classes in
  // parallel fork JVMs); default 4, always forwarded so it is authoritative.
  // 1 = sequential.
  private final int hlForks;
  // run start, baked into the report filename so successive runs never
  // overwrite each other's results
  private final String startedAt = LocalDateTime.now()
    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
  private final Log log;
  private final GradleRunner gradle;
  private final List<Results.Cell> cells = new ArrayList<>();

  private List<Path> haxeDirs = List.of();
  private List<Path> hlDirs = List.of();

  private static final List<String> HL_FIXTURE_TASKS = List.of(
    "buildTestFixture", "buildThreadsFixture", "buildSpinFixture", "buildUncaughtFixture",
    "buildVmFixture", "buildStackTraceFixture", "buildTypedThrowFixture");
  private static final Map<String, String> HL_FIXTURE_FILES = Map.of(
    "buildTestFixture", "test-fixture.hl", "buildThreadsFixture", "threads-fixture.hl",
    "buildSpinFixture", "spin-fixture.hl", "buildUncaughtFixture", "uncaught-fixture.hl",
    "buildVmFixture", "vm-fixture.hl", "buildStackTraceFixture", "stacktrace-fixture.hl",
    "buildTypedThrowFixture", "typedthrow-fixture.hl");
  // CLI flags (see README). Value flags end with '=' and are read with flagValue(),
  // which slices at the flag's own length - no hand-counted substring offsets.
  private static final String FLAG_LANES = "--lanes=";
  private static final String FLAG_HAXE = "--haxe=";
  private static final String FLAG_HL = "--hl=";
  private static final String FLAG_RESOURCES = "--resources=";
  private static final String FLAG_OUT = "--out=";
  private static final String FLAG_HL_FORKS = "--hl-forks=";
  private static final String FLAG_FULL = "--full";
  private static final String FLAG_PARALLEL_LANES = "--parallel-lanes";
  private static final String FLAG_REPORT_ONLY = "--report-only";

  // gradle argument literals reused across the lanes (named once so a rename is
  // one edit, not a hunt through every lane)
  private static final String GRADLE_CONTINUE = "--continue";
  private static final String GRADLE_NO_BUILD_CACHE = "--no-build-cache";
  private static final String GRADLE_EXCLUDE = "-x";
  private static final String GRADLE_TESTS = "--tests";
  private static final String TASK_CLEAN_TEST = ":cleanTest";
  private static final String TASK_TEST = ":test";
  private static final String TASK_BUILD_ADAPTER = "buildDebugAdapter";
  private static final String PROP_HASHLINK_BIN = "-PhashlinkBin=";
  private static final String PROP_DAP_TEST_FORKS = "-PdapTestForks=";

  private static final List<String> EXCLUDE_HAXELIB = List.of(
    GRADLE_EXCLUDE, "installFormatHaxelib", GRADLE_EXCLUDE, "registerDapProtocolHaxelib",
    GRADLE_EXCLUDE, "registerServerHaxelib", GRADLE_EXCLUDE, "installHscript");

  private MatrixMain(Path root, Path resources, Path out, List<String> lanes, List<String> haxeFilter,
                     List<String> hlFilter, boolean full, boolean parallelLanes, int hlForks) throws IOException {
    this.root = root;
    this.resources = resources;
    this.out = out;
    this.lanes = lanes;
    this.haxeFilter = haxeFilter;
    this.hlFilter = hlFilter;
    this.full = full;
    this.parallelLanes = parallelLanes;
    this.hlForks = Math.max(1, hlForks);
    this.log = new Log(out.resolve("progress.log"));
    this.gradle = new GradleRunner(root, log);
  }

  public static void main(String[] args) throws Exception {
    Path root = Path.of(System.getProperty("matrix.root", ".")).toAbsolutePath().normalize();
    Path resources = root.resolve("debuggerResources");
    Path out = root.resolve("build/reports/debugger-matrix");
    List<String> lanes = List.of("eval", "hashlink", "hxcpp");
    List<String> haxeFilter = List.of();
    List<String> hlFilter = List.of();
    boolean full = false;
    boolean parallelLanes = false;
    int hlForks = 4;
    boolean reportOnly = false;
    for (String arg : args) {
      if (arg.startsWith(FLAG_LANES)) {
        // lane names are matched lowercase (run() tests contains("hashlink") etc.)
        lanes = flagValueList(arg, FLAG_LANES).stream().map(s -> s.toLowerCase(Locale.ROOT)).toList();
      } else if (arg.startsWith(FLAG_HAXE)) {
        haxeFilter = flagValueList(arg, FLAG_HAXE);
      } else if (arg.startsWith(FLAG_HL)) {
        hlFilter = flagValueList(arg, FLAG_HL);
      } else if (arg.startsWith(FLAG_RESOURCES)) {
        resources = flagValuePath(arg, FLAG_RESOURCES);
      } else if (arg.startsWith(FLAG_OUT)) {
        out = flagValuePath(arg, FLAG_OUT);
      } else if (arg.equals(FLAG_FULL)) {
        full = true;
      } else if (arg.equals(FLAG_PARALLEL_LANES)) {
        parallelLanes = true;
      } else if (arg.startsWith(FLAG_HL_FORKS)) {
        hlForks = flagValueInt(arg, FLAG_HL_FORKS);
      } else if (arg.equals(FLAG_REPORT_ONLY)) {
        reportOnly = true;
      } else {
        System.err.println("unknown argument: " + arg);
        System.exit(2);
      }
    }
    MatrixMain matrix = new MatrixMain(root, resources, out, lanes, haxeFilter, hlFilter, full, parallelLanes, hlForks);
    if (reportOnly) {
      matrix.reportOnly();
    } else {
      matrix.run();
    }
  }

  /** The value of a {@code --flag=value} argument, sliced at the flag's own length. */
  private static String flagValue(String arg, String flag) {
    return arg.substring(flag.length());
  }

  private static List<String> flagValueList(String arg, String flag) {
    return Arrays.stream(flagValue(arg, flag).split(","))
      .map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  private static Path flagValuePath(String arg, String flag) {
    return Path.of(flagValue(arg, flag)).toAbsolutePath().normalize();
  }

  private static int flagValueInt(String arg, String flag) {
    return Integer.parseInt(flagValue(arg, flag).trim());
  }

  private void run() throws IOException {
    logConfig();
    Provisioner provisioner = new Provisioner(resources, log);
    haxeDirs = applyNameFilter(provisioner.haxeDirs(), haxeFilter);
    hlDirs = lanes.contains("hashlink") ? applyNameFilter(provisioner.hashlinkDirs(), hlFilter) : List.of();
    log.line("matrix start: lanes=" + String.join("+", lanes)
             + " haxe=" + names(haxeDirs) + " hl=" + names(hlDirs));
    if (haxeDirs.isEmpty()) {
      log.line("no haxe toolchains available - aborting");
      System.exit(1);
    }

    registerHaxelibsOnce();

    if (parallelLanes && lanes.size() > 1) {
      runLanesInParallel();
    } else {
      if (lanes.contains("eval")) {
        evalLane();
      }
      if (lanes.contains("hashlink")) {
        hashlinkLane();
      }
      if (lanes.contains("hxcpp")) {
        hxcppLane();
      }
    }
    restore();
    report();
    log.line("matrix done");
  }

  /** Logs the effective flag values before provisioning, so a run self-documents its config. */
  private void logConfig() {
    log.line("matrix config:");
    log.line("  lanes         = " + String.join("+", lanes));
    log.line("  haxe          = " + (haxeFilter.isEmpty() ? "all" : String.join(",", haxeFilter)));
    log.line("  hl            = " + (hlFilter.isEmpty() ? "all" : String.join(",", hlFilter)));
    log.line("  full          = " + full);
    log.line("  parallelLanes = " + parallelLanes);
    log.line("  hlForks       = " + hlForks);
    log.line("  resources     = " + resources);
    log.line("  out           = " + out);
  }

  /** The dirs whose file name is listed in {@code names}, or all of them when {@code names} is empty. */
  private static List<Path> applyNameFilter(List<Path> dirs, List<String> names) {
    return names.isEmpty() ? dirs
      : dirs.stream().filter(d -> names.contains(d.getFileName().toString())).toList();
  }

  /**
   * haxelib dev registrations ONCE, with the dev haxe, before any lane runs;
   * the lanes exclude these tasks so nothing races the shared haxelib repo.
   */
  private void registerHaxelibsOnce() {
    if (lanes.contains("hashlink") || lanes.contains("hxcpp")) {
      gradle.run(List.of(":debuggers:hashlink-debug-adapter:registerDapProtocolHaxelib",
                         ":debuggers:intellij-hxcpp-debugger:registerServerHaxelib"),
                 Map.of(), out.resolve("logs/preflight.log"), 300);
    }
  }

  /**
   * One thread per lane. The lanes are disjoint by construction — separate
   * gradle modules, separate fixtures, separate debugger binaries, and each
   * child build carries its own environment — so the only shared state is
   * this process (log/cells, synchronized) and the machine-wide stray-process
   * sweep, which must be deferred until every lane is done: it kills hl/haxe
   * by NAME, and one lane's sweep would kill another lane's live compiler.
   * Note the wall-clock win is bounded by the slowest lane (hashlink, by
   * far); eval+hxcpp just disappear inside it.
   */
  private void runLanesInParallel() {
    log.line("running " + lanes.size() + " lanes in parallel (one thread per lane)");
    GradleRunner.deferStrayKills = true;
    try {
      List<Thread> threads = new ArrayList<>();
      if (lanes.contains("eval")) {
        threads.add(laneThread("eval", this::evalLane));
      }
      if (lanes.contains("hashlink")) {
        threads.add(laneThread("hashlink", this::hashlinkLane));
      }
      if (lanes.contains("hxcpp")) {
        threads.add(laneThread("hxcpp", this::hxcppLane));
      }
      threads.forEach(Thread::start);
      for (Thread thread : threads) {
        try {
          thread.join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    } finally {
      GradleRunner.deferStrayKills = false;
      GradleRunner.killStraysNow();
    }
  }

  private interface Lane {
    void run() throws IOException;
  }

  // the "-lane" suffix makes Log tag every line this thread writes
  private Thread laneThread(String name, Lane lane) {
    return new Thread(() -> {
      try {
        lane.run();
      } catch (Exception e) {
        log.line("LANE CRASHED: " + e);
      }
    }, name + "-lane");
  }

  // ------------------------------------------------------------------ lanes

  private record SuiteRun(GradleRunner.Status status, List<Results.ClassResult> classes, List<String> flaky) {
  }

  /**
   * Runs a cell's test suite; when a HANDFUL of suites fail, they are rerun
   * once and the results merged — a test that fails then passes is reported
   * as FLAKY (machine load, not a version incompatibility) instead of
   * failing the cell. The debugger ITs drive real debuggees against wait
   * timeouts, so contention flakes are a fact of life; the certified-green
   * combos kept "failing surprisingly" on busy machines without this.
   */
  private SuiteRun runSuite(String modulePath, List<String> extraArgs, Map<String, String> env,
                            Path logFile, int timeoutSec, Path moduleResults, Path evidence) throws IOException {
    List<String> first = new ArrayList<>(List.of(modulePath + TASK_CLEAN_TEST, modulePath + TASK_TEST));
    first.addAll(extraArgs);
    GradleRunner.Status status = gradle.run(first, env, logFile, timeoutSec, true);
    GradleRunner.killStrays();
    List<Results.ClassResult> classes = Results.collect(moduleResults, evidence);
    List<Results.ClassResult> failing = classes.stream()
      .filter(c -> c.failures() + c.errors() > 0).toList();
    if (failing.isEmpty() || failing.size() > 8 || status == GradleRunner.Status.TIMEOUT) {
      return new SuiteRun(status, classes, List.of());
    }
    log.line("      " + failing.size() + " suite(s) failed - retrying them once to tell machine flakes from real failures");
    List<String> before = failing.stream()
      .flatMap(c -> c.failed().stream().map(f -> c.name() + "::" + f.test())).toList();
    preserveFirstAttempt(failing, evidence);
    List<String> retry = new ArrayList<>(List.of(modulePath + TASK_CLEAN_TEST, modulePath + TASK_TEST));
    for (Results.ClassResult failed : failing) {
      retry.add(GRADLE_TESTS);
      retry.add(failed.fqName());
    }
    retry.addAll(extraArgs);
    gradle.run(retry, env, Path.of(logFile + ".retry"), timeoutSec, true);
    GradleRunner.killStrays();
    overlayResults(moduleResults, evidence);
    List<Results.ClassResult> merged = Results.parse(evidence);
    List<String> after = merged.stream()
      .flatMap(c -> c.failed().stream().map(f -> c.name() + "::" + f.test())).toList();
    List<String> flaky = before.stream().filter(t -> !after.contains(t)).toList();
    if (!flaky.isEmpty()) {
      log.line("      flaky (passed on retry): " + String.join(", ", flaky));
    }
    return new SuiteRun(status, merged, flaky);
  }

  /**
   * Copies the first attempt's failure XMLs into {@code evidence/first-attempt}
   * before the retry overwrites them — a flake that "passed on retry" is
   * undiagnosable without the stack traces (and adapter output) that first
   * failed it.
   */
  private void preserveFirstAttempt(List<Results.ClassResult> failing, Path evidence) throws IOException {
    Path firstAttempt = evidence.resolve("first-attempt");
    Files.createDirectories(firstAttempt);
    for (Results.ClassResult failed : failing) {
      Path xml = evidence.resolve("TEST-" + failed.fqName() + ".xml");
      if (Files.isRegularFile(xml)) {
        Files.copy(xml, firstAttempt.resolve(xml.getFileName()), StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }

  /**
   * Overlays the retry's XMLs onto the evidence dir: the retried suites' results
   * REPLACE the first run's, while suites the retry did not touch keep their
   * first-run XMLs.
   */
  private void overlayResults(Path moduleResults, Path evidence) throws IOException {
    if (!Files.isDirectory(moduleResults)) {
      return;
    }
    try (var files = Files.list(moduleResults)) {
      for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".xml")).toList()) {
        Files.copy(file, evidence.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }

  private Map<String, String> haxeEnv(Path haxeDir) {
    Path binDir = Platform.findBinary(haxeDir, "haxe").getParent();
    Map<String, String> env = new LinkedHashMap<>();
    env.put("PATH", binDir + File.pathSeparator + System.getenv("PATH"));
    env.put("HAXE_STD_PATH", binDir.resolve("std").toString());
    return env;
  }

  /**
   * Tripwire: logs the haxe a CHILD process actually resolves under the
   * lane's environment. A leaked environment (a stale daemon, a surviving
   * "Path" case-variant) makes a lane compile with the WRONG haxe against
   * the lane's std — a confusing salad of std-typing errors on one user
   * machine — so a mismatch is called out loudly before the cell runs.
   */
  private void verifyLaneHaxe(String laneName, Map<String, String> env) {
    try {
      List<String> command = Platform.WINDOWS
        ? List.of("cmd", "/c", "haxe", "--version")
        : List.of("sh", "-c", "haxe --version");
      ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
      GradleRunner.applyEnv(builder.environment(), env);
      Process process = builder.start();
      String version = new String(process.getInputStream().readAllBytes()).trim();
      process.waitFor(15, TimeUnit.SECONDS);
      String expected = laneName.replaceFirst("^haxe_", "").replace('_', '.');
      boolean plainVersion = expected.matches("\\d+\\.\\d+\\.\\d+");
      String note = (plainVersion && !version.startsWith(expected))
        ? "   <-- WARNING: expected " + expected + "; the lane environment leaked (see README)"
        : "";
      log.line("    " + laneName + " : children resolve haxe " + version + note);
    } catch (Exception e) {
      log.line("    " + laneName + " : haxe resolution check failed (" + e.getMessage() + ")");
    }
  }

  private void evalLane() throws IOException {
    log.line("EVAL LANE");
    Path moduleResults = root.resolve("debuggers/eval-debugger/build/test-results/test");
    for (Path haxeDir : haxeDirs) {
      String haxe = haxeDir.getFileName().toString();
      Map<String, String> env = haxeEnv(haxeDir);
      verifyLaneHaxe(haxe, env);
      log.line("    " + haxe + " : running the eval suite");
      long start = System.nanoTime();
      SuiteRun run = runSuite(":debuggers:eval-debugger",
                              List.of(GRADLE_NO_BUILD_CACHE, GRADLE_CONTINUE),
                              env, out.resolve("logs/eval-" + haxe + ".log"), 900,
                              moduleResults, out.resolve("results/eval_" + haxe));
      addCell("eval", haxe, null, run.status().name().toLowerCase(Locale.ROOT),
              run.classes(), run.flaky(), start);
    }
  }

  private void hxcppLane() throws IOException {
    log.line("HXCPP LANE");
    Map<String, String> fixtures = Map.of(
      "buildHxcppFixtureFixture", "hxcpp/fixture/" + Platform.exe("Main-debug"),
      "buildHxcppFixtureExFixture", "hxcpp/fixture-ex/" + Platform.exe("MainEx-debug"));
    Path moduleBuild = root.resolve("debuggers/intellij-hxcpp-debugger/build");
    for (Path haxeDir : haxeDirs) {
      String haxe = haxeDir.getFileName().toString();
      long start = System.nanoTime();
      deleteQuietly(moduleBuild.resolve("hxcpp"));
      verifyLaneHaxe(haxe, haxeEnv(haxeDir));
      log.line("    " + haxe + " : building the C++ fixtures (this is the slow part)");
      List<String> build = new ArrayList<>();
      fixtures.keySet().stream().sorted().forEach(t -> build.add(":debuggers:intellij-hxcpp-debugger:" + t));
      build.addAll(EXCLUDE_HAXELIB);
      build.add(GRADLE_CONTINUE);
      gradle.run(build, haxeEnv(haxeDir), out.resolve("logs/hxcpp-" + haxe + "-build.log"), 1200);
      List<String> missing = fixtures.entrySet().stream()
        .filter(e -> !Files.isRegularFile(moduleBuild.resolve(e.getValue())))
        .map(Map.Entry::getKey).toList();
      if (missing.size() == fixtures.size()) {
        diagnoseCompileFail(out.resolve("logs/hxcpp-" + haxe + "-build.log"));
        addCell("hxcpp", haxe, null, "compile-fail", List.of(), List.of(), start);
        continue;
      }
      log.line("    " + haxe + " : fixtures built"
               + (missing.isEmpty() ? "" : " (missing " + missing.size() + ")") + ", running the suite");
      List<String> extra = new ArrayList<>(List.of(GRADLE_NO_BUILD_CACHE));
      extra.addAll(EXCLUDE_HAXELIB);
      missing.forEach(t -> extra.addAll(List.of(GRADLE_EXCLUDE, t)));
      extra.add(GRADLE_CONTINUE);
      SuiteRun run = runSuite(":debuggers:intellij-hxcpp-debugger", extra, haxeEnv(haxeDir),
                              out.resolve("logs/hxcpp-" + haxe + "-test.log"), 1500,
                              moduleBuild.resolve("test-results/test"), out.resolve("results/hxcpp_" + haxe));
      addCell("hxcpp", haxe, null, run.status().name().toLowerCase(Locale.ROOT),
              run.classes(), run.flaky(), start);
    }
  }

  /**
   * Reads a failed fixture build's error output for KNOWN toolchain problems
   * and logs an actionable hint — "compile-fail" alone sent the first user
   * hunting through log files for what was an environment issue.
   */
  private void diagnoseCompileFail(Path buildLog) {
    try {
      Path err = Path.of(buildLog + ".err");
      String text = (Files.isRegularFile(err) ? Files.readString(err) : "")
                    + (Files.isRegularFile(buildLog) ? Files.readString(buildLog) : "");
      if (text.contains("cl.exe")) {
        log.line("      HINT: MSVC (cl.exe) was not usable in this build's environment.");
        log.line("      The gradle daemon keeps the environment it was STARTED with - if it was");
        log.line("      started from an IDE or a stale shell, hxcpp cannot find Visual Studio.");
        log.line("      Run `gradlew --stop`, then rerun from a shell where a plain hxcpp build works.");
      }
    } catch (IOException ignored) {
    }
  }

  private void hashlinkLane() throws IOException {
    Path moduleBuild = root.resolve("debuggers/hashlink-debug-adapter/build");
    Path adapter = moduleBuild.resolve("hl/hl-debug-adapter.hl");
    if (!Files.isRegularFile(adapter)) {
      // the shipped adapter is a DEV-haxe artifact; build it once and PIN it
      gradle.run(List.of(":debuggers:hashlink-debug-adapter:" + TASK_BUILD_ADAPTER),
                 Map.of(), out.resolve("logs/hl-adapter.log"), 600);
    }
    long pinned = lastModified(adapter);
    log.line("HL LANE (adapter pinned; " + (full ? "FULL grid" : "smart-reduced") + ")");
    for (Path haxeDir : haxeDirs) {
      String haxe = haxeDir.getFileName().toString();
      HL_FIXTURE_FILES.values().forEach(f -> deleteQuietly(moduleBuild.resolve("hl/" + f)));
      long start = System.nanoTime();
      verifyLaneHaxe(haxe, haxeEnv(haxeDir));
      log.line("    " + haxe + " : building the HL fixtures");
      List<String> build = new ArrayList<>();
      HL_FIXTURE_TASKS.forEach(t -> build.add(":debuggers:hashlink-debug-adapter:" + t));
      build.addAll(EXCLUDE_HAXELIB);
      build.add(GRADLE_CONTINUE);
      gradle.run(build, haxeEnv(haxeDir), out.resolve("logs/hl-" + haxe + "-build.log"), 600);
      List<String> missing = HL_FIXTURE_TASKS.stream()
        .filter(t -> !Files.isRegularFile(moduleBuild.resolve("hl/" + HL_FIXTURE_FILES.get(t))))
        .toList();
      if (missing.contains("buildTestFixture")) {
        diagnoseCompileFail(out.resolve("logs/hl-" + haxe + "-build.log"));
        for (Path hlDir : hlDirs) {
          addCell("hashlink", haxe, hlDir.getFileName().toString(), "compile-fail", List.of(), List.of(), start);
        }
        continue;
      }
      // an explicit --hl selection overrides the smart-reduced grid
      List<Path> runtimes = (full || !hlFilter.isEmpty() || !VersionManifest.DEGRADED_HAXE_ON_HL.contains(haxe))
        ? hlDirs
        : hlDirs.stream().filter(d -> VersionManifest.REFERENCE_RUNTIMES.contains(d.getFileName().toString())).toList();
      for (Path hlDir : runtimes) {
        String runtime = hlDir.getFileName().toString();
        Path hlBinary = Platform.findBinary(hlDir, "hl");
        log.line("    " + haxe + " x " + runtime + " : running the HL suite");
        long cellStart = System.nanoTime();
        List<String> extra = new ArrayList<>(List.of(
          PROP_HASHLINK_BIN + hlBinary, GRADLE_NO_BUILD_CACHE, GRADLE_EXCLUDE, TASK_BUILD_ADAPTER));
        // the matrix's fork count is authoritative for the cell - always passed,
        // so -PmatrixHlForks=1 runs sequentially even though the module task's
        // own default is higher. (Fixture builds are excluded tasks here, so a
        // parallel fork can never overlap a running test.)
        extra.add(PROP_DAP_TEST_FORKS + hlForks);
        HL_FIXTURE_TASKS.forEach(t -> extra.addAll(List.of(GRADLE_EXCLUDE, t)));
        extra.addAll(EXCLUDE_HAXELIB);
        extra.add(GRADLE_CONTINUE);
        Map<String, String> env = haxeEnv(haxeDir);
        if (!Platform.WINDOWS) {
          // linux: hl finds libhl.so and the std .hdll libraries beside itself
          String previous = System.getenv("LD_LIBRARY_PATH");
          env.put("LD_LIBRARY_PATH", hlBinary.getParent() + (previous != null ? ":" + previous : ""));
        }
        SuiteRun run = runSuite(":debuggers:hashlink-debug-adapter", extra, env,
                                out.resolve("logs/hl-" + haxe + "-" + runtime + ".log"), 1500,
                                moduleBuild.resolve("test-results/test"),
                                out.resolve("results/hl_" + haxe + "__" + runtime));
        addCell("hashlink", haxe, runtime, run.status().name().toLowerCase(Locale.ROOT),
                run.classes(), run.flaky(), cellStart);
      }
      if (lastModified(adapter) != pinned) {
        log.line("  WARNING: pinned adapter was rebuilt during " + haxe);
      }
    }
  }

  /** Dev haxe back in charge: rebuild the HL fixtures the lanes overwrote. */
  private void restore() {
    if (!lanes.contains("hashlink")) {
      return;
    }
    Path moduleBuild = root.resolve("debuggers/hashlink-debug-adapter/build");
    HL_FIXTURE_FILES.values().forEach(f -> deleteQuietly(moduleBuild.resolve("hl/" + f)));
    List<String> build = new ArrayList<>();
    HL_FIXTURE_TASKS.forEach(t -> build.add(":debuggers:hashlink-debug-adapter:" + t));
    build.addAll(EXCLUDE_HAXELIB);
    build.add(GRADLE_CONTINUE);
    gradle.run(build, Map.of(), out.resolve("logs/restore-hl-fixtures.log"), 600);
  }

  // ----------------------------------------------------------------- report

  private void reportOnly() throws IOException {
    Path results = out.resolve("results");
    if (Files.isDirectory(results)) {
      try (var dirs = Files.list(results)) {
        for (Path dir : dirs.filter(Files::isDirectory).sorted().toList()) {
          String name = dir.getFileName().toString();
          String lane;
          String haxe;
          String runtime = null;
          if (name.startsWith("hl_")) {
            lane = "hashlink";
            String rest = name.substring(3);
            int split = rest.indexOf("__");
            haxe = split >= 0 ? rest.substring(0, split) : rest;
            runtime = split >= 0 ? rest.substring(split + 2) : null;
          } else {
            int split = name.indexOf('_');
            lane = name.substring(0, split);
            haxe = name.substring(split + 1);
          }
          cells.add(new Results.Cell(lane, haxe, runtime, "ok", Results.parse(dir), List.of(), 0));
        }
      }
    }
    haxeDirs = List.of();
    hlDirs = List.of();
    report();
  }

  private void report() throws IOException {
    List<String> haxeNames = !haxeDirs.isEmpty() ? names(haxeDirs)
      : cells.stream().map(Results.Cell::haxe).distinct().sorted().toList();
    List<String> hlNames = !hlDirs.isEmpty() ? names(hlDirs)
      : cells.stream().map(Results.Cell::runtime).filter(Objects::nonNull).distinct().sorted().toList();
    // one timestamped report per run (so results stay traceable to WHEN they
    // ran) plus index.html always mirroring the newest run
    Path stamped = out.resolve("matrix-" + startedAt + ".html");
    Report.write(root.resolve("debuggers/compat-matrix/report-template.html"),
                 stamped, cells, haxeNames, hlNames, resources, full);
    Files.copy(stamped, out.resolve("index.html"),
               StandardCopyOption.REPLACE_EXISTING);
    log.line("report: " + stamped + " (also copied to index.html)");
  }

  // ---------------------------------------------------------------- helpers

  // synchronized: lane threads report cells concurrently in parallel mode
  private synchronized void addCell(String lane, String haxe, String runtime, String status,
                                    List<Results.ClassResult> classes, List<String> flaky, long startNanos) {
    long seconds = (System.nanoTime() - startNanos) / 1_000_000_000L;
    Results.Cell cell = new Results.Cell(lane, haxe, runtime, status, classes, flaky, seconds);
    cells.add(cell);
    log.line(String.format("  %s %s%s : %s classes=%d failures=%d%s (%ds)",
                           lane, haxe, runtime != null ? " x " + runtime : "", status,
                           classes.size(), cell.totalFailures(),
                           flaky.isEmpty() ? "" : " flaky=" + flaky.size(), seconds));
  }

  private static List<String> names(List<Path> dirs) {
    return dirs.stream().map(d -> d.getFileName().toString()).toList();
  }

  private static long lastModified(Path path) {
    try {
      return Files.getLastModifiedTime(path).toMillis();
    } catch (IOException e) {
      return -1;
    }
  }

  private static void deleteQuietly(Path path) {
    try {
      if (Files.isDirectory(path)) {
        try (var walk = Files.walk(path)) {
          for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
            Files.deleteIfExists(p);
          }
        }
      } else {
        Files.deleteIfExists(path);
      }
    } catch (IOException ignored) {
    }
  }
}
