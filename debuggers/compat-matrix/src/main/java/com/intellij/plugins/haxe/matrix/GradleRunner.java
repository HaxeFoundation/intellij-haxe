package com.intellij.plugins.haxe.matrix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Runs a child gradle build with a lane's environment. Always {@code
 * --no-daemon}: the forked test JVM must inherit THIS invocation's
 * PATH/HAXE_STD_PATH, and a warm daemon keeps the environment it was born
 * with (it would silently test the wrong haxe). Bounded; on
 * timeout the whole process TREE dies, plus known stray debuggees — a stuck
 * runtime error dialog must not wedge the matrix.
 */
final class GradleRunner {
  private static final List<String> STRAY_BASENAMES =
    List.of("hl", "hl.exe", "haxe", "haxe.exe", "Main-debug", "Main-debug.exe", "MainEx-debug", "MainEx-debug.exe");

  private final Path root;
  private final Log log;

  GradleRunner(Path root, Log log) {
    this.root = root;
    this.log = log;
  }

  enum Status { OK, FAIL, TIMEOUT }

  Status run(List<String> tasks, Map<String, String> extraEnv, Path logFile, int timeoutSec) {
    return run(tasks, extraEnv, logFile, timeoutSec, false);
  }

  /**
   * With {@code liveTestProgress}, the child build gets the tool's init
   * script (per-test events on stdout — gradle only writes junit XMLs at
   * task END, so those cannot drive live progress) and the log file is
   * tailed while the build runs: each finished suite is logged with its
   * counts, each failing test immediately.
   */
  Status run(List<String> tasks, Map<String, String> extraEnv, Path logFile, int timeoutSec, boolean liveTestProgress) {
    List<String> command = new ArrayList<>();
    command.add(root.resolve(Platform.gradlew()).toString());
    command.addAll(tasks);
    if (liveTestProgress) {
      command.add("-I");
      command.add(root.resolve("debuggers/compat-matrix/test-events.init.gradle").toString());
    }
    // debugger tests and fixture builds are OPT-IN repo-wide; the matrix IS
    // the debugger-test runner, so every child build gets the flag
    command.add("-PdebuggerTests=true");
    command.add("--no-daemon");
    command.add("--console=plain");
    ProcessBuilder builder = new ProcessBuilder(command)
      .directory(root.toFile())
      .redirectOutput(logFile.toFile())
      .redirectError(new File(logFile + ".err"));
    applyEnv(builder.environment(), extraEnv);
    // the child gradlew needs a JVM the SHELL environment may not have (an
    // IDE-launched parent runs on a gradle-provisioned JDK invisible to the
    // shell — on a bare linux VM there is no `java` on PATH at all); this
    // process's own JVM is by construction a working one
    builder.environment().putIfAbsent("JAVA_HOME", System.getProperty("java.home"));
    try {
      // the redirect targets must exist or CreateProcess fails with a
      // misleading "cannot find the path specified" (fresh checkouts have
      // no logs/ directory yet; older runs left one behind, masking this)
      Files.createDirectories(logFile.toAbsolutePath().getParent());
      Process process = builder.start();
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSec);
      TestEventTail tail = liveTestProgress ? new TestEventTail(logFile, log) : null;
      while (!process.waitFor(2, TimeUnit.SECONDS)) {
        if (tail != null) {
          tail.poll();
        }
        if (System.nanoTime() > deadline) {
          killTree(process.toHandle());
          killStrays();
          return Status.TIMEOUT;
        }
      }
      if (tail != null) {
        tail.poll();
        tail.flush();
      }
      return process.exitValue() == 0 ? Status.OK : Status.FAIL;
    } catch (IOException e) {
      log.line("gradle launch failed: " + e.getMessage());
      return Status.FAIL;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Status.FAIL;
    }
  }

  /**
   * Applies the lane's env overrides, first REMOVING any case-variant of the
   * overridden keys ("Path" vs "PATH"): Windows environments are
   * case-insensitive but java maps are not everywhere in the chain, and a
   * surviving variant with the dev toolchain first produced a lane compiling
   * with the WRONG haxe on one machine (std-typing error salad).
   */
  static void applyEnv(Map<String, String> environment, Map<String, String> overrides) {
    for (String key : overrides.keySet()) {
      environment.keySet().removeIf(existing -> existing.equalsIgnoreCase(key));
    }
    environment.putAll(overrides);
  }

  private static void killTree(ProcessHandle handle) {
    handle.descendants().forEach(ProcessHandle::destroyForcibly);
    handle.destroyForcibly();
  }

  /**
   * In parallel-lane mode the per-suite stray sweep must be deferred: it
   * kills by executable NAME system-wide, so one lane's sweep would murder
   * another lane's live compiler/VM. The matrix sets this before spawning
   * lane threads and calls {@link #killStraysNow()} once after they join.
   */
  static volatile boolean deferStrayKills = false;

  /** Kills leftover debuggee/toolchain processes by exact executable name. */
  static void killStrays() {
    if (deferStrayKills) {
      return;
    }
    killStraysNow();
  }

  static void killStraysNow() {
    ProcessHandle.allProcesses().forEach(handle -> {
      String cmd = handle.info().command().orElse("");
      if (cmd.isEmpty()) {
        return;
      }
      String base = cmd.substring(Math.max(cmd.lastIndexOf('/'), cmd.lastIndexOf('\\')) + 1);
      if (STRAY_BASENAMES.contains(base)) {
        handle.destroyForcibly();
      }
    });
  }
}
