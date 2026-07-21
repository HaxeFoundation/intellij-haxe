package com.intellij.plugins.haxe.runner.debugger.eval;

import com.intellij.plugins.haxe.runner.debugger.dap.DapPaths;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Drives the REAL eval VM: launches `haxe --interp` with -D eval-debugger
 * pointed at our listener and exercises the protocol end to end — the VM
 * connects, waits before main, hits a line breakpoint, reports the stack,
 * and runs to a clean exit. Skips when haxe is not on PATH.
 *
 * The line constant mirrors the marked line in test-fixtures/EvalMain.hx.
 */
public class EvalLiveTest {
  private static final int BREAK_LINE = 10;
  private static final long TIMEOUT_MS = 15_000;

  private ServerSocket listener;
  private Process haxe;
  private Socket vm;
  private EvalConnection connection;
  private Thread outputGobbler;
  private final StringBuilder haxeOutput = new StringBuilder();

  private static boolean haxeOnPath() {
    try {
      Process probe = new ProcessBuilder("haxe", "--version").redirectErrorStream(true).start();
      return probe.waitFor(10, TimeUnit.SECONDS) && probe.exitValue() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  private static Path fixtureDir() {
    String fromGradle = System.getProperty("eval.fixture.src.dir");
    return fromGradle != null ? Path.of(fromGradle) : Path.of("test-fixtures").toAbsolutePath();
  }

  @Before
  public void launch() throws IOException {
    Assume.assumeTrue("haxe not on PATH - skipping live eval test", haxeOnPath());
    Path fixtures = fixtureDir();
    Assume.assumeTrue("eval fixture missing - skipping", Files.isRegularFile(fixtures.resolve("EvalMain.hx")));

    listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    listener.setSoTimeout((int)TIMEOUT_MS);
    haxe = new ProcessBuilder("haxe", "-cp", fixtures.toString(), "-main", "EvalMain",
                              "-D", "eval-debugger=127.0.0.1:" + listener.getLocalPort(),
                              "--interp")
      .redirectErrorStream(true)
      .start();
    outputGobbler = new Thread(() -> {
      try (BufferedReader reader = new BufferedReader(
             new InputStreamReader(haxe.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          synchronized (haxeOutput) {
            haxeOutput.append(line).append('\n');
          }
        }
      } catch (IOException ignored) {
      }
    }, "haxe-output-gobbler");
    outputGobbler.setDaemon(true);
    outputGobbler.start();

    vm = listener.accept();
    connection = new EvalConnection(vm.getInputStream(), vm.getOutputStream());
  }

  @After
  public void tearDown() throws Exception {
    if (connection != null) {
      connection.close();
    }
    if (haxe != null) {
      if (!haxe.waitFor(3, TimeUnit.SECONDS)) {
        haxe.descendants().forEach(ProcessHandle::destroyForcibly);
        haxe.destroyForcibly();
        haxe.waitFor(5, TimeUnit.SECONDS);
      }
    }
    if (listener != null) {
      listener.close();
    }
  }

  @Test
  public void breakpointStopStackTraceAndCleanExit() throws Exception {
    CountDownLatch stopped = new CountDownLatch(1);
    int[] stoppedThread = {-1};
    connection.setEventListener((method, params) -> {
      if (EvalProtocol.EVENT_BREAKPOINT_STOP.equals(method)) {
        stoppedThread[0] = params.path("threadId").asInt(-1);
        stopped.countDown();
      }
    });
    connection.start();
    EvalProtocol protocol = new EvalProtocol(connection);

    // the VM is waiting before main: it must answer while suspended
    List<EvalProtocol.EvalThread> threads = protocol.getThreads();
    assertFalse("VM reports at least one thread", threads.isEmpty());

    String fixture = fixtureDir().resolve("EvalMain.hx").toString();
    List<EvalProtocol.EvalBreakpoint> ids = protocol.setBreakpoints(fixture, BREAK_LINE);
    assertEquals("one breakpoint registered", 1, ids.size());
    assertTrue("VM assigned a breakpoint id", ids.get(0).id() >= 0);

    protocol.resume();
    assertTrue("hit the breakpoint within " + TIMEOUT_MS + "ms",
               stopped.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

    List<EvalProtocol.EvalStackFrame> frames = protocol.stackTrace(stoppedThread[0]);
    assertFalse("stack has frames at the stop", frames.isEmpty());
    EvalProtocol.EvalStackFrame top = frames.get(0);
    assertEquals("stopped on the breakpoint line", BREAK_LINE, top.line());
    assertTrue("top frame is in the fixture (was " + top.source() + ")",
               top.source() != null && DapPaths.toForwardSlashes(top.source()).endsWith("EvalMain.hx"));

    // scopes/variables at the stop: the local declared BEFORE the break line
    // must be visible with its value
    List<EvalProtocol.EvalScope> scopes = protocol.getScopes(top.id());
    assertFalse("stop exposes scopes", scopes.isEmpty());
    boolean sawGreeting = false;
    for (EvalProtocol.EvalScope scope : scopes) {
      for (EvalProtocol.EvalVar var : protocol.getVariables(scope.id())) {
        if ("greeting".equals(var.name())) {
          sawGreeting = true;
          assertTrue("greeting holds its value (was " + var.value() + ")",
                     var.value().contains("hello"));
        }
      }
    }
    assertTrue("local 'greeting' visible in some scope", sawGreeting);

    protocol.resume();
    assertTrue("debuggee ran to completion", haxe.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    assertEquals("clean exit", 0, haxe.exitValue());
    String output;
    synchronized (haxeOutput) {
      output = haxeOutput.toString();
    }
    assertTrue("fixture output arrived (was: " + output + ")", output.contains("eval-fixture:hello:7"));
  }
}
