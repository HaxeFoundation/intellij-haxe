package com.intellij.plugins.haxe.runner.debugger.eval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.DapConnection;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Uncaught haxe.Exception INSTANCES (as opposed to bare string throws) put
 * the eval VM in a state it can never leave forward: at the exceptionStop,
 * every continue/step RE-EXECUTES the whole throw expression — constructor,
 * stack collection, toString, then the same exceptionStop again, forever
 * (the debugger stepped in circles through the constructor and
 * could not let the program die). The adapter's way out, pinned here: clear
 * the VM's exception options, then resume — the program dies NATURALLY with
 * its own uncaught-exception stderr and exit code 1. Also pinned: a session
 * whose exception breakpoints are disabled (empty filters) never stops on
 * the uncaught throw at all — the VM's DEFAULT would.
 */
public class EvalObjectThrowLiveTest extends EvalLiveTestBase {
  @Override
  protected String fixtureMain() {
    return "EvalThrowObj";
  }



  private void startSession(List<String> filters) throws Exception {
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue("initialize", request(initialize).isSuccess());
    dapClient.pollEvent(TIMEOUT);
    launch();

    SetExceptionBreakpointsRequest exceptions = exceptionBreakpointsRequest(filters);
    assertTrue("setExceptionBreakpoints", request(exceptions).isSuccess());
    configurationDone();
  }

  private StoppedEvent awaitExceptionStop() throws Exception {
    long deadline = System.currentTimeMillis() + TIMEOUT;
    while (System.currentTimeMillis() < deadline) {
      Event event = dapClient.pollEvent(250);
      if (event instanceof StoppedEvent stopped) {
        assertEquals("the stop is the exception", "exception", stopped.getBody().getReason());
        return stopped;
      }
      if (event instanceof TerminatedEvent) {
        throw new AssertionError("terminated before the exception stop");
      }
    }
    throw new AssertionError("no exception stop within " + TIMEOUT + "ms");
  }

  private void awaitNaturalDeath() throws Exception {
    long deadline = System.currentTimeMillis() + TIMEOUT;
    while (System.currentTimeMillis() < deadline) {
      Event event = dapClient.pollEvent(250);
      if (event instanceof TerminatedEvent) {
        assertTrue("haxe exited", haxe.waitFor(TIMEOUT, TimeUnit.MILLISECONDS));
        assertNotEquals("died from the uncaught exception (non-zero exit)", 0, haxe.exitValue());
        return;
      }
      if (event instanceof StoppedEvent stopped) {
        throw new AssertionError("stopped again (" + stopped.getBody().getReason()
                                 + ") instead of dying — the uncaught re-execution loop is back");
      }
    }
    throw new AssertionError("program never died within " + TIMEOUT + "ms");
  }

  @Test(timeout = 60_000)
  public void resumeAtTheUncaughtObjectStopLetsTheProgramDieNaturally() throws Exception {
    startSession(List.of("uncaught"));
    StoppedEvent stopped = awaitExceptionStop();
    String description = stopped.getBody().getDescription();
    assertTrue("stop carries the thrown text", description != null && description.contains("uncaught-object"));

    ContinueRequest resume = continueRequest(stopped.getBody().getThreadId());
    assertTrue("resume at the uncaught stop", request(resume).isSuccess());
    awaitNaturalDeath();
  }

  @Test(timeout = 60_000)
  public void steppingAtTheUncaughtObjectStopLetsTheProgramDieNaturally() throws Exception {
    startSession(List.of("uncaught"));
    StoppedEvent stopped = awaitExceptionStop();

    StepInRequest stepIn = stepInRequest(stopped.getBody().getThreadId());
    assertTrue("stepIn at the uncaught stop", request(stepIn).isSuccess());
    awaitNaturalDeath();
  }

  @Test(timeout = 60_000)
  public void withExceptionBreakpointsDisabledAnUncaughtThrowNeverStops() throws Exception {
    // the IDE sends EMPTY filters when every exception breakpoint is disabled;
    // that must override the VM's stop-on-uncaught DEFAULT: no stop, just the
    // program's natural death
    startSession(List.of());
    awaitNaturalDeath();
  }
}
