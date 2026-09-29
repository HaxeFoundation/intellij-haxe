package com.intellij.plugins.haxe.runner.debugger.eval;

import com.intellij.plugins.haxe.runner.debugger.eval.EvalProtocol.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Drives the REAL eval VM over the RAW protocol (no adapter): launches
 * `haxe --interp` with -D eval-debugger pointed at our listener and pins the
 * wire facts the adapter builds on — the VM answers while suspended before
 * main, assigns breakpoint ids, and may close the connection before acking a
 * resume the program runs off (the resume race). The full stop inspection
 * (stack, scopes, variables) is the DAP layer's pin in
 * EvalDebugAdapterLiveTest. Skips when haxe is not on PATH.
 *
 * The line constant mirrors the marked line in test-fixtures/EvalMain.hx.
 */
@DisplayName("Eval debugger: eval (live)")
public class EvalLiveTest {
  private static final int BREAK_LINE = 10;
  private static final long TIMEOUT_MS = 15_000;
  private final StringBuilder haxeOutput = new StringBuilder();

  private ServerSocket listener;
  private Process haxe;
  private Socket vm;
  private EvalConnection connection;
  private Thread outputGobbler;

  @BeforeEach
  public void launch() throws IOException {
    Assumptions.assumeTrue(EvalLiveTestBase.haxeOnPath(), "haxe not on PATH - skipping live eval test");
    Path fixtures = EvalLiveTestBase.fixtureDir();
    Assumptions.assumeTrue(Files.isRegularFile(fixtures.resolve("EvalMain.hx")), "eval fixture missing - skipping");

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

  @AfterEach
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
  @DisplayName("answers while suspended assigns breakpoint ids and tolerates the resume race")
  public void answersWhileSuspendedAssignsBreakpointIdsAndToleratesTheResumeRace() throws Exception {
    CountDownLatch stopped = new CountDownLatch(1);
    connection.setEventListener((method, params) -> {
      if (EvalProtocol.EVENT_BREAKPOINT_STOP.equals(method)) {
        stopped.countDown();
      }
    });
    connection.start();
    EvalProtocol protocol = new EvalProtocol(connection);

    // the VM is waiting before main: it must answer while suspended
    List<EvalThread> threads = protocol.getThreads();
    assertFalse(threads.isEmpty(), "VM reports at least one thread");

    String fixture = EvalLiveTestBase.fixtureDir().resolve("EvalMain.hx").toString();
    List<EvalBreakpoint> ids = protocol.setBreakpoints(fixture, BREAK_LINE);
    assertEquals(1, ids.size(), "one breakpoint registered");
    assertTrue(ids.get(0).id() >= 0, "VM assigned a breakpoint id");

    protocol.resume();
    assertTrue(stopped.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "hit the breakpoint within " + TIMEOUT_MS + "ms");

    try {
      protocol.resume();
    } catch (EvalConnectionClosedException programEnded) {
      // The VM acks continue from a helper thread while the resumed program
      // runs, so a fixture this small can exit before the ack is flushed; the
      // clean-exit assertions below still verify the resume took effect.
      // The adapter tolerates the same race in resumeToleratingExit.
    }
    assertTrue(haxe.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS), "debuggee ran to completion");
    assertEquals(0, haxe.exitValue(), "clean exit");
    String output;
    synchronized (haxeOutput) {
      output = haxeOutput.toString();
    }
    assertTrue(output.contains("eval-fixture:hello:7"), "fixture output arrived (was: " + output + ")");
  }
}
