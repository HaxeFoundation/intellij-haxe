package com.intellij.plugins.haxe.runner.debugger.eval;

import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SourceBreakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.DapConnection;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.IntFunction;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Regression for the uncaught-exception STEP lock-up: the user stepped onto a
 * throw and pressed step over / step in, and the session wedged (the IDE had to
 * be killed). The eval VM does not raise an exceptionStop for a step (only for
 * continue); it unwinds the stack and leaves the process pending, so the
 * adapter's step-coalescing loops used to loop on / error out of the vanished
 * stack. The fix resumes the VM off the uncaught exception, ending the session
 * cleanly. These tests would HANG (and hit the method timeout) if it regressed.
 */
public class EvalStepExceptionLiveTest extends EvalLiveTestBase {
  @Override
  protected String fixtureMain() {
    return "EvalThrow";
  }

  private static final int THROW_LINE = 9;


  /** Runs to the breakpoint on the throw line and returns the stopped thread id. */
  private int stopOnThrowLine() throws Exception {
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue("initialize", request(initialize).isSuccess());
    dapClient.pollEvent(TIMEOUT);
    launch();

    SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
    SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(fixtureDir().resolve("EvalThrow.hx").toString());
    bpArgs.setSource(source);
    SourceBreakpoint breakpoint = new SourceBreakpoint();
    breakpoint.setLine(THROW_LINE);
    bpArgs.setBreakpoints(List.of(breakpoint));
    setBreakpoints.setArguments(bpArgs);
    assertTrue("setBreakpoints", request(setBreakpoints).isSuccess());
    configurationDone();

    StoppedEvent stopped = awaitEvent(StoppedEvent.class);
    return stopped.getBody().getThreadId();
  }

  @Test(timeout = 60_000)
  public void steppingOverAnUncaughtThrowTerminatesWithoutStalling() throws Exception {
    stepUntilTerminated(EvalLiveTestBase::nextRequest);
  }

  @Test(timeout = 60_000)
  public void steppingIntoAnUncaughtThrowTerminatesWithoutStalling() throws Exception {
    stepUntilTerminated(EvalLiveTestBase::stepInRequest);
  }

  /**
   * Steps over the throw and keeps stepping until the session terminates. The
   * eval VM's behaviour here varies by version — 4.3.x wedges then unwinds
   * immediately; 4.1/4.2 and the 5 preview route `throw "x"` through the
   * haxe.Exception.thrown/ValueException WRAPPER (real steppable std code the
   * walk legitimately visits before the throw executes); the 5 preview's VM
   * additionally survives the recovery resume as a zombie that the adapter
   * must detect and end. In EVERY case each press must be answered PROMPTLY
   * (no 10s VM-timeout stall) and the session must wind down within the
   * budget. Before the fix this wedged (the adapter looped on / blocked
   * against the vanished stack), which is what forced the user to kill the IDE.
   */
  private void stepUntilTerminated(IntFunction<Request> stepFor) throws Exception {
    int threadId = stopOnThrowLine();
    for (int i = 0; i < 10; i++) {
      long before = System.currentTimeMillis();
      Response step = request(stepFor.apply(threadId));
      long elapsed = System.currentTimeMillis() - before;
      assertTrue("step #" + i + " was answered (success), was: " + step.getMessage(), step.isSuccess());
      assertTrue("step #" + i + " answered promptly, no stall (was " + elapsed + "ms)", elapsed < 8_000);

      // the terminate may race ahead of / lag behind the step response; poll a
      // short window for it, or for a fresh stop meaning another step is needed
      long deadline = System.currentTimeMillis() + 4_000;
      boolean stoppedAgain = false;
      while (System.currentTimeMillis() < deadline) {
        Event event = dapClient.pollEvent(200);
        if (event instanceof TerminatedEvent) {
          // usually the process dies with the session (natural death); on the
          // haxe 5 preview the VM survives as a zombie the adapter detected —
          // the terminated event is the contract, the IDE kills the process
          haxe.waitFor(3, TimeUnit.SECONDS);
          return;
        }
        if (event instanceof StoppedEvent) {
          threadId = ((StoppedEvent)event).getBody().getThreadId();
          stoppedAgain = true;
          break;
        }
      }
      if (!stoppedAgain && haxe.waitFor(2, TimeUnit.SECONDS)) {
        return; // process already exited; the terminated event simply raced us
      }
    }
    // Still stopped after the budget: on haxe 4.1/4.2 and the 5 preview,
    // `throw "x"` routes through steppable wrapper AND uncaught-printer std
    // code, so a step-into walk can visit more stops than any fixed budget.
    // That is not a wedge — the invariant is that the session stays
    // CONTROLLABLE: a continue from anywhere in that walk must end it.
    ContinueRequest resume = continueRequest(threadId);

    long before = System.currentTimeMillis();
    Response resumed = request(resume);
    long elapsed = System.currentTimeMillis() - before;
    assertTrue("continue after the walk answered promptly (was " + elapsed + "ms)", elapsed < 8_000);
    assertTrue("continue after the walk answered (success), was: " + resumed.getMessage(),
               resumed.isSuccess());

    awaitTerminated();
    haxe.waitFor(3, TimeUnit.SECONDS);
  }

  // ---- the EXCEPTION-STOP flavour: uncaught filter armed, the VM stops with
  // ---- reason "exception", and the user then steps / resumes FROM that stop
  // ---- (the exact IDE flow that stalled the session for ~50s per press)

