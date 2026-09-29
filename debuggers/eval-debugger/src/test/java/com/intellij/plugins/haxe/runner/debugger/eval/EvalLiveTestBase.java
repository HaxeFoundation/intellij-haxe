package com.intellij.plugins.haxe.runner.debugger.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.TerminatedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.ScopesResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.StackTraceResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.VariablesResponse;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base for the live eval tests. Each drives the REAL adapter against a haxe
 * {@code --interp} debuggee built from one fixture: subclasses name the
 * fixture, everything else — VM port handshake, the DAP socket pair, and the
 * teardown that kills a debuggee still holding the port — is identical.
 *
 * Tests SKIP rather than fail when haxe or the fixture is unavailable, so a
 * machine without a haxe toolchain still runs the rest of the suite.
 */
public abstract class EvalLiveTestBase {
  protected static final long TIMEOUT = 15_000;

  protected EvalDebugAdapter adapter;
  protected DapClient dapClient;
  /** The debuggee: tests wait on its exit or read what it printed. */
  protected Process haxe;

  private ServerSocket dapListener;

  /** The fixture's main class; {@code <name>.hx} must sit in the eval fixture dir. */
  protected abstract String fixtureMain();

  @BeforeEach
  public void wire() throws IOException {
    Assumptions.assumeTrue(haxeOnPath(), "haxe not on PATH - skipping");

    Path fixtures = fixtureDir();
    Assumptions.assumeTrue(Files.isRegularFile(fixtures.resolve(fixtureMain() + ".hx")), fixtureMain() + " fixture missing - skipping");

    adapter = new EvalDebugAdapter(TIMEOUT);
    haxe = new ProcessBuilder("haxe", "-cp", fixtures.toString(), "-main", fixtureMain(),
                              "-D", "eval-debugger=127.0.0.1:" + adapter.getVmPort(), "--interp")
      .redirectErrorStream(true)
      .start();

    dapListener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    Socket clientSide = new Socket(InetAddress.getLoopbackAddress(), dapListener.getLocalPort());

    adapter.start(new DapConnection(dapListener.accept()));
    dapClient = new DapClient(new DapConnection(clientSide));
  }

  @AfterEach
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

  protected Response request(Request request) throws Exception {
    return dapClient.sendRequest(request, TIMEOUT);
  }

  /** launch + assert success. */
  protected void launch() throws Exception {
    assertTrue(request(new LaunchRequest()).isSuccess(), "launch");
  }

  /** configurationDone + assert success: the step that lets the debuggee run. */
  protected void configurationDone() throws Exception {
    assertTrue(request(new ConfigurationDoneRequest()).isSuccess(), "configurationDone");
  }

  /** The session's opening handshake: initialize (with the IDE's adapter id) + the initialized event. */
  protected void initialize() throws Exception {
    InitializeRequest initialize = new InitializeRequest();
    InitializeRequestArguments arguments = new InitializeRequestArguments();
    arguments.setAdapterID("intellij-haxe-eval");
    initialize.setArguments(arguments);
    assertTrue(request(initialize).isSuccess(), "initialize");
    assertNotNull(dapClient.pollEvent(TIMEOUT), "initialized event");
  }

  /** setBreakpoints on a file in the fixture dir + assert success. */
  protected void setBreakpoints(String fixtureFile, int... lines) throws Exception {
    SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
    SetBreakpointsArguments arguments = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(fixtureDir().resolve(fixtureFile).toString());
    arguments.setSource(source);
    List<SourceBreakpoint> breakpoints = new ArrayList<>();
    for (int line : lines) {
      SourceBreakpoint breakpoint = new SourceBreakpoint();
      breakpoint.setLine(line);
      breakpoints.add(breakpoint);
    }
    arguments.setBreakpoints(breakpoints);
    setBreakpoints.setArguments(arguments);
    assertTrue(request(setBreakpoints).isSuccess(), "setBreakpoints");
  }

  /** The whole opening dance to the first breakpoint stop. */
  protected StoppedEvent runToBreakpoint(String fixtureFile, int... lines) throws Exception {
    initialize();
    launch();
    setBreakpoints(fixtureFile, lines);
    configurationDone();
    StoppedEvent stopped = awaitStopped();
    assertEquals("breakpoint", stopped.getBody().getReason(), "stopped by the breakpoint");
    return stopped;
  }

  /** The exception-test opening: initialize, launch, arm the filters, configurationDone. */
  protected void startSession(List<String> exceptionFilters) throws Exception {
    initialize();
    launch();
    assertTrue(request(exceptionBreakpointsRequest(exceptionFilters)).isSuccess(), "setExceptionBreakpoints");
    configurationDone();
  }

