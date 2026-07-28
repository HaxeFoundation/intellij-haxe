package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Uncaught-only exception breakpoints: with only the "uncaught" filter, a
 * throw inside a live {@code try} is skipped and execution stops only at a throw
 * no {@code catch} will handle. The fixture throws "caught-one" inside a try, then
 * "uncaught-one" with no handler — we must stop on the latter.
 */
@DisplayName("HashLink debugger: uncaught exception (integration)")
public class UncaughtExceptionIntegrationTest extends DapIntegrationTestBase {

  @BeforeEach
  public void requireUncaughtFixture() {
    Assumptions.assumeTrue(uncaughtFixtureHl != null, "uncaught fixture not built - skipping");
  }

  @Test
  @DisplayName("uncaught only skips caught and stops on uncaught")
  public void uncaughtOnlySkipsCaughtAndStopsOnUncaught() throws Exception {
    initialize();
    assertTrue(launch(uncaughtFixtureHl.toString()).isSuccess(), "launch succeeds");
    assertTrue(request(exceptionBreakpointsRequest(List.of("uncaught"))).isSuccess(), "uncaught filter enabled");
    configurationDone();

    // "caught-one" (thrown inside a try) is skipped; the stop is on "uncaught-one"
    StoppedEvent stopped = awaitStopped();
    assertEquals("exception", stopped.getBody().getReason(), "stopped for exception");
    String description = stopped.getBody().getDescription();
    assertTrue(description != null && description.contains("uncaught-one"), "stopped on the UNCAUGHT throw, not the caught one (" + description + ")");

    request(new DisconnectRequest());
  }
}