  /** Arms the uncaught filter, runs into the throw, returns the exception stop. */
  private StoppedEvent runIntoExceptionStop() throws Exception {
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue("initialize", request(initialize).isSuccess());
    dapClient.pollEvent(TIMEOUT);
    launch();

    SetExceptionBreakpointsRequest exceptions = exceptionBreakpointsRequest(List.of("uncaught"));
    assertTrue("setExceptionBreakpoints", request(exceptions).isSuccess());
    configurationDone();

    StoppedEvent stopped = awaitEvent(StoppedEvent.class);
    assertTrue("stopped for the exception, was: " + stopped.getBody().getReason(),
               "exception".equals(stopped.getBody().getReason()));
    return stopped;
  }

  /**
   * From the exception stop, every step press must be answered PROMPTLY (the
   * reported stall was ~50s per press: a coalescing loop spinning its 4096
   * cap against a VM that cannot advance) and the session must wind down to
   * terminated within a few presses. Mirrors the IDE: after each stop it also
   * asks for threads, like reportStopped does.
   */
  @Test(timeout = 60_000)
  public void steppingAtTheExceptionStopAnswersPromptlyAndTerminates() throws Exception {
    int threadId = runIntoExceptionStop().getBody().getThreadId();
    hydrateStopLikeTheIde(threadId, "exception stop");

    for (int press = 0; press < 6; press++) {
      long before = System.currentTimeMillis();
      Response step = request(stepInRequest(threadId));
      long elapsed = System.currentTimeMillis() - before;
      assertTrue("step press #" + press + " answered promptly, no stall (was " + elapsed + "ms)",
                 elapsed < 8_000);
      assertTrue("step press #" + press + " answered (success)", step.isSuccess());

      long deadline = System.currentTimeMillis() + 4_000;
      boolean stoppedAgain = false;
      while (System.currentTimeMillis() < deadline) {
        Event event = dapClient.pollEvent(200);
        if (event instanceof TerminatedEvent) {
          assertTrue("haxe exited", haxe.waitFor(TIMEOUT, TimeUnit.MILLISECONDS));
          return;
        }
        if (event instanceof StoppedEvent stopped) {
          threadId = stopped.getBody().getThreadId();
          hydrateStopLikeTheIde(threadId, "press #" + press);
          stoppedAgain = true;
          break;
        }
      }
      if (!stoppedAgain && haxe.waitFor(2, TimeUnit.SECONDS)) {
        return; // exited; terminated event raced us
      }
    }
    throw new AssertionError("stepping at the exception stop never terminated the session");
  }

  /**
   * Everything the IDE requests on a stop (reportStopped + the variables
   * view): threads, stackTrace, scopes of the top frame, variables of every
   * scope. Each must answer promptly — a single wedged one burns a 10s VM
   * timeout, and a handful queued back-to-back is the reported 25-50s freeze.
   */
  private void hydrateStopLikeTheIde(int threadId, String label) throws Exception {
    long before = System.currentTimeMillis();
    assertTrue(label + ": threads answered", request(new ThreadsRequest()).isSuccess());

    StackTraceRequest stackTrace = stackTraceRequest(threadId);
    StackTraceResponse stResponse = (StackTraceResponse)request(stackTrace);
    assertTrue(label + ": stackTrace answered", stResponse.isSuccess());

    List<StackFrame> frames = stResponse.getBody().getStackFrames();
    if (!frames.isEmpty()) {
      ScopesRequest scopes = scopesRequest(frames.get(0).getId());

      Response scopesResponse = request(scopes);

      // a mid-unwind frame may legitimately have nothing to show, but the
      // request must come back rather than sit on the VM timeout
      if (scopesResponse.isSuccess()) {
        for (Scope scope : ((ScopesResponse)scopesResponse).getBody().getScopes()) {
          VariablesRequest variables = variablesRequest(scope.getVariablesReference());
          request(variables); // success optional; promptness is the contract
        }
      }
      // watches / inline values: the IDE re-EVALUATES these on every stop —
      // a wedged evaluate burns a full VM timeout per watch, which is the
      // multi-10s freeze shape the user reported
      EvaluateRequest watch = evaluateRequest(frames.get(0).getId(), "1 + 1");
      request(watch); // success optional; promptness is the contract
    }
    long elapsed = System.currentTimeMillis() - before;
    assertTrue(label + ": full stop hydration stayed prompt (was " + elapsed + "ms)", elapsed < 8_000);
  }

  /**
   * The user's exact report: step at the exception stop a couple of times,
   * THEN resume — the resume must be answered promptly and end the session,
   * not sit wedged behind stalled step loops until timeouts kill it.
   */
  @Test(timeout = 60_000)
  public void resumeAfterSteppingAtTheExceptionStopTerminatesPromptly() throws Exception {
    int threadId = runIntoExceptionStop().getBody().getThreadId();

    for (int press = 0; press < 2; press++) {
      long before = System.currentTimeMillis();
      Response step = request(stepInRequest(threadId));
      long elapsed = System.currentTimeMillis() - before;
      assertTrue("step press #" + press + " answered promptly (was " + elapsed + "ms)", elapsed < 8_000);
      assertTrue("step press #" + press + " answered (success)", step.isSuccess());
      Event event = dapClient.pollEvent(1_500);
      if (event instanceof TerminatedEvent) {
        return; // wound down before we even resumed - fine
      }
      if (event instanceof StoppedEvent stopped) {
        threadId = stopped.getBody().getThreadId();
      }
    }

    ContinueRequest resume = continueRequest(threadId);
    long before = System.currentTimeMillis();
    Response resumed = request(resume);
    long elapsed = System.currentTimeMillis() - before;
    assertTrue("resume answered promptly, no stall (was " + elapsed + "ms)", elapsed < 8_000);
    assertTrue("resume answered (success)", resumed.isSuccess());

    awaitTerminated();
    assertTrue("haxe exited after the resume", haxe.waitFor(TIMEOUT, TimeUnit.MILLISECONDS));
  }
}
