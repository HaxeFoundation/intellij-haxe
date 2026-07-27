package com.intellij.plugins.haxe.debugger.hxcppserver;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Breakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.util.List;
import org.junit.jupiter.api.Test;

import static com.intellij.plugins.haxe.debugger.hxcppserver.FixtureSession.setVariableRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Breakpoints, variables and the evaluate matrix against the plain fixture
 * (Main.hx: add() is called three times with amount 0/1/2, then the program
 * exits). Mirrors the M2–M5 python probes.
 */
public class BreakpointsAndEvaluateIT {

  @Test
  public void aBreakpointHitsAndCarriesLocals() throws Exception {
    try (FixtureSession session = FixtureSession.launchMain()) {
      session.initialize("uncaught", "critical");
      session.setBreakpoints(FixtureSession.MAIN_SOURCE, new int[]{FixtureSession.MAIN_ADD_LINE}, null);
      session.configurationDone();

      StoppedEvent hit = session.awaitStopped();
      assertEquals("breakpoint", hit.getBody().getReason());
      assertTrue(hit.getBody().getHitBreakpointIds() != null
                                             && !hit.getBody().getHitBreakpointIds().isEmpty(), "hitBreakpointIds present");
      int threadId = session.stoppedThread(hit);
      StackFrame top = session.topFrame(threadId);
      assertEquals(FixtureSession.MAIN_ADD_LINE, top.getLine());
      assertEquals(FixtureSession.MAIN_SOURCE, top.getSource().getName());

      List<Variable> locals = session.variables(session.localsReference(top.getId()));
      assertEquals("0", session.variable(locals, "amount").getValue());
      assertEquals("0", session.variable(locals, "current").getValue());
      session.resume(threadId);
    }
  }

  @Test
  public void aLineWithoutCodeIsRejected() throws Exception {
    // Verification against the macro-baked line table: a comment or blank
    // line is rejected (unverified + message, the usual cause being a stale
    // binary). The code line in the same request must still verify AND
    // actually fire, which pins that the baked table agrees with what hxcpp
    // instruments (HXLINE).
    try (FixtureSession session = FixtureSession.launchMain()) {
      session.initialize("uncaught", "critical");

      List<Breakpoint> results = session.setBreakpointsRaw(FixtureSession.MAIN_SOURCE,
                                                           new int[]{FixtureSession.MAIN_COMMENT_LINE,
                                                             FixtureSession.MAIN_ADD_LINE,
                                                             FixtureSession.MAIN_BLANK_LINE}, null)
        .getBody().getBreakpoints();

      assertEquals(3, results.size());
      assertFalse(results.get(0).isVerified(), "comment line rejected");
      assertEquals(Integer.valueOf(FixtureSession.MAIN_COMMENT_LINE), results.get(0).getLine(), "rejected result keeps the requested line");
      assertTrue(results.get(0).getMessage() != null && results.get(0).getMessage().contains("no executable code"), "rejection names the reason (was: " + results.get(0).getMessage() + ")");
      assertTrue(results.get(1).isVerified(), "code line verified");
      assertFalse(results.get(2).isVerified(), "blank line rejected");

      session.configurationDone();

      StoppedEvent hit = session.awaitStopped();
      int threadId = session.stoppedThread(hit);
      assertEquals(FixtureSession.MAIN_ADD_LINE, session.topFrame(threadId).getLine(), "the verified line really fires");
      session.resume(threadId);
    }
  }

  @Test
  public void aConditionalBreakpointStopsOnlyWhenTrue() throws Exception {
    try (FixtureSession session = FixtureSession.launchMain()) {
      session.initialize("uncaught", "critical");
      session.setBreakpoints(FixtureSession.MAIN_SOURCE, new int[]{FixtureSession.MAIN_ADD_LINE}, "amount == 2");
      session.configurationDone();

      StoppedEvent hit = session.awaitStopped();
      int threadId = session.stoppedThread(hit);
      int frameId = session.topFrame(threadId).getId();
      // hits with amount 0 and 1 were silently resumed
      assertEquals("2", session.evaluate("amount", frameId));
      session.resume(threadId);
      assertEquals(0, session.awaitExit(), "the program ran to completion");
    }
  }

  @Test
  public void theEvaluateMatrix() throws Exception {
    try (FixtureSession session = FixtureSession.launchMain()) {
      session.initialize("uncaught", "critical");
      session.setBreakpoints(FixtureSession.MAIN_SOURCE, new int[]{FixtureSession.MAIN_ADD_LINE}, "amount == 2");
      session.configurationDone();
      StoppedEvent hit = session.awaitStopped();
      int threadId = session.stoppedThread(hit);
      int frameId = session.topFrame(threadId).getId();

      // frame locals + compound expressions (current is 0+1 = 1 by the third call)
      assertEquals("3", session.evaluate("current + amount", frameId));
      assertEquals("true", session.evaluate("amount > 1 && current >= 0", frameId));

      // assignment writes back to the debuggee
      session.evaluate("current = 500", frameId);
      assertEquals("500", session.evaluate("current", frameId));

      // static calls run REAL compiled code and their effects persist
      assertEquals("5", session.evaluate("Counter.bump(5)", frameId));
      assertEquals("7", session.evaluate("Counter.bump(2)", frameId));
      assertEquals("7", session.evaluate("Counter.total", frameId));

      // dotted package paths resolve (the type-path binder)
      assertEquals("3", session.evaluate("fix.PackCounter.bump(3)", frameId));
      assertEquals("10", session.evaluate("fix.PackCounter.total + Counter.total", frameId));

      // an unresolvable path errors instead of silently returning null
      assertFalse(session.evaluateRaw("nosuch.pack.Thing", frameId).isSuccess(), "unknown identifier fails");

      session.resume(threadId);
    }
  }

  @Test
  public void setVariableWritesThroughToTheFrame() throws Exception {
    try (FixtureSession session = FixtureSession.launchMain()) {
      session.initialize("uncaught", "critical");
      session.setBreakpoints(FixtureSession.MAIN_SOURCE, new int[]{FixtureSession.MAIN_ADD_LINE}, null);
      session.configurationDone();
      StoppedEvent hit = session.awaitStopped();
      int threadId = session.stoppedThread(hit);
      int reference = session.localsReference(session.topFrame(threadId).getId());

      SetVariableRequest request = setVariableRequest(reference, "current", "41");
      assertTrue(session.request(request).isSuccess(), "setVariable");

      assertEquals("41", session.variable(session.variables(reference), "current").getValue());
      session.resume(threadId);
    }
  }
}
