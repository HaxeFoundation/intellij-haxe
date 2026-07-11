package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ContinueArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ContinueRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.DisconnectRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.InitializeRequestArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.LaunchRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.LaunchRequestArguments;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Reads local variable values from a real HashLink debug session and asserts the
 * KNOWN values at each loop iteration — this validates the reconstructed stack
 * frame offsets end to end. Skipped when the adapter/hl are unavailable.
 * Line constants mirror test-fixtures/src/Main.hx.
 */
public class VariablesIntegrationTest {
  private static final String LISTENING_PREFIX = "DAP-ADAPTER-LISTENING:";
  // generous: the first run after a rebuild can be slow (JIT warmup / AV scans)
  private static final long TIMEOUT = 15_000;
  private static final int FIXTURE_LOOP_LINE = 18; // total = add(total, i)
  private static final int FIXTURE_INSPECT_LINE = 35; // var v = Config.version (p is in scope)
  private static final int FIXTURE_STATICS_LINE = 60; // Config.bump(): version=7, title="cfg"
  private static final int FIXTURE_RICH_LINE = 83; // Rich.demo(): arrays/dyn/enum/anon/closure in scope

  private Process adapterProcess;
  private DapClient client;
  private Path fixtureHl;
  private String fixtureSrc;
  private String hlExecutable;

