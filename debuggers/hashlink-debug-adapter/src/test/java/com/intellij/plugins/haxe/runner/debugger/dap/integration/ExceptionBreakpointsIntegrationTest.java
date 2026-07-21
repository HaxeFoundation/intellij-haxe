package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.util.List;
import org.junit.Test;

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
    assertTrue("launch succeeds", launch().isSuccess());
    assertTrue("exception filter enabled", request(exceptionBreakpoints("all")).isSuccess());
    assertTrue("configurationDone succeeds", request(new ConfigurationDoneRequest()).isSuccess());

    // the fixture's `throw "boom"` stops the debuggee with reason "exception"
    StoppedEvent stopped = awaitStopped();
    assertEquals("stopped for exception", "exception", stopped.getBody().getReason());
    assertNotNull("exception description", stopped.getBody().getDescription());
    assertTrue("description names the thrown value (" + stopped.getBody().getDescription() + ")",
               stopped.getBody().getDescription().contains("boom"));

    // the throwing frame is inspectable
    int threadId = stopped.getBody().getThreadId();
    assertEquals("paused in the throwing function", "Main.throwDemo", topFrameName(threadId));

    request(new DisconnectRequest());
  }

  private static SetExceptionBreakpointsRequest exceptionBreakpoints(String... filters) {
    SetExceptionBreakpointsRequest request = new SetExceptionBreakpointsRequest();
    SetExceptionBreakpointsArguments arguments = new SetExceptionBreakpointsArguments();
    arguments.setFilters(List.of(filters));
    request.setArguments(arguments);
    return request;
  }
}
