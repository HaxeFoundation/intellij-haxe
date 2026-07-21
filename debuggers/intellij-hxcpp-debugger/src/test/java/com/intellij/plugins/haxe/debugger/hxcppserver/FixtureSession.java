package com.intellij.plugins.haxe.debugger.hxcppserver;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SourceBreakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.DapConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Assume;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * One live debug session against a gradle-built fixture, driving the REAL
 * Java client stack (DapClient, Jackson-typed requests, byte framing) the
 * same way the IDE does: bind the ephemeral listener FIRST, spawn the
 * debuggee with the listener's address in the env vars, accept its
 * connection. Fixture locations come from the {@code hxcpp.server.fixture.*}
 * system properties the gradle test task sets (with a build-relative
 * fallback so single tests run from the IDE too); a missing fixture skips
 * the test via {@link Assume}.
 */
final class FixtureSession implements AutoCloseable {
  static final long TIMEOUT_MILLIS = 15_000;

  // Load-bearing fixture line constants (see the marker comments in the
  // fixture sources; the files say "extend at the END, never reflow").
  static final String MAIN_SOURCE = "Main.hx";
  static final int MAIN_ADD_LINE = 17;
  // lines WITHOUT executable code, for the strict line-table verification tests
  static final int MAIN_COMMENT_LINE = 15;
  static final int MAIN_BLANK_LINE = 20;
  static final String EX_SOURCE = "MainEx.hx";
  static final int EX_THROW_LINE = 21;
  static final int EX_CAUGHT_NULL_LINE = 35;
  static final int EX_NULL_LINE = 43;
  static final int SMART_LINE = 107;
  static final int SMART_ONE_LINE = 113;
  static final int SMART_COMBINE_LINE = 117;
  static final int CHAIN_LINE = 131;
  static final int TYPED_THROW_LINE = 191;
  static final int DUP_CHAIN_LINE = 228;
  static final int TOSTRING_LINE = 258;
  static final int CLOSURE_CALL_LINE = 304;
  static final int CLOSURE_ARRAY_CALL_LINE = 306;
  static final int CLOSURE_BODY_LINE = 310;

  private final ServerSocket listener;
  private final Process debuggee;
  private final DapClient client;
  private final List<String> output = Collections.synchronizedList(new ArrayList<>());

  private FixtureSession(ServerSocket listener, Process debuggee, DapClient client) {
    this.listener = listener;
    this.debuggee = debuggee;
    this.client = client;
  }

  /** Launches the plain fixture (Main.hx: a short add() loop, then exit). */
  static FixtureSession launchMain() throws IOException {
    return launch("fixture", "Main-debug", null);
  }

  /** Launches the scenario fixture (MainEx.hx) in the given FIXTURE_MODE. */
  static FixtureSession launchScenario(String mode) throws IOException {
    return launch("fixture-ex", "MainEx-debug", mode);
  }

