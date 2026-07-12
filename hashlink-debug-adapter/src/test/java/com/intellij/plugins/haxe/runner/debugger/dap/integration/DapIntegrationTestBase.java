package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ContinueArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ContinueRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.InitializeRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.InitializeRequestArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.LaunchRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.LaunchRequestArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.NextArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.NextRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ScopesArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ScopesRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.ScopesResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.SetBreakpointsArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.SetBreakpointsRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SourceBreakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.StackTraceArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.StackTraceRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.StackTraceResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.StepInArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.StepInRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.StepOutArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.StepOutRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.VariablesArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.VariablesRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.VariablesResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;

/**
 * Base class for the integration tests that drive the real adapter bytecode with
 * a real HashLink executable over TCP DAP. Owns the adapter process lifecycle
 * (spawn with tracing, port handshake, teardown with output drain) and the
 * request/event helpers every test needs. Tests are skipped (not failed) when
 * the adapter, fixture or HashLink executable is unavailable — see
 * {@link HlExecutableResolver}.
 *
 * The FIXTURE_* constants are load-bearing: they mirror the marked lines in
 * test-fixtures/src/ (Main.hx, Config.hx, Rich.hx) and must be updated
 * together with those files.
 */
public abstract class DapIntegrationTestBase {
  protected static final String LISTENING_PREFIX = "DAP-ADAPTER-LISTENING:";
  // generous: the first run after a rebuild can be slow (JIT warmup / AV scans)
  protected static final long TIMEOUT = 15_000;

  protected static final String FIXTURE_MAIN = "Main.hx";
  protected static final int FIXTURE_LOOP_LINE = 18; // total = add(total, i)
  protected static final int FIXTURE_ADD_LINE = 28; // return current + amount
  protected static final int FIXTURE_INSPECT_LINE = 35; // var v = Config.version (p in scope)
  protected static final String FIXTURE_CONFIG = "Config.hx";
  protected static final int FIXTURE_STATICS_LINE = 14; // Config.bump(): version=7, title="cfg"
  protected static final String FIXTURE_RICH = "Rich.hx";
  protected static final int FIXTURE_RICH_LINE = 39; // Rich.demo(): arrays/dyn/enum/anon/closure/ref/dynobj/maps/structs
  protected static final String FIXTURE_POINT = "Point.hx";
  protected static final int FIXTURE_POINT_METHOD_LINE = 22; // Point.move(): `this` in scope, x still 10
  protected static final String FIXTURE_SHADOW = "Shadowed.hx";
  protected static final int FIXTURE_SHADOW_LOOP_LINE = 16; // inside the loop: x is the shadowing Int
  protected static final int FIXTURE_SHADOW_AFTER_LINE = 18; // after the loop: x is the outer String again
  protected static final String FIXTURE_MUTATE = "Mutate.hx";
  protected static final int FIXTURE_MUTATE_LINE = 21; // Mutate.demo() checkpoint: n/flag/obj/arr in scope
  protected static final int FIXTURE_CACHED_LINE = 37; // Mutate.cachedUse(): v used on this line and the previous one
  protected static final int FIXTURE_FLOAT_LINE = 44; // Mutate.floatParam(): Float arg traced on the callee's first line
  protected static final int FIXTURE_INT_ARG_LINE = 50; // Mutate.intParam(): Int arg traced on the callee's first line
  protected static final String FIXTURE_CALL = "Call.hx";
  protected static final int FIXTURE_CALL_LINE = 36; // Call.demo(): add/scale/negate/label callable, s reassignable

  protected Process adapterProcess;
  protected DapClient client;
  protected Path fixtureHl;
  protected Path fixtureSrcDir;
  protected String hlExecutable;

  private int lastThreadId = 1;
  private final StringBuilder adapterOutput = new StringBuilder();
  private Thread outputGobbler;

  /** Override to false for tests that talk to the adapter without a debuggee. */
  protected boolean needsFixture() {
    return true;
  }

  @Before
  public void startAdapter() throws IOException {
    String adapter = System.getProperty("dap.adapter.hl", "");
    Assume.assumeTrue("adapter bytecode not built - skipping",
                      !adapter.isEmpty() && Files.isRegularFile(Path.of(adapter)));
    if (needsFixture()) {
      String fixtureProperty = System.getProperty("dap.fixture.hl", "");
      Assume.assumeTrue("debuggee fixture not built - skipping",
                        !fixtureProperty.isEmpty() && Files.isRegularFile(Path.of(fixtureProperty)));
      fixtureHl = Path.of(fixtureProperty);
      fixtureSrcDir = Path.of(System.getProperty("dap.fixture.src.dir", ""));
    }
    Optional<Path> hl = HlExecutableResolver.resolve();
    Assume.assumeTrue("HashLink executable not found (set -PhashlinkBin / -Dhashlink.executable, "
                      + "HASHLINK_BIN / HASHLINK / HASHLINKPATH, or put hl on PATH) - skipping",
                      hl.isPresent());
    hlExecutable = hl.get().toString();

    ProcessBuilder builder = new ProcessBuilder(hlExecutable, adapter, "--port", "0")
      .redirectErrorStream(true);
    builder.environment().put("DAP_ADAPTER_TRACE", "1");
    adapterProcess = builder.start();
    client = DapClient.connect("127.0.0.1", awaitListeningPort(), (int)TIMEOUT);
  }

