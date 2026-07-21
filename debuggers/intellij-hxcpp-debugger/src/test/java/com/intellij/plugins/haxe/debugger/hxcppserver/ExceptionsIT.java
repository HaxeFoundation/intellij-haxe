package com.intellij.plugins.haxe.debugger.hxcppserver;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * The M6 exception matrix against the scenario fixture: an uncatchable throw
 * stops AT the throw site before unwinding; a caught throw never stops; null
 * access stops as a critical error EVEN inside try/catch (a runtime property
 * under an attached debugger); disabled filters resume silently.
 */
public class ExceptionsIT {

  @Test
  public void anUncaughtThrowStopsAtTheThrowSite() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("uncaught")) {
      session.initialize("uncaught", "critical");
      session.configurationDone();

      StoppedEvent stopped = session.awaitStopped();
      assertEquals("exception", stopped.getBody().getReason());
      assertEquals("Uncaught exception", stopped.getBody().getDescription());
      assertTrue("the runtime message names the thrown value",
                 stopped.getBody().getText().contains("boom-uncaught"));
      int threadId = session.stoppedThread(stopped);
      assertEquals("stopped AT the throw, before unwinding",
                   FixtureSession.EX_THROW_LINE, session.topFrame(threadId).getLine());

      // locals at the throw site are inspectable
      assertEquals("42", session.evaluate("marker", session.topFrame(threadId).getId()));

      ExceptionInfoRequest info = new ExceptionInfoRequest();
      ExceptionInfoArguments arguments = new ExceptionInfoArguments();
      arguments.setThreadId(threadId);
      info.setArguments(arguments);
      ExceptionInfoResponse response = (ExceptionInfoResponse)session.request(info);
      assertTrue("exceptionInfo", response.isSuccess());
      assertEquals("unhandled", response.getBody().getBreakMode());
      assertTrue(response.getBody().getDescription().contains("boom-uncaught"));

      // resuming an uncatchable throw unwinds and terminates normally
      session.resume(threadId);
      assertNotEquals("the program terminated with the error", 0, session.awaitExit());
    }
  }

  @Test
  public void aCaughtThrowNeverStops() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("caught")) {
      session.initialize("uncaught", "critical");
      session.configurationDone();
      assertEquals(0, session.awaitExit());
      assertTrue("the catch ran", session.outputSnapshot().contains("caught:boom-caught"));
      assertTrue("the program completed", session.outputSnapshot().contains("ex-end"));
    }
  }

  @Test
  public void aNullAccessStopsAsACriticalErrorEvenInsideTryCatch() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("caught-null")) {
      session.initialize("uncaught", "critical");
      session.configurationDone();

      StoppedEvent stopped = session.awaitStopped();
      assertEquals("exception", stopped.getBody().getReason());
      assertEquals("Critical error", stopped.getBody().getDescription());
      assertTrue(stopped.getBody().getText().contains("Null"));
      assertEquals(FixtureSession.EX_CAUGHT_NULL_LINE,
                   session.topFrame(session.stoppedThread(stopped)).getLine());
    }
  }

  /**
   * The "thrown" filter: a class-function breakpoint on haxe.Exception.new
   * stops where an Exception (or subclass — here AppError) is CONSTRUCTED,
   * i.e. at the throw expression, even though this throw is caught. The stop
   * text names the concrete class and the message.
   */
  @Test
  public void theThrownFilterStopsAtACaughtExceptionConstruction() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("typedthrow")) {
      session.initialize("uncaught", "critical", "thrown");
      session.configurationDone();

      StoppedEvent stopped = session.awaitStopped();
      assertEquals("exception", stopped.getBody().getReason());
      assertEquals("Thrown exception", stopped.getBody().getDescription());
      assertTrue(stopped.getBody().getText(), stopped.getBody().getText().contains("AppError"));
      assertTrue(stopped.getBody().getText().contains("kaboom"));

      // the Exception ctor frames are trimmed: the TOP frame is the throw site
      var top = session.topFrame(session.stoppedThread(stopped));
      assertEquals("TypedThrow.run", top.getName());
      assertEquals(FixtureSession.TYPED_THROW_LINE, top.getLine());

      session.resume(session.stoppedThread(stopped));
      assertEquals(0, session.awaitExit());
      assertTrue("the catch still ran", session.outputSnapshot().contains("caught-app:kaboom"));
    }
  }

  /**
   * Typed exception filters: AppError extends haxe.Exception WITHOUT declaring
   * a constructor (no AppError.new frame exists), so matching must come from
   * the class chain read off `this` at the Exception.new hook. The "thrown"
   * filter is OFF — only the typed filter can cause this stop.
   */
  @Test
  public void aTypedFilterStopsItsClassEvenWithAnInheritedConstructor() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("typedthrow")) {
      session.initialize("uncaught", "critical");
      session.setExceptionFilters(java.util.List.of("uncaught", "critical"), java.util.List.of("AppError"));
      session.configurationDone();

      StoppedEvent stopped = session.awaitStopped();
      assertEquals("exception", stopped.getBody().getReason());
      assertTrue(stopped.getBody().getText(), stopped.getBody().getText().contains("AppError: kaboom"));

      session.resume(session.stoppedThread(stopped));
      assertEquals(0, session.awaitExit());
    }
  }

  @Test
  public void aTypedFilterForAnotherClassDoesNotStop() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("typedthrow")) {
      session.initialize("uncaught", "critical");
      session.setExceptionFilters(java.util.List.of("uncaught", "critical"), java.util.List.of("SomeOtherError"));
      session.configurationDone();
      assertEquals("the non-matching construction was resumed silently", 0, session.awaitExit());
      assertTrue("the catch ran", session.outputSnapshot().contains("caught-app:kaboom"));
    }
  }

  /**
   * The user-reported bug's server half: enabling the "thrown" filter DURING a
   * live session (not just at startup) must take effect. The debuggee loops
   * throwing+catching an AppError; the session starts with thrown OFF (runs
   * freely), then setExceptionBreakpoints turns it on and the next throw stops.
   */
  @Test
  public void enablingTheThrownFilterMidSessionTakesEffect() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("throwloop")) {
      session.initialize("uncaught", "critical"); // thrown OFF
      session.configurationDone();
      session.awaitOutputAbove("beat", -1); // looping, not stopped

      session.setExceptionFilters(java.util.List.of("uncaught", "critical", "thrown"), java.util.List.of());
      StoppedEvent stopped = session.awaitStopped();
      assertEquals("exception", stopped.getBody().getReason());
      assertTrue(stopped.getBody().getText(), stopped.getBody().getText().contains("AppError"));
      session.resume(session.stoppedThread(stopped));
    }
  }

  /**
   * Regression guard: ordinary LINE breakpoints inside a try AND inside its
   * catch fire like any other breakpoint. (The exception FILTERS are a
   * separate mechanism — "break on all/caught exceptions" is not supportable,
   * see README gotcha 15 — but a line breakpoint on such a line always works.)
   */
  @Test
  public void lineBreakpointsInsideTryAndCatchFire() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("caught")) {
      session.initialize("uncaught", "critical");
      // line 26 = the throw inside try; line 28 = the handler inside catch
      session.setBreakpoints(FixtureSession.EX_SOURCE, new int[]{26, 28}, null);
      session.configurationDone();

      StoppedEvent insideTry = session.awaitStopped();
      assertEquals("breakpoint", insideTry.getBody().getReason());
      assertEquals(26, session.topFrame(session.stoppedThread(insideTry)).getLine());
      session.resume(session.stoppedThread(insideTry));

      StoppedEvent insideCatch = session.awaitStopped();
      assertEquals("breakpoint", insideCatch.getBody().getReason());
      assertEquals(28, session.topFrame(session.stoppedThread(insideCatch)).getLine());
      session.resume(session.stoppedThread(insideCatch));
      assertEquals("program completes normally", 0, session.awaitExit());
    }
  }

  @Test
  public void disabledFiltersResumeAnUncaughtThrowSilently() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("uncaught")) {
      session.initialize(/* all filters off */);
      session.configurationDone();
      // no stop: the program unwinds and dies on its own
      assertNotEquals(0, session.awaitExit());
      assertTrue("it reached the throw", session.outputSnapshot().contains("ex-start:uncaught"));
    }
  }
}