  private static FixtureSession launch(String fixture, String exeBaseName, String mode) throws IOException {
    Path exe = fixtureExe(fixture, exeBaseName);
    Assume.assumeTrue("fixture not built: " + exe + " (gradle builds it when haxe is on PATH)",
                      Files.isRegularFile(exe));
    ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    ProcessBuilder builder = new ProcessBuilder(exe.toString());
    builder.environment().put("HXCPP_DEBUG_HOST", "127.0.0.1");
    builder.environment().put("HXCPP_DEBUG_PORT", Integer.toString(listener.getLocalPort()));
    if (mode != null) {
      builder.environment().put("FIXTURE_MODE", mode);
    }
    builder.redirectErrorStream(true);
    Process debuggee = builder.start();
    FixtureSession[] holder = new FixtureSession[1];
    Thread pump = new Thread(() -> {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(debuggee.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          holder[0].output.add(line);
        }
      } catch (IOException ignored) {
      }
    }, "fixture-output");
    listener.setSoTimeout((int)TIMEOUT_MILLIS);
    try {
      FixtureSession session = new FixtureSession(listener, debuggee, new DapClient(new DapConnection(listener.accept())));
      holder[0] = session;
      pump.setDaemon(true);
      pump.start();
      return session;
    } catch (IOException e) {
      debuggee.destroyForcibly();
      listener.close();
      throw e;
    }
  }

  private static Path fixtureExe(String fixture, String exeBaseName) {
    String fromGradle = System.getProperty("hxcpp.server.fixture." + fixture + ".exe");
    if (fromGradle != null) {
      return Path.of(fromGradle);
    }
    String suffix = System.getProperty("os.name").startsWith("Windows") ? ".exe" : "";
    return Path.of("build", "hxcpp", fixture, exeBaseName + suffix).toAbsolutePath();
  }

  static Path sourcePath(String fileName) {
    String fromGradle = System.getProperty("hxcpp.server.fixture.src.dir");
    Path dir = fromGradle != null ? Path.of(fromGradle) : Path.of("test-fixtures", "src").toAbsolutePath();
    return dir.resolve(fileName);
  }

  // --- the IDE's session-start sequence ---

  /** initialize → initialized event → exception filters → (no configurationDone yet). */
  void initialize(String... exceptionFilters) throws IOException, InterruptedException {
    InitializeRequest initialize = new InitializeRequest();
    InitializeRequestArguments arguments = new InitializeRequestArguments();
    arguments.setAdapterID("intellij-haxe");
    arguments.setClientID("intellij");
    initialize.setArguments(arguments);
    assertTrue("initialize", request(initialize).isSuccess());
    if (client.pollEvent(TIMEOUT_MILLIS) == null) {
      fail("no initialized event");
    }
    setExceptionFilters(List.of(exceptionFilters), List.of());
  }

  /** Replaces the exception filters; filterTypes are typed exception class names. */
  void setExceptionFilters(List<String> filters, List<String> filterTypes) throws IOException, InterruptedException {
    SetExceptionBreakpointsRequest request = new SetExceptionBreakpointsRequest();
    SetExceptionBreakpointsArguments arguments = new SetExceptionBreakpointsArguments();
    arguments.setFilters(filters);
    arguments.setFilterTypes(filterTypes);
    request.setArguments(arguments);
    assertTrue("setExceptionBreakpoints", request(request).isSuccess());
  }

  void configurationDone() throws IOException, InterruptedException {
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());
  }

  /** Replaces the source's breakpoints; a null condition is an unconditional breakpoint. */
  void setBreakpoints(String sourceFile, int[] lines, String condition) throws IOException, InterruptedException {
    setBreakpointsRaw(sourceFile, lines, condition);
  }

  /** Like {@link #setBreakpoints} but hands back the response, for per-breakpoint verification asserts. */
  SetBreakpointsResponse setBreakpointsRaw(String sourceFile, int[] lines, String condition)
    throws IOException, InterruptedException {
    SetBreakpointsRequest request = new SetBreakpointsRequest();
    SetBreakpointsArguments arguments = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(sourcePath(sourceFile).toString());
    arguments.setSource(source);
    List<SourceBreakpoint> breakpoints = new ArrayList<>();
    for (int line : lines) {
      SourceBreakpoint breakpoint = new SourceBreakpoint();
      breakpoint.setLine(line);
      if (condition != null) {
        breakpoint.setCondition(condition);
      }
      breakpoints.add(breakpoint);
    }
    arguments.setBreakpoints(breakpoints);
    request.setArguments(arguments);
    Response response = request(request);
    assertTrue("setBreakpoints", response.isSuccess());
    return (SetBreakpointsResponse)response;
  }

  void clearBreakpoints(String sourceFile) throws IOException, InterruptedException {
    setBreakpoints(sourceFile, new int[0], null);
  }

  // --- run control ---

  StoppedEvent awaitStopped() throws IOException, InterruptedException {
    long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
    while (System.currentTimeMillis() < deadline) {
      Event event = client.pollEvent(250);
      if (event instanceof StoppedEvent stopped) {
        return stopped;
      }
    }
    fail("no stopped event within " + TIMEOUT_MILLIS + "ms");
    return null; // unreachable
  }

  int stoppedThread(StoppedEvent stopped) {
    return stopped.getBody().getThreadId() != null ? stopped.getBody().getThreadId() : 0;
  }

  void resume(int threadId) throws IOException, InterruptedException {
    ContinueRequest request = new ContinueRequest();
    ContinueArguments arguments = new ContinueArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    assertTrue("continue", request(request).isSuccess());
  }

  void pause() throws IOException, InterruptedException {
    assertTrue("pause", request(new PauseRequest()).isSuccess());
  }

  void next(int threadId) throws IOException, InterruptedException {
    NextRequest request = new NextRequest();
    NextArguments arguments = new NextArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    assertTrue("next", request(request).isSuccess());
  }

  void stepIn(int threadId) throws IOException, InterruptedException {
    StepInRequest request = new StepInRequest();
    StepInArguments arguments = new StepInArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    assertTrue("stepIn", request(request).isSuccess());
  }

  // --- inspection ---

  List<StackFrame> stackTrace(int threadId) throws IOException, InterruptedException {
    StackTraceRequest request = new StackTraceRequest();
    StackTraceArguments arguments = new StackTraceArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    StackTraceResponse response = (StackTraceResponse)request(request);
    assertTrue("stackTrace", response.isSuccess());
    return response.getBody().getStackFrames();
  }

  StackFrame topFrame(int threadId) throws IOException, InterruptedException {
    return stackTrace(threadId).get(0);
  }

  int localsReference(int frameId) throws IOException, InterruptedException {
    ScopesRequest request = new ScopesRequest();
    ScopesArguments arguments = new ScopesArguments();
    arguments.setFrameId(frameId);
    request.setArguments(arguments);
    ScopesResponse response = (ScopesResponse)request(request);
    assertTrue("scopes", response.isSuccess());
    return response.getBody().getScopes().get(0).getVariablesReference();
  }

  List<Variable> variables(int reference) throws IOException, InterruptedException {
    VariablesRequest request = new VariablesRequest();
    VariablesArguments arguments = new VariablesArguments();
    arguments.setVariablesReference(reference);
    request.setArguments(arguments);
    VariablesResponse response = (VariablesResponse)request(request);
    assertTrue("variables", response.isSuccess());
    return response.getBody().getVariables();
  }

  Variable variable(List<Variable> variables, String name) {
    for (Variable candidate : variables) {
      if (name.equals(candidate.getName())) {
        return candidate;
      }
    }
    fail("no variable '" + name + "' in " + variables);
    return null; // unreachable
  }

  /** Evaluate, asserting success; returns the rendered result. */
  String evaluate(String expression, int frameId) throws IOException, InterruptedException {
    Response response = evaluateRaw(expression, frameId);
    assertTrue("evaluate '" + expression + "': " + response.getMessage(), response.isSuccess());
    return ((EvaluateResponse)response).getBody().getResult();
  }

  /** A FAILED response decodes as ErrorResponse, hence the base return type. */
  Response evaluateRaw(String expression, int frameId) throws IOException, InterruptedException {
    EvaluateRequest request = new EvaluateRequest();
    EvaluateArguments arguments = new EvaluateArguments();
    arguments.setExpression(expression);
    arguments.setFrameId(frameId);
    arguments.setContext("watch");
    request.setArguments(arguments);
    return request(request);
  }

  // --- low-level ---

  Response request(Request request) throws IOException, InterruptedException {
    return client.sendRequest(request, TIMEOUT_MILLIS);
  }

  Event pollEvent(long timeoutMillis) throws InterruptedException {
    return client.pollEvent(timeoutMillis);
  }

  /** Lines the debuggee printed so far whose text starts with the prefix. */
  int outputCount(String prefix) {
    synchronized (output) {
      return (int)output.stream().filter(line -> line.startsWith(prefix)).count();
    }
  }

  List<String> outputSnapshot() {
    synchronized (output) {
      return new ArrayList<>(output);
    }
  }

  /** Waits until the count of prefixed output lines exceeds the floor (program is running). */
  void awaitOutputAbove(String prefix, int floor) throws InterruptedException {
    long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
    while (outputCount(prefix) <= floor && System.currentTimeMillis() < deadline) {
      Thread.sleep(50);
    }
    assertTrue("output '" + prefix + "' never grew past " + floor + " — the program is not running",
               outputCount(prefix) > floor);
  }

  /** Waits for the debuggee process to exit on its own; returns the exit code. */
  int awaitExit() throws InterruptedException {
    if (!debuggee.waitFor(TIMEOUT_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)) {
      fail("the debuggee did not exit");
    }
    return debuggee.exitValue();
  }

  @Override
  public void close() {
    debuggee.destroyForcibly();
    try {
      client.close();
    } catch (IOException ignored) {
    }
    try {
      listener.close();
    } catch (IOException ignored) {
    }
  }
}
