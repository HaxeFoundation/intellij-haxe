package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Presentation of a haxe.Exception's {@code __nativeStack}: its entries are
 * {@code hl.NativeArray<hl_symbol>} — code return addresses. Rather than the opaque
 * {@code hl_symbol @ 0x..} the runtime would otherwise show, each is resolved to a
 * "Class.method (File.hx:line)" label using the same debug info the call stack uses.
 *
 * The fixture builds an exception two calls deep (deeper -> build -> main), so the
 * captured stack must name those fixture methods with source locations.
 */
@DisplayName("HashLink debugger: native stack trace (integration)")
public class NativeStackTraceIntegrationTest extends DapIntegrationTestBase {

  private static final int STACKTRACE_BREAK_LINE = 11; // Sys.println line; `err` is a live local

  @BeforeEach
  public void requireStackTraceFixture() {
    Assumptions.assumeTrue(stacktraceFixtureHl != null, "stacktrace fixture not built - skipping");
  }

  @Test
  @DisplayName("resolves native stack entries to source locations")
  public void resolvesNativeStackEntriesToSourceLocations() throws Exception {
    // pre-4.3 compilers store __nativeStack in a shape whose entries the
    // adapter cannot resolve to source locations (they stay raw Bytes
    // addresses) — not supported by the current adapter
    assumeFixtureHaxe43Plus();
    initialize();
    assertTrue(launch(stacktraceFixtureHl.toString()).isSuccess(), "launch succeeds");
    assertTrue(setBreakpoint("StackTrace.hx", STACKTRACE_BREAK_LINE).isSuccess(), "setBreakpoints succeeds");
    configurationDone();
    StoppedEvent stopped = awaitStopped();

    // the live local `err` is a haxe.Exception; expand it to reach __nativeStack
    Variable err = findVariable(topFrameVariables(stopped.getBody().getThreadId()), "err");
    assertNotNull(err, "local err present");
    assertTrue(err.getVariablesReference() > 0, "err is expandable");

    Variable nativeStack = findVariable(variables(err.getVariablesReference()), "__nativeStack");
    assertNotNull(nativeStack, "__nativeStack present");
    assertTrue(nativeStack.getVariablesReference() > 0, "__nativeStack is expandable");

    List<String> entries = new ArrayList<>();
    for (Variable entry : variables(nativeStack.getVariablesReference())) {
      entries.add(entry.getValue());
    }
    assertFalse(entries.isEmpty(), "__nativeStack has entries");
    String joined = String.join(" | ", entries);

    // the captured call chain is resolved to "Class.method (File.hx:line)" — the
    // whole point: source locations instead of opaque hl_symbol pointers. build()
    // and main() survive inlining; each carries a real source location.
    assertTrue(joined.contains("StackTrace.build (StackTrace.hx:"), "build() resolved with a location (" + joined + ")");
    assertTrue(joined.contains("StackTrace.main (StackTrace.hx:"), "main() resolved with a location (" + joined + ")");

    // at least two entries have the full "Class.method (File.hx:line)" shape
    int located = 0;
    for (String value : entries) {
      // a fully located fixture frame: "StackTrace.<method> (StackTrace.hx:<line>)"
      if (value.matches("StackTrace\\.\\w+ \\(StackTrace\\.hx:\\d+\\)")) located++;
    }
    assertTrue(located >= 2, "two+ fixture frames carry a File.hx:line location (" + joined + ")");

    // NOTE: entries are deliberately allowed to remain opaque `hl_symbol @ 0x..`:
    // some VM versions (1.16+) capture C-runtime frames that have no Haxe source
    // to resolve to — showing the raw symbol is the intended graceful fallback.

    request(new DisconnectRequest());
  }
}
