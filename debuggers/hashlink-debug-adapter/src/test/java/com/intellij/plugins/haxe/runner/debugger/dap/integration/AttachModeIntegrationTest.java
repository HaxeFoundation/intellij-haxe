package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Attach mode: the CLIENT spawns {@code hl --debug <port> --debug-wait} and the
 * adapter attaches by pid (launch args {@code attachPid}/{@code debugPort}).
 * This is the path the IDE ships — spawning from the adapter (an HL process)
 * would force SW_HIDE onto a GUI debuggee's first window on Windows.
 *
 * Key attach-mode contracts verified here:
 * - the debuggee's stdout stays on the SPAWNER's pipe (not DAP output events);
 * - breakpoints/stepping work exactly as in launch mode;
 * - disconnect detaches (restoring all patched INT3 bytes) instead of killing,
 *   so the debuggee runs to completion on its own.
 */
@DisplayName("HashLink debugger: attach mode (integration)")
public class AttachModeIntegrationTest extends DapIntegrationTestBase {
  private final StringBuilder debuggeeOutput = new StringBuilder();

  private Process debuggee;
  private Thread debuggeeGobbler;

  @AfterEach
  public void stopDebuggee() {
    if (debuggee != null && debuggee.isAlive()) {
      debuggee.destroyForcibly();
    }
  }

  @Test
  @DisplayName("attaches to an externally spawned debuggee and debugs it")
  public void attachesToAnExternallySpawnedDebuggeeAndDebugsIt() throws Exception {
    int port = spawnDebuggee();
    initialize();
    assertTrue(request(attachLaunch(port)).isSuccess(), "attach-mode launch succeeds");
    assertTrue(setBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE).isSuccess(), "setBreakpoints succeeds");
    configurationDone();

    // the loop body runs three times; variables must be readable at a stop
    int stops = 0;
    boolean terminated = false;

    while (!terminated) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull(event, "expected a debug event");
      if (event instanceof StoppedEvent stopped) {
        stops++;
        int threadId = stopped.getBody().getThreadId();
        if (stops == 1) {
          Map<String, String> locals = localsInTopFrame(threadId);
          assertEquals("3", locals.get("count"), "locals readable in attach mode");
        }
        assertTrue(request(continueRequest(threadId)).isSuccess(), "continue succeeds");
      }
      else if (event instanceof TerminatedEvent) {
        terminated = true;
      }
    }
    assertEquals(3, stops, "breakpoint hit once per loop iteration");

    assertTrue(debuggee.waitFor(TIMEOUT, TimeUnit.MILLISECONDS), "debuggee exits");
    assertEquals(0, debuggee.exitValue(), "clean exit");
    // stdio ownership: the fixture's output arrived on OUR pipe, not as DAP events
    assertTrue(drainedDebuggeeOutput().contains("fixture-total:3"), "debuggee stdout stays with the spawner (got: " + drainedDebuggeeOutput() + ")");
    assertTrue(request(new DisconnectRequest()).isSuccess(), "disconnect succeeds");
  }

  @Test
  @DisplayName("disconnect detaches and lets the debuggee finish")
  public void disconnectDetachesAndLetsTheDebuggeeFinish() throws Exception {
    int port = spawnDebuggee();
    initialize();
    assertTrue(request(attachLaunch(port)).isSuccess(), "attach-mode launch succeeds");
    assertTrue(setBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE).isSuccess(), "setBreakpoints succeeds");
    configurationDone();
    awaitStopped();

    // disconnect while stopped at a breakpoint: the adapter must restore every
    // patched INT3 and detach — a leftover trap would crash the free-running
    // debuggee (non-zero exit) and a missed resume would hang it (timeout)
    assertTrue(request(new DisconnectRequest()).isSuccess(), "disconnect succeeds");
    assertTrue(debuggee.waitFor(TIMEOUT, TimeUnit.MILLISECONDS), "debuggee finishes on its own after detach");
    assertEquals(0, debuggee.exitValue(), "clean exit after detach");
    assertTrue(drainedDebuggeeOutput().contains("fixture-total:3"), "debuggee completed its work (got: " + drainedDebuggeeOutput() + ")");
  }

  // --- helpers ---

  /** Spawns the fixture under the VM debug server and returns the debug port. */
  private int spawnDebuggee() throws IOException {
    int port = freeDebugPort();
    debuggee = new ProcessBuilder(hlExecutable, "--debug", Integer.toString(port), "--debug-wait", fixtureHl.toString())
      .redirectErrorStream(true)
      .start();

    BufferedReader stdout = new BufferedReader(
      new InputStreamReader(debuggee.getInputStream(), StandardCharsets.UTF_8));
    debuggeeGobbler = new Thread(() -> gobbleDebuggee(stdout), "debuggee-output-gobbler");
    debuggeeGobbler.setDaemon(true);
    debuggeeGobbler.start();

    return port;
  }

  private LaunchRequest attachLaunch(int port) {
    LaunchRequest request = new LaunchRequest();
    LaunchRequestArguments args = new LaunchRequestArguments();
    args.setProgram(fixtureHl.toString());
    args.setAttachPid((int)debuggee.pid());
    args.setDebugPort(port);
    request.setArguments(args);
    return request;
  }

  private static int freeDebugPort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  private void gobbleDebuggee(BufferedReader stdout) {
    try {
      String line;
      while ((line = stdout.readLine()) != null) {
        synchronized (debuggeeOutput) {
          debuggeeOutput.append(line).append('\n');
        }
      }
    } catch (IOException ignored) {
      // process ended
    }
  }

  private String drainedDebuggeeOutput() throws InterruptedException {
    if (debuggeeGobbler != null) {
      debuggeeGobbler.join(2000);
    }
    synchronized (debuggeeOutput) {
      return debuggeeOutput.toString();
    }
  }
}