  @After
  public void stopAdapter() throws Exception {
    if (client != null) {
      try {
        client.close();
      } catch (IOException ignored) {
      }
    }
    if (adapterProcess != null) {
      if (!adapterProcess.waitFor(3, TimeUnit.SECONDS)) {
        adapterProcess.destroyForcibly();
        adapterProcess.waitFor(5, TimeUnit.SECONDS);
      }
      if (outputGobbler != null) {
        outputGobbler.join(2000);
      }
      printAdapterOutput();
    }
  }

  // The trace breadcrumbs (DAP_ADAPTER_TRACE) can exceed the OS pipe buffer, so
  // a background thread must drain the adapter's merged stdout/stderr for the
  // whole test — otherwise the adapter would block mid-write and we would be
  // debugging a hang we created ourselves. The captured output is printed on
  // teardown so a failed run self-diagnoses.
  private void printAdapterOutput() {
    String output;
    synchronized (adapterOutput) {
      output = adapterOutput.toString();
    }
    if (!output.isEmpty()) {
      System.out.println("[adapter output]\n" + output);
    }
  }

  // Reads until the port line, then keeps draining into adapterOutput.
  private int awaitListeningPort() throws IOException {
    BufferedReader stdout = new BufferedReader(
      new InputStreamReader(adapterProcess.getInputStream(), StandardCharsets.UTF_8));
    String line;
    while ((line = stdout.readLine()) != null) {
      if (line.startsWith(LISTENING_PREFIX)) {
        int port = Integer.parseInt(line.substring(LISTENING_PREFIX.length()).trim());
        outputGobbler = new Thread(() -> gobble(stdout), "adapter-output-gobbler");
        outputGobbler.setDaemon(true);
        outputGobbler.start();
        return port;
      }
    }
    throw new IOException("Adapter exited before announcing its listening port");
  }

  private void gobble(BufferedReader stdout) {
    try {
      String line;
      while ((line = stdout.readLine()) != null) {
        synchronized (adapterOutput) {
          adapterOutput.append(line).append('\n');
        }
      }
    } catch (IOException ignored) {
      // process ended
    }
  }

  // --- request plumbing ---

  protected Response request(Request request) throws Exception {
    return client.sendRequest(request, TIMEOUT);
  }

  /** initialize + assert success + drain the initialized event. */
  protected void initialize() throws Exception {
    InitializeRequest request = new InitializeRequest();
    InitializeRequestArguments args = new InitializeRequestArguments();
    args.setAdapterID("intellij-haxe-test");
    args.setClientID("junit");
    request.setArguments(args);
    assertTrue("initialize succeeds", request(request).isSuccess());
    assertNotNull("initialized event", client.pollEvent(TIMEOUT));
  }

  /** Launches the shared fixture debuggee. */
  protected Response launch() throws Exception {
    return launch(fixtureHl.toString());
  }

  protected Response launch(String program) throws Exception {
    LaunchRequest request = new LaunchRequest();
    LaunchRequestArguments args = new LaunchRequestArguments();
    args.setProgram(program);
    args.setHlPath(hlExecutable);
    request.setArguments(args);
    return request(request);
  }

  /** Sets a breakpoint in one of the fixture source files (FIXTURE_MAIN etc.). */
  protected Response setBreakpoint(String fixtureFile, int line) throws Exception {
    return setBreakpoints(fixtureSrcDir.resolve(fixtureFile).toString(), line);
  }

  protected Response setBreakpoints(String sourcePath, int... lines) throws Exception {
    SetBreakpointsRequest request = new SetBreakpointsRequest();
    SetBreakpointsArguments args = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(sourcePath);
    source.setName(Path.of(sourcePath).getFileName().toString());
    args.setSource(source);
    List<SourceBreakpoint> breakpoints = new ArrayList<>();
    for (int line : lines) {
      breakpoints.add(sourceBreakpoint(line));
    }
    args.setBreakpoints(breakpoints);
    request.setArguments(args);
    return request(request);
  }

  protected static SourceBreakpoint sourceBreakpoint(int line) {
    SourceBreakpoint breakpoint = new SourceBreakpoint();
    breakpoint.setLine(line);
    return breakpoint;
  }

