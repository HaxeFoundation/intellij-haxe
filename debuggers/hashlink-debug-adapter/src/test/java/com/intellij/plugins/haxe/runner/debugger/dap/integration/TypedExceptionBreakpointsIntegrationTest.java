package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Type-specific exception breakpoints: a filter naming exception class
 * "Boom" must skip an unrelated {@code haxe.Exception} throw and stop only at the
 * {@code Kaboom} throw — Kaboom extends Boom, so this also proves subtype matching
 * (the match covers the thrown value's class and its superclasses).
 */
public class TypedExceptionBreakpointsIntegrationTest extends DapIntegrationTestBase {

  @BeforeEach
  public void requireTypedThrowFixture() {
    Assumptions.assumeTrue(typedThrowFixtureHl != null, "typedthrow fixture not built - skipping");
  }

  @Test
  public void stopsOnlyOnTheFilteredTypeAndItsSubclasses() throws Exception {
    initialize();
    assertTrue(launch(typedThrowFixtureHl.toString()).isSuccess(), "launch succeeds");
    // filter on the base class "Boom" — the first throw (haxe.Exception) is not a
    // Boom and must be skipped; the Kaboom throw (a Boom subclass) must stop
    assertTrue(request(exceptionTypeFilter("Boom")).isSuccess(), "type filter set");
    configurationDone();

    StoppedEvent stopped = awaitStopped();
    assertEquals("exception", stopped.getBody().getReason(), "stopped for an exception");
    String description = stopped.getBody().getDescription();
    // the throwing value's runtime class is Kaboom (not the skipped haxe.Exception)
    assertTrue(description != null && description.contains("Kaboom"), "stopped on the Kaboom throw, not the unrelated one (" + description + ")");

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
