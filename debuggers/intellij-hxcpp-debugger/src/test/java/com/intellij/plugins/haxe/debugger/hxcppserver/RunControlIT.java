package com.intellij.plugins.haxe.debugger.hxcppserver;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Pause/resume/step against the live fixtures — the scenarios that caught the
 * real-world session wedges (getter deadlock, fault-in-renderer) and the
 * multi-threaded shape real apps have.
 */
public class RunControlIT {

  @Test
  public void pauseInspectResumeTwiceStaysResponsive() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("spin")) {
      session.initialize("uncaught", "critical");
      session.configurationDone();
      session.awaitOutputAbove("beat", -1); // spinning

      for (int round = 1; round <= 2; round++) {
        session.pause();
        StoppedEvent stopped = session.awaitStopped();
        assertEquals("pause", stopped.getBody().getReason());
        int threadId = session.stoppedThread(stopped);

        StackFrame top = session.topFrame(threadId);
        int reference = session.localsReference(top.getId());
        List<Variable> locals = session.variables(reference);
        assertTrue("round " + round + ": spin locals present", !locals.isEmpty());

        int beats = session.outputCount("beat");
        session.resume(threadId);
        session.awaitOutputAbove("beat", beats); // really running again
      }
    }
  }

  @Test
  public void breakpointsStayArmedAcrossAPause() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("spin")) {
      session.initialize("uncaught", "critical");
      // the heartbeat line inside spin() fires every ~1s of spinning
      session.setBreakpoints(FixtureSession.EX_SOURCE, new int[]{53}, null);
      session.configurationDone();

      StoppedEvent hit = session.awaitStopped();
      assertEquals("breakpoint", hit.getBody().getReason());
      int threadId = session.stoppedThread(hit);

      session.resume(threadId);
      Thread.sleep(200);
      session.pause();
      StoppedEvent paused = session.awaitStopped();
      // a breakpoint may win the race with the pause; both are healthy stops
      assertTrue("stop reason", List.of("pause", "breakpoint").contains(paused.getBody().getReason()));
      session.stackTrace(session.stoppedThread(paused));

      session.resume(session.stoppedThread(paused));
      StoppedEvent second = session.awaitStopped(); // the breakpoint again
      assertEquals("breakpoint", second.getBody().getReason());
    }
  }

  @Test
  public void stepOverAtABreakpointReportsAStepStop() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("spin")) {
      session.initialize("uncaught", "critical");
      session.setBreakpoints(FixtureSession.EX_SOURCE, new int[]{53}, null);
      session.configurationDone();
      StoppedEvent hit = session.awaitStopped();
      int threadId = session.stoppedThread(hit);
      int lineBefore = session.topFrame(threadId).getLine();

      session.next(threadId);
      StoppedEvent stepped = session.awaitStopped();
      assertEquals("step", stepped.getBody().getReason());
      assertNotEquals("the step moved off the line", lineBefore, session.topFrame(threadId).getLine());
    }
  }

  /**
   * The multi-threaded shape (lime ThreadPool): workers never opt into
   * debugging, so a pause stops ONLY main — worker heartbeats keep flowing
   * while main is paused, and resume brings main back.
   */
  @Test
  public void workersKeepRunningWhileMainIsPaused() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("threads")) {
      session.initialize("uncaught", "critical");
      session.configurationDone();
      session.awaitOutputAbove("beat", -1);
      session.awaitOutputAbove("worker", -1);

      session.pause();
      StoppedEvent stopped = session.awaitStopped();
      int threadId = session.stoppedThread(stopped);

      Thread.sleep(200); // let in-flight main output settle
      int beatsWhilePaused = session.outputCount("beat");
      int workersAtPause = session.outputCount("worker");
      session.awaitOutputAbove("worker", workersAtPause); // workers still alive
      assertEquals("main is really paused", beatsWhilePaused, session.outputCount("beat"));

      session.resume(threadId);
      session.awaitOutputAbove("beat", beatsWhilePaused); // main runs again
    }
  }

  /**
   * The regression that froze real sessions: a local whose @:isVar property
   * getter needs a mutex the PAUSED main thread holds. Rendering must not run
   * the getter — the raw backing value shows and the session stays live.
   */
  @Test
  public void renderingNeverRunsGettersEvenAgainstAHeldLock() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("getterlock")) {
      session.initialize("uncaught", "critical");
      session.configurationDone();
      session.awaitOutputAbove("beat", -1);

      session.pause();
      StoppedEvent stopped = session.awaitStopped();
      int threadId = session.stoppedThread(stopped);
      int reference = session.localsReference(session.topFrame(threadId).getId());
      Variable box = session.variable(session.variables(reference), "box");
      List<Variable> fields = session.variables(box.getVariablesReference());
      assertEquals("raw backing value, getter never invoked", "13",
                   session.variable(fields, "danger").getValue());

      int beats = session.outputCount("beat");
      session.resume(threadId);
      session.awaitOutputAbove("beat", beats);
    }
  }
}
