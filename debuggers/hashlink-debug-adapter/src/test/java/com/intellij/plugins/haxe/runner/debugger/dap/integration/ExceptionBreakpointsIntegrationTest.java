package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Exception breakpoints: with the "All Exceptions" filter enabled, the
 * debugger stops at a throw site (an OThrow) with reason "exception", the thrown
 * value in the description, and an inspectable frame. Uses the fixture's caught
 * {@code throw "boom"} in Main.throwDemo() as a deterministic early throw.
 */
public class ExceptionBreakpointsIntegrationTest extends DapIntegrationTestBase {

  @Test
  public void breaksOnThrownExceptionWithValueAndFrame() throws Exception {
    // pre-4.3 compilers wrap `throw "boom"` through Exception.thrown with a
    // message layout the adapter does not decode (the description degrades to
    // the wrapper's class name) — not supported by the current adapter
    assumeFixtureHaxe43Plus();
    initialize();
    assertTrue(launch().isSuccess(), "launch succeeds");
    assertTrue(request(exceptionBreakpointsRequest(List.of("all"))).isSuccess(), "exception filter enabled");
    configurationDone();

    // the fixture's `throw "boom"` stops the debuggee with reason "exception"
    StoppedEvent stopped = awaitStopped();
    assertEquals("exception", stopped.getBody().getReason(), "stopped for exception");
    assertNotNull(stopped.getBody().getDescription(), "exception description");
    assertTrue(stopped.getBody().getDescription().contains("boom"), "description names the thrown value (" + stopped.getBody().getDescription() + ")");

    // the throwing frame is inspectable
    int threadId = stopped.getBody().getThreadId();
    assertEquals("Main.throwDemo", topFrameName(threadId), "paused in the throwing function");

    request(new DisconnectRequest());
  }
}
