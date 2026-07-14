package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.DisconnectRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.SetExceptionBreakpointsArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.SetExceptionBreakpointsRequest;
import java.util.List;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Uncaught-only exception breakpoints: with only the "uncaught" filter, a
 * throw inside a live {@code try} is skipped and execution stops only at a throw
 * no {@code catch} will handle. The fixture throws "caught-one" inside a try, then
 * "uncaught-one" with no handler — we must stop on the latter.
 */
public class UncaughtExceptionIntegrationTest extends DapIntegrationTestBase {

  @Before
  public void requireUncaughtFixture() {
    Assume.assumeTrue("uncaught fixture not built - skipping", uncaughtFixtureHl != null);
  }

  @Test
  public void uncaughtOnlySkipsCaughtAndStopsOnUncaught() throws Exception {
    initialize();
    assertTrue("launch succeeds", launch(uncaughtFixtureHl.toString()).isSuccess());
    assertTrue("uncaught filter enabled", request(exceptionBreakpoints("uncaught")).isSuccess());
    assertTrue("configurationDone succeeds", request(new ConfigurationDoneRequest()).isSuccess());

    // "caught-one" (thrown inside a try) is skipped; we stop on "uncaught-one"
    StoppedEvent stopped = awaitStopped();
    assertEquals("stopped for exception", "exception", stopped.getBody().getReason());
    String description = stopped.getBody().getDescription();
    assertTrue("stopped on the UNCAUGHT throw, not the caught one (" + description + ")",
               description != null && description.contains("uncaught-one"));

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
