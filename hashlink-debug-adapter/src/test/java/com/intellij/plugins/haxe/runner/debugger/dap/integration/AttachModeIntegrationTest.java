package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.TerminatedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.DisconnectRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.LaunchRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.LaunchRequestArguments;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;

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
public class AttachModeIntegrationTest extends DapIntegrationTestBase {
  private Process debuggee;
  private final StringBuilder debuggeeOutput = new StringBuilder();
  private Thread debuggeeGobbler;

  @After
  public void stopDebuggee() {
    if (debuggee != null && debuggee.isAlive()) {
      debuggee.destroyForcibly();
    }
  }

  @Test
  public void attachesToAnExternallySpawnedDebuggeeAndDebugsIt() throws Exception {
    int port = spawnDebuggee();
    initialize();
    assertTrue("attach-mode launch succeeds", request(attachLaunch(port)).isSuccess());
    assertTrue("setBreakpoints succeeds", setBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE).isSuccess());
    assertTrue("configurationDone succeeds", request(new ConfigurationDoneRequest()).isSuccess());

    // the loop body runs three times; variables must be readable at a stop
    int stops = 0;
    boolean terminated = false;
    while (!terminated) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull("expected a debug event", event);
      if (event instanceof StoppedEvent stopped) {
        stops++;
        int threadId = stopped.getBody().getThreadId();
        if (stops == 1) {
          Map<String, String> locals = localsInTopFrame(threadId);
          assertEquals("locals readable in attach mode", "3", locals.get("count"));
        }
        assertTrue("continue succeeds", request(continueRequest(threadId)).isSuccess());
      }
      else if (event instanceof TerminatedEvent) {
        terminated = true;
      }
    }
    assertEquals("breakpoint hit once per loop iteration", 3, stops);

    assertTrue("debuggee exits", debuggee.waitFor(TIMEOUT, TimeUnit.MILLISECONDS));
    assertEquals("clean exit", 0, debuggee.exitValue());
    // stdio ownership: the fixture's output arrived on OUR pipe, not as DAP events
    assertTrue("debuggee stdout stays with the spawner (got: " + drainedDebuggeeOutput() + ")",
               drainedDebuggeeOutput().contains("fixture-total:3"));
    assertTrue("disconnect succeeds", request(new DisconnectRequest()).isSuccess());
  }

  @Test
  public void disconnectDetachesAndLetsTheDebuggeeFinish() throws Exception {
    int port = spawnDebuggee();
    initialize();
    assertTrue("attach-mode launch succeeds", request(attachLaunch(port)).isSuccess());
    assertTrue("setBreakpoints succeeds", setBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE).isSuccess());
    assertTrue("configurationDone succeeds", request(new ConfigurationDoneRequest()).isSuccess());
    awaitStopped();

    // disconnect while stopped at a breakpoint: the adapter must restore every
    // patched INT3 and detach — a leftover trap would crash the free-running
    // debuggee (non-zero exit) and a missed resume would hang it (timeout)
    assertTrue("disconnect succeeds", request(new DisconnectRequest()).isSuccess());
    assertTrue("debuggee finishes on its own after detach", debuggee.waitFor(TIMEOUT, TimeUnit.MILLISECONDS));
    assertEquals("clean exit after detach", 0, debuggee.exitValue());
    assertTrue("debuggee completed its work (got: " + drainedDebuggeeOutput() + ")",
               drainedDebuggeeOutput().contains("fixture-total:3"));
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
