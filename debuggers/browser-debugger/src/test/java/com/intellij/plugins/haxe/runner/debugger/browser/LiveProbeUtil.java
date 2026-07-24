package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.util.net.NetUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * The plumbing shared by the live probes: haxe availability, fixture
 * compilation, adapter connection with retry, and process-tree teardown.
 */
final class LiveProbeUtil {
  private LiveProbeUtil() {
  }

  static boolean haxeOnPath() {
    try {
      Process probe = new ProcessBuilder("haxe", "--version").redirectErrorStream(true).start();
      return probe.waitFor(10, TimeUnit.SECONDS) && probe.exitValue() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Compiles one {@code haxe -js} unit with {@code -debug} (source maps);
   * fails the test with the compiler's output when the compile fails.
   */
  static void compileHaxeJs(Path classPath, String mainClass, String outJsName) throws Exception {
    Process haxe = new ProcessBuilder("haxe", "-cp", classPath.toString(), "-main", mainClass,
                                      "-js", classPath.resolve(outJsName).toString(), "-debug")
      .redirectErrorStream(true)
      .start();
    String output = new String(haxe.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (!haxe.waitFor(30, TimeUnit.SECONDS) || haxe.exitValue() != 0) {
      throw new AssertionError("fixture compile of " + mainClass + " failed:\n" + output);
    }
  }

  /**
   * A free TCP port from the OS, for the adapter's DAP listener and the RDP
   * port in launch configs. A random pick from a fixed range flakes on
   * Windows: Hyper-V/WinNAT reserve blocks of the port space (excluded port
   * ranges) and a bind inside one dies with EACCES.
   */
  static int freePort() throws IOException {
    return NetUtils.findAvailableSocketPort();
  }

  /** Connects to an adapter's DAP port, retrying briefly (see DapClient.connectWithRetry). */
  static DapClient connectWithRetry(int port, int connectTimeoutMillis) throws IOException {
    return DapClient.connectWithRetry("127.0.0.1", port, connectTimeoutMillis, 10_000);
  }

  /**
   * Kills the WHOLE process tree: killing node does not kill the browser it
   * spawned, and every leaked headless browser poisons later
   * launches.
   */
  static void killTree(Process process) throws InterruptedException {
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroy();
    if (!process.waitFor(3, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      process.waitFor(3, TimeUnit.SECONDS);
    }
  }
}
