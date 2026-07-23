package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * The "vm" exception filter: a VM-raised error (a null access, raised by
 * HashLink's C runtime through hl_throw with NO bytecode OThrow) must stop at
 * the offending Haxe line — recovered from a stop inside C code — with the
 * ACTUAL runtime message ("Null access .length", read from the thread's
 * exc_value at hl_throw's own debug break) in the stop description, and the
 * frame's locals inspectable. The OThrow-based "all" filter, by contrast,
 * must NOT catch it.
 */
public class VmExceptionIntegrationTest extends DapIntegrationTestBase {

  @Before
  public void requireVmFixture() {
    Assume.assumeTrue("vm fixture not built - skipping", vmFixtureHl != null);
  }

  @Test
  public void vmFilterStopsAtTheNullAccessWithTheRealMessageAndLocals() throws Exception {
    initialize();
    assertTrue("launch succeeds", launch(vmFixtureHl.toString()).isSuccess());
    assertTrue("vm filter enabled", request(exceptionBreakpoints("vm")).isSuccess());
    assertTrue("configurationDone succeeds", request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent stopped = awaitStopped();
    assertEquals("stopped for exception", "exception", stopped.getBody().getReason());
    int threadId = stopped.getBody().getThreadId();

    // the description carries the VM's own message, not a generic hint —
    // this is what tells the user null-access vs out-of-bounds at a glance
    String description = stopped.getBody().getDescription();
    assertTrue("description has the runtime message (" + description + ")",
               description != null && description.contains("Null access"));

    // the throwing Haxe frame is recovered even though the trap fired inside C:
    // top frame is VmError.main at the null-access line
    List<StackFrame> frames = stackTrace(threadId).getBody().getStackFrames();
    StackFrame top = frames.get(0);
    assertTrue("top frame is '" + top.getName() + "', expected VmError.main",
               top.getName().contains("main"));
    assertNotNull("throwing frame has a source", top.getSource());
    assertTrue("source is VmError.hx", top.getSource().getPath().endsWith("VmError.hx"));
    assertEquals("stopped at the null-access line", nullAccessLine(), top.getLine());

    // and its locals are readable — `maybe` is the null that caused the access
    Map<String, String> locals = localsInTopFrame(threadId);
    assertTrue("local 'maybe' visible in the throwing frame: " + locals, locals.containsKey("maybe"));
    assertEquals("maybe is null", "null", locals.get("maybe").trim());

    request(new DisconnectRequest());
  }

  @Test
  public void allFilterDoesNotCatchAVmRaisedError() throws Exception {
    initialize();
    assertTrue("launch succeeds", launch(vmFixtureHl.toString()).isSuccess());
    // the OThrow-based "all" filter has no bytecode throw site to trap here
    assertTrue("all filter enabled", request(exceptionBreakpoints("all")).isSuccess());
    assertTrue("configurationDone succeeds", request(new ConfigurationDoneRequest()).isSuccess());

    // the program runs to termination (the null access escapes the OThrow traps)
    // rather than stopping — the very point the vm filter exists to fix
    boolean stoppedForException = false;
    long deadline = System.currentTimeMillis() + TIMEOUT;
    while (System.currentTimeMillis() < deadline) {
      var event = client.pollEvent(500);
      if (event == null) {
        continue;
      }
      if (event instanceof StoppedEvent stopped && "exception".equals(stopped.getBody().getReason())) {
        stoppedForException = true;
        break;
      }
      if (event instanceof com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.ExitedEvent
          || event instanceof com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.TerminatedEvent) {
        break;
      }
    }
    assertTrue("the 'all' (OThrow) filter must NOT stop on a VM-raised null access", !stoppedForException);
  }

  private int nullAccessLine() throws Exception {
    List<String> lines = Files.readAllLines(fixtureSrcDir.resolve("VmError.hx"));
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).contains("// bp:nullaccess")) {
        return i + 1;
      }
    }
    throw new AssertionError("no '// bp:nullaccess' marker in VmError.hx");
  }

  private static SetExceptionBreakpointsRequest exceptionBreakpoints(String... filters) {
    SetExceptionBreakpointsRequest request = new SetExceptionBreakpointsRequest();
    SetExceptionBreakpointsArguments arguments = new SetExceptionBreakpointsArguments();
    arguments.setFilters(List.of(filters));
    request.setArguments(arguments);
    return request;
  }
}
