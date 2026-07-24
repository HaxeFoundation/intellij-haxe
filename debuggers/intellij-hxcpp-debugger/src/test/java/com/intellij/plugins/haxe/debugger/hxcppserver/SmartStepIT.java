package com.intellij.plugins.haxe.debugger.hxcppserver;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Smart step into (the custom custom/stepIntoFunction request): a temporary
 * entry breakpoint on the chosen callee races a step-over. Covers static and
 * instance targets, packaged class names, the step-over fallback for a callee
 * that never runs, and the no-op stop for a class name the runtime rejects.
 */
public class SmartStepIT {

  private void stepIntoFunction(FixtureSession session, int threadId,
                                String className, String functionName) throws Exception {
    stepIntoFunction(session, threadId, className, functionName, 1);
  }

  private void stepIntoFunction(FixtureSession session, int threadId,
                                String className, String functionName, int occurrence) throws Exception {
    StepIntoFunctionRequest request = new StepIntoFunctionRequest();
    StepIntoFunctionArguments arguments = new StepIntoFunctionArguments();
    arguments.setThreadId(threadId);
    arguments.setClassName(className);
    arguments.setFunctionName(functionName);
    arguments.setOccurrence(occurrence);
    request.setArguments(arguments);
    assertTrue("stepIntoFunction " + className + "." + functionName + " #" + occurrence,
               session.request(request).isSuccess());
  }

  // stop at the smart line, remove line breakpoints so only the temp can fire
  private int stopAtSmartLine(FixtureSession session) throws Exception {
    session.setBreakpoints(FixtureSession.EX_SOURCE, new int[]{FixtureSession.SMART_LINE}, null);
    StoppedEvent stopped = session.awaitStopped();
    int threadId = session.stoppedThread(stopped);
    assertEquals(FixtureSession.SMART_LINE, session.topFrame(threadId).getLine());
    session.clearBreakpoints(FixtureSession.EX_SOURCE);
    return threadId;
  }

  private int rearmAndStop(FixtureSession session, int threadId) throws Exception {
    session.setBreakpoints(FixtureSession.EX_SOURCE, new int[]{FixtureSession.SMART_LINE}, null);
    session.resume(threadId);
    StoppedEvent stopped = session.awaitStopped();
    int stoppedThread = session.stoppedThread(stopped);
    assertEquals(FixtureSession.SMART_LINE, session.topFrame(stoppedThread).getLine());
    session.clearBreakpoints(FixtureSession.EX_SOURCE);
    return stoppedThread;
  }

  @Test
  public void entersTheChosenCalleeIncludingLaterAndPackagedOnes() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("smartstep")) {
      session.initialize("uncaught", "critical");
      session.configurationDone();
      int threadId = stopAtSmartLine(session);

      // the FIRST call on the line
      stepIntoFunction(session, threadId, "SmartStepTarget", "one");
      StoppedEvent entered = session.awaitStopped();
      assertEquals("step", entered.getBody().getReason());
      StackFrame top = session.topFrame(session.stoppedThread(entered));
      assertEquals("SmartStepTarget.one", top.getName());
      assertEquals(FixtureSession.SMART_ONE_LINE, top.getLine());

      // a LATER call: runs through the earlier calls before landing
      threadId = rearmAndStop(session, session.stoppedThread(entered));
      stepIntoFunction(session, threadId, "SmartStepTarget", "combine");
      entered = session.awaitStopped();
      top = session.topFrame(session.stoppedThread(entered));
      assertEquals("step", entered.getBody().getReason());
      assertEquals("SmartStepTarget.combine", top.getName());
      assertEquals(FixtureSession.SMART_COMBINE_LINE, top.getLine());

      // a PACKAGED callee: the runtime class name is the dotted FQN
      threadId = rearmAndStop(session, session.stoppedThread(entered));
      stepIntoFunction(session, threadId, "fix.PackCounter", "bump");
      entered = session.awaitStopped();
      top = session.topFrame(session.stoppedThread(entered));
      assertEquals("step", entered.getBody().getReason());
      assertEquals("fix.PackCounter.bump", top.getName());
      assertEquals("PackCounter.hx", top.getSource().getName());
    }
  }

  @Test
  public void anInstanceMethodChainEntersTheChosenLink() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("chain")) {
      session.initialize("uncaught", "critical");
      session.configurationDone();
      session.setBreakpoints(FixtureSession.EX_SOURCE, new int[]{FixtureSession.CHAIN_LINE}, null);
      StoppedEvent stopped = session.awaitStopped();
      int threadId = session.stoppedThread(stopped);
      session.clearBreakpoints(FixtureSession.EX_SOURCE);

      stepIntoFunction(session, threadId, "ChainTarget", "test2");
      StoppedEvent entered = session.awaitStopped();
      assertEquals("step", entered.getBody().getReason());
      assertEquals("ChainTarget.test2", session.topFrame(session.stoppedThread(entered)).getName());
    }
  }

  /**
   * A callee invoked TWICE on one line ({@code cfg.dup(1).mid().dup(2)}):
   * occurrence=2 must land in the SECOND invocation — the entry breakpoint
   * alone stops at the first. The argument value proves which one we entered.
   */
  @Test
  public void occurrencePicksTheLaterInvocationOfADuplicatedCallee() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("dupchain")) {
      session.initialize("uncaught", "critical");
      session.configurationDone();
      session.setBreakpoints(FixtureSession.EX_SOURCE, new int[]{FixtureSession.DUP_CHAIN_LINE}, null);
      StoppedEvent stopped = session.awaitStopped();
      int threadId = session.stoppedThread(stopped);
      session.clearBreakpoints(FixtureSession.EX_SOURCE);

      stepIntoFunction(session, threadId, "DupChainTarget", "dup", 2);
      StoppedEvent entered = session.awaitStopped();
      assertEquals("step", entered.getBody().getReason());
      StackFrame top = session.topFrame(session.stoppedThread(entered));
      assertEquals("DupChainTarget.dup", top.getName());
      // no exact-line assert: the entry stop reports the signature line for
      // functions with parameters. The ARGUMENT proves which invocation.
      assertEquals("the SECOND invocation passes v=2", "2", session.evaluate("v", top.getId()));
    }
  }

  @Test
  public void aCalleeThatNeverRunsFallsBackToAStepOver() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("smartstep")) {
      session.initialize("uncaught", "critical");
      session.configurationDone();
      int threadId = stopAtSmartLine(session);

      stepIntoFunction(session, threadId, "SmartStepTarget", "nosuchfn");
      StoppedEvent landed = session.awaitStopped();
      assertEquals("step", landed.getBody().getReason());
      assertNotEquals("moved off the line like a plain step over",
                      FixtureSession.SMART_LINE, session.topFrame(session.stoppedThread(landed)).getLine());
    }
  }

  /**
   * hxcpp validates the class name against its class table and arms NOTHING
   * for an unknown one — the server must not gamble with an unguarded step
   * (it could run forever): it re-reports the current stop instead.
   */
  @Test
  public void aRuntimeUnknownClassNameIsANoOpStop() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("smartstep")) {
      session.initialize("uncaught", "critical");
      session.configurationDone();
      int threadId = stopAtSmartLine(session);

      stepIntoFunction(session, threadId, "MainEx.SmartStepTarget", "one");
      StoppedEvent unmoved = session.awaitStopped();
      assertEquals("step", unmoved.getBody().getReason());
      assertEquals("did not move (and did not run away)",
                   FixtureSession.SMART_LINE, session.topFrame(session.stoppedThread(unmoved)).getLine());
    }
  }
}