  @Before
  public void startAdapter() throws IOException {
    String adapter = System.getProperty("dap.adapter.hl", "");
    Assume.assumeTrue("adapter bytecode not built - skipping",
                      !adapter.isEmpty() && Files.isRegularFile(Path.of(adapter)));
    String fixtureProperty = System.getProperty("dap.fixture.hl", "");
    Assume.assumeTrue("debuggee fixture not built - skipping",
                      !fixtureProperty.isEmpty() && Files.isRegularFile(Path.of(fixtureProperty)));
    fixtureHl = Path.of(fixtureProperty);
    fixtureSrc = System.getProperty("dap.fixture.src", "");
    Optional<Path> hl = HlExecutableResolver.resolve();
    Assume.assumeTrue("HashLink executable not found - skipping", hl.isPresent());
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
  // pipe during the test, so a crash trace would otherwise be invisible).
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

  @Test
  public void readsLocalsAndTracksValuesAcrossIterations() throws Exception {
    initialize();
    assertTrue(launch().isSuccess());
    assertTrue(setBreakpoint(FIXTURE_LOOP_LINE).isSuccess());
    assertTrue(request(new ConfigurationDoneRequest()).isSuccess());

    // iteration 1: i=0, count=3, total=0 (not yet updated on this line)
    StoppedEvent stop1 = awaitStopped();
    int thread = stop1.getBody().getThreadId();
    Map<String, String> locals1 = localsInTopFrame(thread);
    assertEquals("count is 3", "3", locals1.get("count"));
    assertEquals("i is 0 on first iteration", "0", locals1.get("i"));
    assertEquals("total is 0 before first add", "0", locals1.get("total"));

    // step into add(total, i) and read its arguments (stack-passed on Windows x64)
    Map<String, String> addArgs = argsAfterStepIn(thread);
    assertEquals("current arg is 0", "0", addArgs.get("current"));
    assertEquals("amount arg is 0", "0", addArgs.get("amount"));

    // resume to the next iterations and watch the values move
    Map<String, String> locals2 = localsInTopFrame(continueToNextStop());
    assertEquals("i is 1 on second iteration", "1", locals2.get("i"));

    Map<String, String> locals3 = localsInTopFrame(continueToNextStop());
    assertEquals("i is 2 on third iteration", "2", locals3.get("i"));
    assertEquals("total is 1 after two adds", "1", locals3.get("total"));

    request(new DisconnectRequest());
  }

  @Test
  public void expandsObjectFields() throws Exception {
    initialize();
    assertTrue(launch().isSuccess());
    assertTrue(setBreakpoint(FIXTURE_INSPECT_LINE).isSuccess());
    assertTrue(request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent stopped = awaitStopped();
    int thread = stopped.getBody().getThreadId();

    // find the local `p` (a Point) and confirm it is expandable
    Variable p = findVariable(topFrameVariables(thread), "p");
    assertNotNull("local p present", p);
    assertTrue("Point is expandable", p.getVariablesReference() > 0);
    assertTrue("p typed as Point (was " + p.getType() + ")", "Point".equals(p.getType()));

    // expand Point -> x=10, y=20, label="origin"
    Map<String, String> fields = variablesByName(p.getVariablesReference());
    assertEquals("Point.x", "10", fields.get("x"));
    assertEquals("Point.y", "20", fields.get("y"));
    assertEquals("Point.label", "\"origin\"", fields.get("label"));

    request(new DisconnectRequest());
  }

  @Test
  public void readsStaticFields() throws Exception {
    initialize();
    assertTrue(launch().isSuccess());
    assertTrue(setBreakpoint(FIXTURE_STATICS_LINE).isSuccess());
    assertTrue(request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent stopped = awaitStopped();
    int frameId = topFrameId(stopped.getBody().getThreadId());

    // the frame is Config.bump — its Statics scope holds version=7, title="cfg"
    int staticsRef = staticsScopeReference(frameId);
    Map<String, String> statics = variablesByName(staticsRef);
    assertEquals("Config.version", "7", statics.get("version"));
    assertEquals("Config.title", "\"cfg\"", statics.get("title"));
    // the static method sharing the container must not leak into the scope
    assertFalse("bump() hidden from Statics", statics.containsKey("bump"));

    request(new DisconnectRequest());
  }

  @Test
  public void readsRichValues() throws Exception {
    initialize();
    assertTrue(launch().isSuccess());
    assertTrue(setBreakpoint(FIXTURE_RICH_LINE).isSuccess());
    assertTrue(request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent stopped = awaitStopped();
    List<Variable> locals = topFrameVariables(stopped.getBody().getThreadId());

    assertEquals("n", "2", findVariable(locals, "n").getValue());

    // Array<Int> -> hl.types.ArrayBytes_Int: elements straight from the bytes
    Variable ints = findVariable(locals, "ints");
    assertNotNull("local ints present", ints);
    assertEquals("ints preview", "Array(3)", ints.getValue());
    Map<String, String> intElems = variablesByName(ints.getVariablesReference());
    assertEquals("ints[0]", "2", intElems.get("0"));
    assertEquals("ints[1]", "5", intElems.get("1"));
    assertEquals("ints[2]", "10", intElems.get("2"));

    // Array<String> -> hl.types.ArrayObj: elements typed via the varray's runtime type
    Variable names = findVariable(locals, "names");
    assertEquals("names preview", "Array(2)", names.getValue());
    Map<String, String> nameElems = variablesByName(names.getVariablesReference());
    assertEquals("names[0]", "\"a2\"", nameElems.get("0"));
    assertEquals("names[1]", "\"b\"", nameElems.get("1"));

    // Dynamic holding an Int: unboxed via the vdynamic's runtime type
    assertEquals("dyn unboxes via runtime type", "42", findVariable(locals, "dyn").getValue());

    // enum: inline constructor preview + params as children
    Variable shade = findVariable(locals, "shade");
    assertEquals("enum inline preview", "Tinted(2, \"red\")", shade.getValue());
    Map<String, String> shadeParams = variablesByName(shade.getVariablesReference());
    assertEquals("Tinted param 0", "2", shadeParams.get("0"));
    assertEquals("Tinted param 1", "\"red\"", shadeParams.get("1"));

    // anonymous structure (virtual): fields via the indirect pointers
    Variable anon = findVariable(locals, "anon");
    assertTrue("anon is expandable", anon.getVariablesReference() > 0);
    Map<String, String> anonFields = variablesByName(anon.getVariablesReference());
    assertEquals("anon.width", "2", anonFields.get("width"));
    assertEquals("anon.tag", "\"t2\"", anonFields.get("tag"));

    Variable f = findVariable(locals, "f");
    assertTrue("closure renders as a function (was " + f.getValue() + ")",
               f.getValue().startsWith("function"));

    request(new DisconnectRequest());
  }

  // --- helpers ---

  private int staticsScopeReference(int frameId) throws Exception {
    ScopesRequest sr = new ScopesRequest();
    ScopesArguments a = new ScopesArguments();
    a.setFrameId(frameId);
    sr.setArguments(a);
    ScopesResponse response = (ScopesResponse)request(sr);
    for (Scope scope : response.getBody().getScopes()) {
      if (scope.getName() != null && scope.getName().startsWith("Statics")) {
        return scope.getVariablesReference();
      }
    }
    throw new IllegalStateException("no Statics scope");
  }

  private List<Variable> topFrameVariables(int threadId) throws Exception {
    int reference = localsScopeReference(topFrameId(threadId));
    VariablesRequest vr = new VariablesRequest();
    VariablesArguments a = new VariablesArguments();
    a.setVariablesReference(reference);
    vr.setArguments(a);
    return ((VariablesResponse)request(vr)).getBody().getVariables();
  }

  private static Variable findVariable(List<Variable> variables, String name) {
    for (Variable v : variables) {
      if (name.equals(v.getName())) {
        return v;
      }
    }
    return null;
  }

  private Map<String, String> localsInTopFrame(int threadId) throws Exception {
    int frameId = topFrameId(threadId);
    int scopeRef = localsScopeReference(frameId);
    return variablesByName(scopeRef);
  }

  private Map<String, String> argsAfterStepIn(int threadId) throws Exception {
    StepInRequest step = new StepInRequest();
    StepInArguments args = new StepInArguments();
    args.setThreadId(threadId);
    step.setArguments(args);
    assertTrue(request(step).isSuccess());
    StoppedEvent stopped = awaitStopped();
    return localsInTopFrame(stopped.getBody().getThreadId());
  }

  private int continueToNextStop() throws Exception {
    // find the current thread from a fresh stackTrace is unnecessary; continue uses the last stop
    ContinueRequest cont = new ContinueRequest();
    ContinueArguments a = new ContinueArguments();
    a.setThreadId(lastThreadId);
    cont.setArguments(a);
    assertTrue(request(cont).isSuccess());
    StoppedEvent stopped = awaitStopped();
    return stopped.getBody().getThreadId();
  }

  private int lastThreadId = 1;

  private int topFrameId(int threadId) throws Exception {
    StackTraceRequest st = new StackTraceRequest();
    StackTraceArguments a = new StackTraceArguments();
    a.setThreadId(threadId);
    st.setArguments(a);
    StackTraceResponse response = (StackTraceResponse)request(st);
    assertTrue("has a top frame", response.getBody().getStackFrames().size() >= 1);
    return response.getBody().getStackFrames().get(0).getId();
  }

  private int localsScopeReference(int frameId) throws Exception {
    ScopesRequest sr = new ScopesRequest();
    ScopesArguments a = new ScopesArguments();
    a.setFrameId(frameId);
    sr.setArguments(a);
    ScopesResponse response = (ScopesResponse)request(sr);
    for (Scope scope : response.getBody().getScopes()) {
      if ("Locals".equals(scope.getName())) {
        return scope.getVariablesReference();
      }
    }
    throw new IllegalStateException("no Locals scope");
  }

  private Map<String, String> variablesByName(int reference) throws Exception {
    VariablesRequest vr = new VariablesRequest();
    VariablesArguments a = new VariablesArguments();
    a.setVariablesReference(reference);
    vr.setArguments(a);
    VariablesResponse response = (VariablesResponse)request(vr);
    Map<String, String> byName = new HashMap<>();
    for (Variable v : response.getBody().getVariables()) {
      byName.put(v.getName(), v.getValue());
    }
    return byName;
  }

  private StoppedEvent awaitStopped() throws Exception {
    while (true) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull("expected a stopped event", event);
      if (event instanceof StoppedEvent stopped) {
        lastThreadId = stopped.getBody().getThreadId();
        return stopped;
      }
    }
  }

  private void initialize() throws Exception {
    InitializeRequest request = new InitializeRequest();
    InitializeRequestArguments args = new InitializeRequestArguments();
    args.setAdapterID("intellij-haxe-test");
    request.setArguments(args);
    assertTrue(request(request).isSuccess());
    assertNotNull("initialized event", client.pollEvent(TIMEOUT));
  }

  private Response launch() throws Exception {
    LaunchRequest request = new LaunchRequest();
    LaunchRequestArguments args = new LaunchRequestArguments();
    args.setProgram(fixtureHl.toString());
    args.setHlPath(hlExecutable);
    request.setArguments(args);
    return request(request);
  }

  private Response setBreakpoint(int line) throws Exception {
    SetBreakpointsRequest request = new SetBreakpointsRequest();
    SetBreakpointsArguments args = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(fixtureSrc);
    source.setName("Main.hx");
    args.setSource(source);
    SourceBreakpoint bp = new SourceBreakpoint();
    bp.setLine(line);
    args.setBreakpoints(List.of(bp));
    request.setArguments(args);
    return request(request);
  }

  private Response request(Request request) throws Exception {
    return client.sendRequest(request, TIMEOUT);
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
}
