package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ContinueArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ContinueRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeRequestArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.LaunchRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.LaunchRequestArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.NextArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.NextRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ScopesArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ScopesRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ScopesResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SetBreakpointsArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SetBreakpointsRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SourceBreakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackTraceArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackTraceRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackTraceResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StepInArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StepInRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StepOutArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StepOutRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.VariablesArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.VariablesRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.VariablesResponse;
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
 * The FIXTURE_* line constants are load-bearing: they mirror
 * test-fixtures/src/Main.hx and must be updated together with it.
 */
public abstract class DapIntegrationTestBase {
  protected static final String LISTENING_PREFIX = "DAP-ADAPTER-LISTENING:";
  // generous: the first run after a rebuild can be slow (JIT warmup / AV scans)
  protected static final long TIMEOUT = 15_000;

  protected static final int FIXTURE_LOOP_LINE = 18; // total = add(total, i)
  protected static final int FIXTURE_ADD_LINE = 28; // return current + amount
  protected static final int FIXTURE_INSPECT_LINE = 35; // var v = Config.version (p in scope)
  protected static final int FIXTURE_STATICS_LINE = 60; // Config.bump(): version=7, title="cfg"
  protected static final int FIXTURE_RICH_LINE = 83; // Rich.demo(): arrays/dyn/enum/anon/closure

  protected Process adapterProcess;
  protected DapClient client;
  protected Path fixtureHl;
  protected String fixtureSrc;
  protected String hlExecutable;

  private int lastThreadId = 1;

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
      fixtureSrc = System.getProperty("dap.fixture.src", "");
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
      drainAdapterOutput();
      if (!adapterProcess.waitFor(3, TimeUnit.SECONDS)) {
        adapterProcess.destroyForcibly();
        adapterProcess.waitFor(5, TimeUnit.SECONDS);
      }
    }
  }

  // Surface anything the adapter printed after the port line (nothing reads that
  // pipe during a test, so trace breadcrumbs / crash output would otherwise be
  // invisible).
  private void drainAdapterOutput() {
    try {
      var in = adapterProcess.getInputStream();
      int available = in.available();
      if (available > 0) {
        byte[] pending = in.readNBytes(available);
        System.out.println("[adapter output] " + new String(pending, StandardCharsets.UTF_8));
      }
    } catch (IOException ignored) {
    }
  }

  private int awaitListeningPort() throws IOException {
    BufferedReader stdout = new BufferedReader(
      new InputStreamReader(adapterProcess.getInputStream(), StandardCharsets.UTF_8));
    String line;
    while ((line = stdout.readLine()) != null) {
      if (line.startsWith(LISTENING_PREFIX)) {
        return Integer.parseInt(line.substring(LISTENING_PREFIX.length()).trim());
      }
    }
    throw new IOException("Adapter exited before announcing its listening port");
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

  protected Response setBreakpoint(int line) throws Exception {
    return setBreakpoints(fixtureSrc, line);
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
  protected StoppedEvent runToBreakpoint(int line) throws Exception {
    initialize();
    assertTrue("launch succeeds", launch().isSuccess());
    assertTrue("setBreakpoints succeeds", setBreakpoint(line).isSuccess());
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
    ScopesRequest request = new ScopesRequest();
    ScopesArguments args = new ScopesArguments();
    args.setFrameId(frameId);
    request.setArguments(args);
    ScopesResponse response = (ScopesResponse)request(request);
    for (Scope scope : response.getBody().getScopes()) {
      if (scope.getName() != null && scope.getName().startsWith(namePrefix)) {
        return scope.getVariablesReference();
      }
    }
    throw new IllegalStateException("no " + namePrefix + " scope");
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