  /** initialize + launch the fixture + one breakpoint + configurationDone + first stop. */
  protected StoppedEvent runToBreakpoint(String fixtureFile, int line) throws Exception {
    initialize();
    assertTrue("launch succeeds", launch().isSuccess());
    assertTrue("setBreakpoints succeeds", setBreakpoint(fixtureFile, line).isSuccess());
    assertTrue("configurationDone succeeds", request(new ConfigurationDoneRequest()).isSuccess());
    return awaitStopped();
  }

  // --- events ---

  /** Polls events until a stopped event arrives (output etc. is skipped). */
  protected StoppedEvent awaitStopped() throws Exception {
    while (true) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull("expected a stopped event", event);
      if (event instanceof StoppedEvent stopped) {
        lastThreadId = stopped.getBody().getThreadId();
        return stopped;
      }
    }
  }

  /** The thread id of the most recent stopped event (default 1). */
  protected int lastStoppedThreadId() {
    return lastThreadId;
  }

  /** continue on the last stopped thread, then wait for the next stop. */
  protected StoppedEvent continueToNextStop() throws Exception {
    assertTrue("continue succeeds", request(continueRequest(lastThreadId)).isSuccess());
    return awaitStopped();
  }

  // --- step/continue request builders ---

  protected static ContinueRequest continueRequest(int threadId) {
    ContinueRequest request = new ContinueRequest();
    ContinueArguments args = new ContinueArguments();
    args.setThreadId(threadId);
    request.setArguments(args);
    return request;
  }

  protected static NextRequest nextRequest(int threadId) {
    NextRequest request = new NextRequest();
    NextArguments args = new NextArguments();
    args.setThreadId(threadId);
    request.setArguments(args);
    return request;
  }

  protected static StepInRequest stepInRequest(int threadId) {
    StepInRequest request = new StepInRequest();
    StepInArguments args = new StepInArguments();
    args.setThreadId(threadId);
    request.setArguments(args);
    return request;
  }

  protected static StepOutRequest stepOutRequest(int threadId) {
    StepOutRequest request = new StepOutRequest();
    StepOutArguments args = new StepOutArguments();
    args.setThreadId(threadId);
    request.setArguments(args);
    return request;
  }

  // --- stack / scopes / variables ---

  protected StackTraceResponse stackTrace(int threadId) throws Exception {
    StackTraceRequest request = new StackTraceRequest();
    StackTraceArguments args = new StackTraceArguments();
    args.setThreadId(threadId);
    request.setArguments(args);
    StackTraceResponse response = (StackTraceResponse)request(request);
    assertTrue("has a top frame", response.getBody().getStackFrames().size() >= 1);
    return response;
  }

  protected int topFrameId(int threadId) throws Exception {
    return stackTrace(threadId).getBody().getStackFrames().get(0).getId();
  }

  protected String topFrameName(int threadId) throws Exception {
    return stackTrace(threadId).getBody().getStackFrames().get(0).getName();
  }

  protected int localsScopeReference(int frameId) throws Exception {
    return scopeReference(frameId, "Locals");
  }

  protected int staticsScopeReference(int frameId) throws Exception {
    return scopeReference(frameId, "Statics");
  }

  private int scopeReference(int frameId, String namePrefix) throws Exception {
    Scope scope = scopeByPrefix(frameId, namePrefix);
    if (scope == null) throw new IllegalStateException("no " + namePrefix + " scope");
    return scope.getVariablesReference();
  }

  protected Scope scopeByPrefix(int frameId, String namePrefix) throws Exception {
    ScopesRequest request = new ScopesRequest();
    ScopesArguments args = new ScopesArguments();
    args.setFrameId(frameId);
    request.setArguments(args);
    ScopesResponse response = (ScopesResponse)request(request);
    for (Scope scope : response.getBody().getScopes()) {
      if (scope.getName() != null && scope.getName().startsWith(namePrefix)) {
        return scope;
      }
    }
    return null;
  }

  protected List<Variable> variables(int reference) throws Exception {
    VariablesRequest request = new VariablesRequest();
    VariablesArguments args = new VariablesArguments();
    args.setVariablesReference(reference);
    request.setArguments(args);
    return ((VariablesResponse)request(request)).getBody().getVariables();
  }

  protected Map<String, String> variablesByName(int reference) throws Exception {
    Map<String, String> byName = new HashMap<>();
    for (Variable v : variables(reference)) {
      byName.put(v.getName(), v.getValue());
    }
    return byName;
  }

  protected List<Variable> topFrameVariables(int threadId) throws Exception {
    return variables(localsScopeReference(topFrameId(threadId)));
  }

  protected Map<String, String> localsInTopFrame(int threadId) throws Exception {
    return variablesByName(localsScopeReference(topFrameId(threadId)));
  }

  protected static Variable findVariable(List<Variable> variables, String name) {
    for (Variable v : variables) {
      if (name.equals(v.getName())) {
        return v;
      }
    }
    return null;
  }
}
