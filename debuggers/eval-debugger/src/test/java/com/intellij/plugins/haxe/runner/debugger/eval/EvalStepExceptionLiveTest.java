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
public class EvalStepExceptionLiveTest {
  private static final int THROW_LINE = 9;
  private static final long TIMEOUT = 15_000;

  private EvalDebugAdapter adapter;
  private DapClient dapClient;
  private ServerSocket dapListener;
  private Process haxe;

  private static boolean haxeOnPath() {
    try {
      Process probe = new ProcessBuilder("haxe", "--version").redirectErrorStream(true).start();
      return probe.waitFor(10, TimeUnit.SECONDS) && probe.exitValue() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  private static Path fixtureDir() {
    String fromGradle = System.getProperty("eval.fixture.src.dir");
    return fromGradle != null ? Path.of(fromGradle) : Path.of("test-fixtures").toAbsolutePath();
  }

  @Before
  public void wire() throws IOException {
    Assume.assumeTrue("haxe not on PATH - skipping", haxeOnPath());
    Path fixtures = fixtureDir();
    Assume.assumeTrue("throw fixture missing - skipping", Files.isRegularFile(fixtures.resolve("EvalThrow.hx")));

    adapter = new EvalDebugAdapter(TIMEOUT);
    haxe = new ProcessBuilder("haxe", "-cp", fixtures.toString(), "-main", "EvalThrow",
                              "-D", "eval-debugger=127.0.0.1:" + adapter.getVmPort(), "--interp")
      .redirectErrorStream(true).start();
    dapListener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    Socket clientSide = new Socket(InetAddress.getLoopbackAddress(), dapListener.getLocalPort());
    adapter.start(new DapConnection(dapListener.accept()));
    dapClient = new DapClient(new DapConnection(clientSide));
  }

  @After
  public void tearDown() throws Exception {
    if (dapClient != null) {
      try {
        dapClient.close();
      } catch (IOException ignored) {
      }
    }
    if (adapter != null) {
      adapter.close();
    }
    if (haxe != null && !haxe.waitFor(3, TimeUnit.SECONDS)) {
      haxe.descendants().forEach(ProcessHandle::destroyForcibly);
      haxe.destroyForcibly();
      haxe.waitFor(5, TimeUnit.SECONDS);
    }
    if (dapListener != null) {
      dapListener.close();
    }
  }

  private Response request(Request request) throws Exception {
    return dapClient.sendRequest(request, TIMEOUT);
  }

  private <T extends Event> T awaitEvent(Class<T> type) throws Exception {
    long deadline = System.currentTimeMillis() + TIMEOUT;
    while (System.currentTimeMillis() < deadline) {
      Event event = dapClient.pollEvent(250);
      if (type.isInstance(event)) {
        return type.cast(event);
      }
    }
    throw new AssertionError("no " + type.getSimpleName() + " within " + TIMEOUT + "ms");
  }

  /** Runs to the breakpoint on the throw line and returns the stopped thread id. */
  private int stopOnThrowLine() throws Exception {
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue("initialize", request(initialize).isSuccess());
    dapClient.pollEvent(TIMEOUT);
    assertTrue("launch", request(new LaunchRequest()).isSuccess());

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
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent stopped = awaitEvent(StoppedEvent.class);
    return stopped.getBody().getThreadId();
  }

  @Test(timeout = 60_000)
  public void steppingOverAnUncaughtThrowTerminatesWithoutStalling() throws Exception {
    stepUntilTerminated(this::nextRequestFor);
  }

  @Test(timeout = 60_000)
  public void steppingIntoAnUncaughtThrowTerminatesWithoutStalling() throws Exception {
    stepUntilTerminated(this::stepInRequestFor);
  }

  private Request nextRequestFor(int threadId) {
    NextRequest next = new NextRequest();
    NextArguments args = new NextArguments();
    args.setThreadId(threadId);
    next.setArguments(args);
    return next;
  }

  private Request stepInRequestFor(int threadId) {
    StepInRequest stepIn = new StepInRequest();
    StepInArguments args = new StepInArguments();
    args.setThreadId(threadId);
    stepIn.setArguments(args);
    return stepIn;
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
      // short window for it, or for a fresh stop meaning we must step again
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
    ContinueRequest resume = new ContinueRequest();
    ContinueArguments cArgs = new ContinueArguments();
    cArgs.setThreadId(threadId);
    resume.setArguments(cArgs);
    long before = System.currentTimeMillis();
    Response resumed = request(resume);
    long elapsed = System.currentTimeMillis() - before;
    assertTrue("continue after the walk answered promptly (was " + elapsed + "ms)", elapsed < 8_000);
    assertTrue("continue after the walk answered (success), was: " + resumed.getMessage(),
               resumed.isSuccess());
    awaitEvent(TerminatedEvent.class);
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
    assertTrue("launch", request(new LaunchRequest()).isSuccess());

    SetExceptionBreakpointsRequest exceptions = new SetExceptionBreakpointsRequest();
    SetExceptionBreakpointsArguments exArgs = new SetExceptionBreakpointsArguments();
    exArgs.setFilters(List.of("uncaught"));
    exceptions.setArguments(exArgs);
    assertTrue("setExceptionBreakpoints", request(exceptions).isSuccess());
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());

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
      Response step = request(stepInRequestFor(threadId));
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

    StackTraceRequest stackTrace = new StackTraceRequest();
    StackTraceArguments stArgs = new StackTraceArguments();
    stArgs.setThreadId(threadId);
    stackTrace.setArguments(stArgs);
    StackTraceResponse stResponse = (StackTraceResponse)request(stackTrace);
    assertTrue(label + ": stackTrace answered", stResponse.isSuccess());

    List<StackFrame> frames = stResponse.getBody().getStackFrames();
    if (!frames.isEmpty()) {
      ScopesRequest scopes = new ScopesRequest();
      ScopesArguments scArgs = new ScopesArguments();
      scArgs.setFrameId(frames.get(0).getId());
      scopes.setArguments(scArgs);
      Response scopesResponse = request(scopes);
      // a mid-unwind frame may legitimately have nothing to show, but the
      // request must come back rather than sit on the VM timeout
      if (scopesResponse.isSuccess()) {
        for (Scope scope : ((ScopesResponse)scopesResponse).getBody().getScopes()) {
          VariablesRequest variables = new VariablesRequest();
          VariablesArguments vArgs = new VariablesArguments();
          vArgs.setVariablesReference(scope.getVariablesReference());
          variables.setArguments(vArgs);
          request(variables); // success optional; promptness is the contract
        }
      }
      // watches / inline values: the IDE re-EVALUATES these on every stop —
      // a wedged evaluate burns a full VM timeout per watch, which is the
      // multi-10s freeze shape the user reported
      EvaluateRequest watch = new EvaluateRequest();
      EvaluateArguments evArgs = new EvaluateArguments();
      evArgs.setExpression("1 + 1");
      evArgs.setFrameId(frames.get(0).getId());
      watch.setArguments(evArgs);
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
      Response step = request(stepInRequestFor(threadId));
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

    ContinueRequest resume = new ContinueRequest();
    ContinueArguments cArgs = new ContinueArguments();
    cArgs.setThreadId(threadId);
    resume.setArguments(cArgs);
    long before = System.currentTimeMillis();
    Response resumed = request(resume);
    long elapsed = System.currentTimeMillis() - before;
    assertTrue("resume answered promptly, no stall (was " + elapsed + "ms)", elapsed < 8_000);
    assertTrue("resume answered (success)", resumed.isSuccess());

    awaitEvent(TerminatedEvent.class);
    assertTrue("haxe exited after the resume", haxe.waitFor(TIMEOUT, TimeUnit.MILLISECONDS));
  }
}
