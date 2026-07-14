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
 * Type-specific exception breakpoints: a filter naming exception class
 * "Boom" must skip an unrelated {@code haxe.Exception} throw and stop only at the
 * {@code Kaboom} throw — Kaboom extends Boom, so this also proves subtype matching
 * (we match the thrown value's class and its superclasses).
 */
public class TypedExceptionBreakpointsIntegrationTest extends DapIntegrationTestBase {

  @Before
  public void requireTypedThrowFixture() {
    Assume.assumeTrue("typedthrow fixture not built - skipping", typedThrowFixtureHl != null);
  }

  @Test
  public void stopsOnlyOnTheFilteredTypeAndItsSubclasses() throws Exception {
    initialize();
    assertTrue("launch succeeds", launch(typedThrowFixtureHl.toString()).isSuccess());
    // filter on the base class "Boom" — the first throw (haxe.Exception) is not a
    // Boom and must be skipped; the Kaboom throw (a Boom subclass) must stop us
    assertTrue("type filter set", request(exceptionTypeFilter("Boom")).isSuccess());
    assertTrue("configurationDone succeeds", request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent stopped = awaitStopped();
    assertEquals("stopped for an exception", "exception", stopped.getBody().getReason());
    String description = stopped.getBody().getDescription();
    // the throwing value's runtime class is Kaboom (not the skipped haxe.Exception)
    assertTrue("stopped on the Kaboom throw, not the unrelated one (" + description + ")",
               description != null && description.contains("Kaboom"));

    request(new DisconnectRequest());
  }

  private static SetExceptionBreakpointsRequest exceptionTypeFilter(String... types) {
    SetExceptionBreakpointsRequest request = new SetExceptionBreakpointsRequest();
    SetExceptionBreakpointsArguments arguments = new SetExceptionBreakpointsArguments();
    arguments.setFilters(List.of()); // no "all"/"uncaught" — only the type filter
    arguments.setFilterTypes(List.of(types));
    request.setArguments(arguments);
    return request;
  }
}
