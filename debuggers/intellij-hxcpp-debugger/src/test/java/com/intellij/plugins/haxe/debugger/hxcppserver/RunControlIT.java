package com.intellij.plugins.haxe.debugger.hxcppserver;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pause/resume/step against the live fixtures — the scenarios that caught the
 * real-world session wedges (getter deadlock, fault-in-renderer) and the
 * multi-threaded shape real apps have.
 */
@DisplayName("HXCPP debugger: run control (integration)")
public class RunControlIT {

  @Test
  @DisplayName("pause inspect resume twice stays responsive")
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
        assertTrue(!locals.isEmpty(), "round " + round + ": spin locals present");

        int beats = session.outputCount("beat");
        session.resume(threadId);
        session.awaitOutputAbove("beat", beats); // really running again
      }
    }
  }

  @Test
  @DisplayName("breakpoints stay armed across a pause")
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
      assertTrue(List.of("pause", "breakpoint").contains(paused.getBody().getReason()), "stop reason");
      session.stackTrace(session.stoppedThread(paused));

      session.resume(session.stoppedThread(paused));
      StoppedEvent second = session.awaitStopped(); // the breakpoint again
      assertEquals("breakpoint", second.getBody().getReason());
    }
  }

  @Test
  @DisplayName("step over at a breakpoint reports a step stop")
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
      assertNotEquals(lineBefore, session.topFrame(threadId).getLine(), "the step moved off the line");
    }
  }

  /**
   * The multi-threaded shape (lime ThreadPool): every thread opts into
   * debugging as it attaches, so a pause freezes the WHOLE program — worker
   * heartbeats stop together with main's — and one resume brings everything
   * back.
   */
  @Test
  @DisplayName("pause freezes workers together with main")
  public void pauseFreezesWorkersTogetherWithMain() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("threads")) {
      session.initialize("uncaught", "critical");
      session.configurationDone();
      session.awaitOutputAbove("beat", -1);
      session.awaitOutputAbove("worker", -1);

      session.pause();
      StoppedEvent stopped = session.awaitStopped();
      int threadId = session.stoppedThread(stopped);

      Thread.sleep(200); // let in-flight output settle
      int beatsWhilePaused = session.outputCount("beat");
      int workersWhilePaused = session.outputCount("worker");
      Thread.sleep(400);
      assertEquals(beatsWhilePaused, session.outputCount("beat"), "main is really paused");
      assertEquals(workersWhilePaused, session.outputCount("worker"),
                   "workers freeze with main - they opted into debugging on attach");

      session.resume(threadId);
      session.awaitOutputAbove("beat", beatsWhilePaused); // main runs again
      session.awaitOutputAbove("worker", workersWhilePaused); // workers run again
    }
  }

  /**
   * A thread spawned AFTER startup self-enables debugging when it attaches
   * (THREAD_CREATED runs on the new thread), so a breakpoint inside worker
   * code actually hits. nme runs its whole application loop on a spawned
   * thread, so this is the path its breakpoints take.
   */
  @Test
  @DisplayName("breakpoint in worker code hits")
  public void breakpointInWorkerCodeHits() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("threads")) {
      session.initialize("uncaught", "critical");
      session.setBreakpoints(FixtureSession.EX_SOURCE, new int[]{FixtureSession.EX_WORKER_PRINT_LINE}, null);
      session.configurationDone();

      StoppedEvent hit = session.awaitStopped();
      assertEquals("breakpoint", hit.getBody().getReason());
      int threadId = session.stoppedThread(hit);
      assertEquals(FixtureSession.EX_WORKER_PRINT_LINE, session.topFrame(threadId).getLine(),
                   "stopped on the worker's print line");
    }
  }

  /**
   * The regression that froze real sessions: a local whose @:isVar property
   * getter needs a mutex the PAUSED main thread holds. Rendering must not run
   * the getter — the raw backing value shows and the session stays live.
   */
  @Test
  @DisplayName("rendering never runs getters even against a held lock")
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
      assertEquals("13", session.variable(fields, "danger").getValue(), "raw backing value, getter never invoked");

      int beats = session.outputCount("beat");
      session.resume(threadId);
      session.awaitOutputAbove("beat", beats);
    }
  }
}
