package com.intellij.plugins.haxe.runner.debugger.browser;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.client.DapEndpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.InitializedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.util.net.NetUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The plumbing shared by the live probes: haxe availability, fixture
 * compilation, adapter connection with retry, and process-tree teardown.
 */
final class LiveProbeUtil {
  /** The one-page host for the compiled fixture; every probe writes the same file. */
  static final String INDEX_HTML = """
    <!DOCTYPE html><html><head><meta charset='utf-8'></head>\
    <body><script src='app.js'></script></body></html>""";

  private LiveProbeUtil() {
  }

  /** Probe-side diagnostics; the tag separates them from the [adapter] and [server] streams. */
  static void probe(String message) {
    System.out.println("[probe] " + message);
  }

  /**
   * Asserts the stop landed on the haxe original at {@code line} — a frame
   * pointing at the generated app.js means the source map was not applied.
   */
  static void assertStoppedInHx(StackFrame top, String hxFile, int line) {
    assertNotNull("top frame has no source", top.getSource());
    assertTrue("top frame is not the .hx original: " + top.getSource().getPath(),
               top.getSource().getPath() != null && top.getSource().getPath().endsWith(hxFile));
    assertTrue("wrong line: " + top.getLine(), top.getLine() == line);
  }

  static StackTraceRequest stackTraceRequest(int threadId) {
    StackTraceRequest request = new StackTraceRequest();
    StackTraceArguments arguments = new StackTraceArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return request;
  }

  static ScopesRequest scopesRequest(int frameId) {
    ScopesRequest request = new ScopesRequest();
    ScopesArguments arguments = new ScopesArguments();
    arguments.setFrameId(frameId);
    request.setArguments(arguments);
    return request;
  }

  static VariablesRequest variablesRequest(int variablesReference) {
    VariablesRequest request = new VariablesRequest();
    VariablesArguments arguments = new VariablesArguments();
    arguments.setVariablesReference(variablesReference);
    request.setArguments(arguments);
    return request;
  }

  static ContinueRequest continueRequest(int threadId) {
    ContinueRequest request = new ContinueRequest();
    ContinueArguments arguments = new ContinueArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return request;
  }

  static PauseRequest pauseRequest(int threadId) {
    PauseRequest request = new PauseRequest();
    PauseArguments arguments = new PauseArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return request;
  }

  static NextRequest nextRequest(int threadId) {
    NextRequest request = new NextRequest();
    NextArguments arguments = new NextArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return request;
  }

  static StepInTargetsRequest stepInTargetsRequest(int frameId) {
    StepInTargetsRequest request = new StepInTargetsRequest();
    StepInTargetsArguments arguments = new StepInTargetsArguments();
    arguments.setFrameId(frameId);
    request.setArguments(arguments);
    return request;
  }

  /** Smart step into: enter the chosen call on the line rather than the first one. */
  static StepInRequest stepInRequest(int threadId, int targetId) {
    StepInRequest request = new StepInRequest();
    StepInArguments arguments = new StepInArguments();
    arguments.setThreadId(threadId);
    arguments.setTargetId(targetId);
    request.setArguments(arguments);
    return request;
  }

  static SetExceptionBreakpointsRequest exceptionBreakpointsRequest(List<String> filters) {
    SetExceptionBreakpointsRequest request = new SetExceptionBreakpointsRequest();
    SetExceptionBreakpointsArguments arguments = new SetExceptionBreakpointsArguments();
    arguments.setFilters(filters);
    request.setArguments(arguments);
    return request;
  }

  /** True when the adapter's initialized event arrives before the timeout. */
  static boolean awaitInitialized(DapEndpoint endpoint, long millis) throws Exception {
    long deadline = System.currentTimeMillis() + millis;
    while (System.currentTimeMillis() < deadline) {
      if (endpoint.pollEvent(250) instanceof InitializedEvent) {
        return true;
      }
    }
    return false;
  }

  /** The next stopped event, or null when none arrives before the timeout. */
  static StoppedEvent awaitStopped(DapEndpoint endpoint, long millis) throws Exception {
    long deadline = System.currentTimeMillis() + millis;
    while (System.currentTimeMillis() < deadline) {
      if (endpoint.pollEvent(250) instanceof StoppedEvent stopped) {
        return stopped;
      }
    }
    return null;
  }

  static boolean haxeOnPath() {
    try {
      Process probe = new ProcessBuilder("haxe", "--version").redirectErrorStream(true).start();
      return probe.waitFor(10, TimeUnit.SECONDS) && probe.exitValue() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  /** The host page plus its compiled app.js — the two files every probe fixture needs. */
  static void writePageAndCompile(Path dir, String mainClass) throws Exception {
    Files.writeString(dir.resolve("index.html"), INDEX_HTML);
    compileHaxeJs(dir, mainClass, "app.js");
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
