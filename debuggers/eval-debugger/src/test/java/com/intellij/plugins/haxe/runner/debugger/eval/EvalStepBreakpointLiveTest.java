package com.intellij.plugins.haxe.runner.debugger.eval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SourceBreakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.DapConnection;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Breakpoints WIN over in-flight steps — the behaviour every sibling backend
 * has (hxcpp's HandleBreakpoints checks user breakpoints in every step mode;
 * the hashlink adapter resumes with user INT3s armed): a step-out must stop at
 * a breakpoint further down the function, and a step-over must stop at a
 * breakpoint inside the function being stepped over. The eval VM itself honours
 * breakpoints mid-verb (response + breakpointStop notification);
 * these tests pin the adapter's step-emulation loops to the same contract.
 */
public class EvalStepBreakpointLiveTest {
  // EvalStepBp.hx load-bearing lines
  private static final int HELPER_BP_LINE = 11;
  private static final int WORK_START_LINE = 16;
  private static final int HELPER_CALL_LINE = 18;
  private static final int WORK_LATER_BP_LINE = 19;

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
    Assume.assumeTrue("EvalStepBp fixture missing - skipping", Files.isRegularFile(fixtures.resolve("EvalStepBp.hx")));

    adapter = new EvalDebugAdapter(TIMEOUT);
    haxe = new ProcessBuilder("haxe", "-cp", fixtures.toString(), "-main", "EvalStepBp",
                              "-D", "eval-debugger=127.0.0.1:" + adapter.getVmPort(), "--interp")
      .redirectErrorStream(true)
      .start();
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

  private StoppedEvent awaitStopped() throws Exception {
    long deadline = System.currentTimeMillis() + TIMEOUT;
    while (System.currentTimeMillis() < deadline) {
      Event event = dapClient.pollEvent(250);
      if (event instanceof StoppedEvent stopped) {
        return stopped;
      }
    }
    throw new AssertionError("no StoppedEvent within " + TIMEOUT + "ms");
  }

  /** Sets the given breakpoints, runs to the first stop, returns the thread id. */
  private int runToFirstBreakpoint(int... lines) throws Exception {
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue("initialize", request(initialize).isSuccess());
    dapClient.pollEvent(TIMEOUT);
    assertTrue("launch", request(new LaunchRequest()).isSuccess());

    SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
    SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(fixtureDir().resolve("EvalStepBp.hx").toString());
    bpArgs.setSource(source);
    List<SourceBreakpoint> breakpoints = new ArrayList<>();
    for (int line : lines) {
      SourceBreakpoint breakpoint = new SourceBreakpoint();
      breakpoint.setLine(line);
      breakpoints.add(breakpoint);
    }
    bpArgs.setBreakpoints(breakpoints);
    setBreakpoints.setArguments(bpArgs);
    assertTrue("setBreakpoints", request(setBreakpoints).isSuccess());
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent stopped = awaitStopped();
    assertEquals("first stop is the breakpoint", "breakpoint", stopped.getBody().getReason());
    return stopped.getBody().getThreadId();
  }

  private StackFrame topFrame(int threadId) throws Exception {
    StackTraceRequest stackTrace = new StackTraceRequest();
    StackTraceArguments args = new StackTraceArguments();
    args.setThreadId(threadId);
    stackTrace.setArguments(args);
    StackTraceResponse response = (StackTraceResponse)request(stackTrace);
    assertTrue("stackTrace", response.isSuccess());
    return response.getBody().getStackFrames().get(0);
  }

  @Test(timeout = 60_000)
  public void stepOutStopsAtABreakpointFurtherDownTheFunction() throws Exception {
    int threadId = runToFirstBreakpoint(WORK_START_LINE, WORK_LATER_BP_LINE);
    assertEquals("parked at the top of work", WORK_START_LINE, topFrame(threadId).getLine());

    StepOutRequest stepOut = new StepOutRequest();
    StepOutArguments args = new StepOutArguments();
    args.setThreadId(threadId);
    stepOut.setArguments(args);
    assertTrue("stepOut", request(stepOut).isSuccess());

    StoppedEvent stopped = awaitStopped();
    assertEquals("step-out yields to the breakpoint below", "breakpoint", stopped.getBody().getReason());
    assertEquals("stopped ON that breakpoint's line", WORK_LATER_BP_LINE,
                 topFrame(stopped.getBody().getThreadId()).getLine());
  }

  @Test(timeout = 60_000)
  public void stepOverStopsAtABreakpointInsideTheSteppedOverCall() throws Exception {
    int threadId = runToFirstBreakpoint(HELPER_CALL_LINE, HELPER_BP_LINE);
    assertEquals("parked on the helper() call", HELPER_CALL_LINE, topFrame(threadId).getLine());

    NextRequest next = new NextRequest();
    NextArguments args = new NextArguments();
    args.setThreadId(threadId);
    next.setArguments(args);
    assertTrue("next", request(next).isSuccess());

    StoppedEvent stopped = awaitStopped();
    assertEquals("step-over yields to the breakpoint inside the callee", "breakpoint",
                 stopped.getBody().getReason());
    assertEquals("stopped ON the callee's breakpoint line", HELPER_BP_LINE,
                 topFrame(stopped.getBody().getThreadId()).getLine());
  }

  @Test(timeout = 60_000)
  public void steppingOffABreakpointLineDoesNotInstaStopOnItsOwnBreakpoint() throws Exception {
    int threadId = runToFirstBreakpoint(WORK_START_LINE);
    assertEquals("parked at the top of work", WORK_START_LINE, topFrame(threadId).getLine());

    NextRequest next = new NextRequest();
    NextArguments args = new NextArguments();
    args.setThreadId(threadId);
    next.setArguments(args);
    assertTrue("next", request(next).isSuccess());

    StoppedEvent stopped = awaitStopped();
    assertEquals("a normal step off the parked breakpoint line", "step", stopped.getBody().getReason());
    // the exact landing line wobbles by one (the VM sometimes reports a
    // trailing same-line sub-position first); the contract under test is only
    // that the step LEFT the line instead of insta-stopping on its own bp
    int landedLine = topFrame(stopped.getBody().getThreadId()).getLine();
    assertTrue("landed past the breakpoint line (was " + landedLine + ")",
               landedLine > WORK_START_LINE);
  }
}