  /** The next event of {@code type}; fails rather than hanging when none arrives. */
  protected <T extends Event> T awaitEvent(Class<T> type) throws Exception {
    long deadline = System.currentTimeMillis() + TIMEOUT;
    while (System.currentTimeMillis() < deadline) {
      Event event = dapClient.pollEvent(250);
      if (type.isInstance(event)) {
        return type.cast(event);
      }
    }
    throw new AssertionError("no " + type.getSimpleName() + " within " + TIMEOUT + "ms");
  }

  protected StoppedEvent awaitStopped() throws Exception {
    return awaitEvent(StoppedEvent.class);
  }

  protected TerminatedEvent awaitTerminated() throws Exception {
    return awaitEvent(TerminatedEvent.class);
  }

  /** The stopped thread's top stack frame. */
  protected StackFrame topFrame(int threadId) throws Exception {
    StackTraceResponse response = (StackTraceResponse)request(stackTraceRequest(threadId));
    assertTrue(response.isSuccess(), "stackTrace");
    List<StackFrame> frames = response.getBody().getStackFrames();
    assertFalse(frames.isEmpty(), "frames at the stop");
    return frames.get(0);
  }

  /** A local found by scanning every scope of a frame, with the scope reference it lives under. */
  protected record FoundLocal(Variable variable, int scopeReference) {}

  /** Scans every scope of the frame for the named local; fails when absent. */
  protected FoundLocal findLocal(int frameId, String name) throws Exception {
    ScopesResponse scopes = (ScopesResponse)request(scopesRequest(frameId));
    assertTrue(scopes.isSuccess(), "scopes");
    for (Scope scope : scopes.getBody().getScopes()) {
      VariablesResponse variables = (VariablesResponse)request(variablesRequest(scope.getVariablesReference()));
      assertTrue(variables.isSuccess(), "variables of scope " + scope.getName());
      for (Variable variable : variables.getBody().getVariables()) {
        if (name.equals(variable.getName())) {
          return new FoundLocal(variable, scope.getVariablesReference());
        }
      }
    }
    throw new AssertionError("no local '" + name + "' in any scope of frame " + frameId);
  }

  // --- request factories: the build/set/assign dance, named once each -------

  protected static StackTraceRequest stackTraceRequest(int threadId) {
    StackTraceRequest request = new StackTraceRequest();
    StackTraceArguments arguments = new StackTraceArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return request;
  }

  protected static ScopesRequest scopesRequest(int frameId) {
    ScopesRequest request = new ScopesRequest();
    ScopesArguments arguments = new ScopesArguments();
    arguments.setFrameId(frameId);
    request.setArguments(arguments);
    return request;
  }

  protected static VariablesRequest variablesRequest(int variablesReference) {
    VariablesRequest request = new VariablesRequest();
    VariablesArguments arguments = new VariablesArguments();
    arguments.setVariablesReference(variablesReference);
    request.setArguments(arguments);
    return request;
  }

  protected static ContinueRequest continueRequest(int threadId) {
    ContinueRequest request = new ContinueRequest();
    ContinueArguments arguments = new ContinueArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return request;
  }

  protected static NextRequest nextRequest(int threadId) {
    NextRequest request = new NextRequest();
    NextArguments arguments = new NextArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return request;
  }

  protected static StepInRequest stepInRequest(int threadId) {
    StepInRequest request = new StepInRequest();
    StepInArguments arguments = new StepInArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return request;
  }

  protected static StepOutRequest stepOutRequest(int threadId) {
    StepOutRequest request = new StepOutRequest();
    StepOutArguments arguments = new StepOutArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return request;
  }

  protected static EvaluateRequest evaluateRequest(int frameId, String expression) {
    EvaluateRequest request = new EvaluateRequest();
    EvaluateArguments arguments = new EvaluateArguments();
    arguments.setExpression(expression);
    arguments.setFrameId(frameId);
    request.setArguments(arguments);
    return request;
  }

  protected static SetVariableRequest setVariableRequest(int variablesReference, String name, String value) {
    SetVariableRequest request = new SetVariableRequest();
    SetVariableArguments arguments = new SetVariableArguments();
    arguments.setVariablesReference(variablesReference);
    arguments.setName(name);
    arguments.setValue(value);
    request.setArguments(arguments);
    return request;
  }

  protected static SetExceptionBreakpointsRequest exceptionBreakpointsRequest(List<String> filters) {
    SetExceptionBreakpointsRequest request = new SetExceptionBreakpointsRequest();
    SetExceptionBreakpointsArguments arguments = new SetExceptionBreakpointsArguments();
    arguments.setFilters(filters);
    request.setArguments(arguments);
    return request;
  }

  protected static boolean haxeOnPath() {
    try {
      Process probe = new ProcessBuilder("haxe", "--version").redirectErrorStream(true).start();
      return probe.waitFor(10, TimeUnit.SECONDS) && probe.exitValue() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  protected static Path fixtureDir() {
    String fromGradle = System.getProperty("eval.fixture.src.dir");
    return fromGradle != null ? Path.of(fromGradle) : Path.of("test-fixtures").toAbsolutePath();
  }
}
