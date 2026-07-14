package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.DisconnectRequest;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Presentation of a haxe.Exception's {@code __nativeStack}: its entries are
 * {@code hl.NativeArray<hl_symbol>} — code return addresses. Rather than the opaque
 * {@code hl_symbol @ 0x..} the runtime would otherwise show, each is resolved to a
 * "Class.method (File.hx:line)" label using the same debug info the call stack uses.
 *
 * The fixture builds an exception two calls deep (deeper -> build -> main), so the
 * captured stack must name those fixture methods with source locations.
 */
public class NativeStackTraceIntegrationTest extends DapIntegrationTestBase {

  private static final int STACKTRACE_BREAK_LINE = 11; // Sys.println line; `err` is a live local

  @Before
  public void requireStackTraceFixture() {
    Assume.assumeTrue("stacktrace fixture not built - skipping", stacktraceFixtureHl != null);
  }

  @Test
  public void resolvesNativeStackEntriesToSourceLocations() throws Exception {
    initialize();
    assertTrue("launch succeeds", launch(stacktraceFixtureHl.toString()).isSuccess());
    assertTrue("setBreakpoints succeeds", setBreakpoint("StackTrace.hx", STACKTRACE_BREAK_LINE).isSuccess());
    assertTrue("configurationDone succeeds", request(new ConfigurationDoneRequest()).isSuccess());
    StoppedEvent stopped = awaitStopped();

    // the live local `err` is a haxe.Exception; expand it to reach __nativeStack
    Variable err = findVariable(topFrameVariables(stopped.getBody().getThreadId()), "err");
    assertNotNull("local err present", err);
    assertTrue("err is expandable", err.getVariablesReference() > 0);

    Variable nativeStack = findVariable(variables(err.getVariablesReference()), "__nativeStack");
    assertNotNull("__nativeStack present", nativeStack);
    assertTrue("__nativeStack is expandable", nativeStack.getVariablesReference() > 0);

    List<String> entries = new ArrayList<>();
    for (Variable entry : variables(nativeStack.getVariablesReference())) {
      entries.add(entry.getValue());
    }
    assertFalse("__nativeStack has entries", entries.isEmpty());
    String joined = String.join(" | ", entries);

    // the captured call chain is resolved to "Class.method (File.hx:line)" — the
    // whole point: source locations instead of opaque hl_symbol pointers. build()
    // and main() survive inlining; each carries a real source location.
    assertTrue("build() resolved with a location (" + joined + ")",
               joined.contains("StackTrace.build (StackTrace.hx:"));
    assertTrue("main() resolved with a location (" + joined + ")",
               joined.contains("StackTrace.main (StackTrace.hx:"));

    // at least two entries have the full "Class.method (File.hx:line)" shape
    int located = 0;
    for (String value : entries) {
      if (value.matches("StackTrace\\.\\w+ \\(StackTrace\\.hx:\\d+\\)")) located++;
    }
    assertTrue("two+ fixture frames carry a File.hx:line location (" + joined + ")", located >= 2);

    // nothing is left as the raw `hl_symbol @ 0x..` the runtime would otherwise show
    assertFalse("no entry left as an opaque hl_symbol pointer (" + joined + ")",
                joined.contains("hl_symbol @"));

    request(new DisconnectRequest());
  }
}
