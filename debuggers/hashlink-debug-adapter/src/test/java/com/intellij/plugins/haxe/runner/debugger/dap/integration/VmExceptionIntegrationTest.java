package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

  @BeforeEach
  public void requireVmFixture() {
    Assumptions.assumeTrue(vmFixtureHl != null, "vm fixture not built - skipping");
  }

  @Test
  public void vmFilterStopsAtTheNullAccessWithTheRealMessageAndLocals() throws Exception {
    initialize();
    assertTrue(launch(vmFixtureHl.toString()).isSuccess(), "launch succeeds");
    assertTrue(request(exceptionBreakpointsRequest(List.of("vm"))).isSuccess(), "vm filter enabled");
    configurationDone();

    StoppedEvent stopped = awaitStopped();
    assertEquals("exception", stopped.getBody().getReason(), "stopped for exception");
    int threadId = stopped.getBody().getThreadId();

    // the description carries the VM's own message, not a generic hint —
    // this is what tells the user null-access vs out-of-bounds at a glance
    String description = stopped.getBody().getDescription();
    assertTrue(description != null && description.contains("Null access"), "description has the runtime message (" + description + ")");

    // the throwing Haxe frame is recovered even though the trap fired inside C:
    // top frame is VmError.main at the null-access line
    List<StackFrame> frames = stackTrace(threadId).getBody().getStackFrames();
    StackFrame top = frames.get(0);
    assertTrue(top.getName().contains("main"), "top frame is '" + top.getName() + "', expected VmError.main");
    assertNotNull(top.getSource(), "throwing frame has a source");
    assertTrue(top.getSource().getPath().endsWith("VmError.hx"), "source is VmError.hx");
    assertEquals(nullAccessLine(), top.getLine(), "stopped at the null-access line");

    // and its locals are readable — `maybe` is the null that caused the access
    Map<String, String> locals = localsInTopFrame(threadId);
    assertTrue(locals.containsKey("maybe"), "local 'maybe' visible in the throwing frame: " + locals);
    assertEquals("null", locals.get("maybe").trim(), "maybe is null");

    request(new DisconnectRequest());
  }

  @Test
  public void allFilterDoesNotCatchAVmRaisedError() throws Exception {
    initialize();
    assertTrue(launch(vmFixtureHl.toString()).isSuccess(), "launch succeeds");
    // the OThrow-based "all" filter has no bytecode throw site to trap here
    assertTrue(request(exceptionBreakpointsRequest(List.of("all"))).isSuccess(), "all filter enabled");
    configurationDone();

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
      if (event instanceof ExitedEvent || event instanceof TerminatedEvent) {
        break;
      }
    }
    assertTrue(!stoppedForException, "the 'all' (OThrow) filter must NOT stop on a VM-raised null access");
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
}
