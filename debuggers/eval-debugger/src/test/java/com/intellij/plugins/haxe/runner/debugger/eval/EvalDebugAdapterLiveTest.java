package com.intellij.plugins.haxe.runner.debugger.eval;

import com.intellij.plugins.haxe.runner.debugger.dap.DapPaths;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SourceBreakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Drives {@link EvalDebugAdapter} end to end: a real {@link DapClient} on one
 * side, the REAL haxe eval VM (spawned with -D eval-debugger) on the other.
 * The full IDE flow — initialize, launch, breakpoints, configurationDone,
 * stop, stack/scopes/variables, evaluate, step, resume, terminate — against
 * the fixture in test-fixtures/EvalMain.hx. Skips when haxe is not on PATH.
 */
public class EvalDebugAdapterLiveTest {
  private static final int BREAK_LINE = 10;
  private static final int NESTED_CALL_LINE = 11; // `var nested = outer(inner(3));`
  private static final int INNER_LINE = 16;       // first EXECUTABLE line inside inner() (the return)
  private static final int OUTER_LINE = 20;       // first executable line inside outer()
  private static final int CHAIN_LINE = 25;       // `cfg.test1(1).test2().test3().test1(2);`
  private static final int CHAIN_AFTER_LINE = 26; // the println after the chain
  private static final int COLL_LINE = 51;        // Coll.collections println (items array live)
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
    Assume.assumeTrue("haxe not on PATH - skipping live eval adapter test", haxeOnPath());
    Path fixtures = fixtureDir();
    Assume.assumeTrue("eval fixture missing - skipping", Files.isRegularFile(fixtures.resolve("EvalMain.hx")));

    adapter = new EvalDebugAdapter(TIMEOUT);
    haxe = new ProcessBuilder("haxe", "-cp", fixtures.toString(), "-main", "EvalMain",
                              "-D", "eval-debugger=127.0.0.1:" + adapter.getVmPort(),
                              "--interp")
      .redirectErrorStream(true)
      .start();

    dapListener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    Socket clientSide = new Socket(InetAddress.getLoopbackAddress(), dapListener.getLocalPort());
    Socket adapterSide = dapListener.accept();
    adapter.start(new DapConnection(adapterSide));
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
    throw new AssertionError("no stopped event within " + TIMEOUT + "ms");
  }

  private boolean awaitTerminated() throws Exception {
    long deadline = System.currentTimeMillis() + TIMEOUT;
    while (System.currentTimeMillis() < deadline) {
      Event event = dapClient.pollEvent(250);
      if (event instanceof TerminatedEvent) {
        return true;
      }
    }
    return false;
  }

  @Test
  public void fullSessionBreakpointInspectStepAndFinish() throws Exception {
    InitializeRequest initialize = new InitializeRequest();
    InitializeRequestArguments initArgs = new InitializeRequestArguments();
    initArgs.setAdapterID("intellij-haxe-eval");
    initialize.setArguments(initArgs);
    assertTrue("initialize", request(initialize).isSuccess());
    assertNotNull("initialized event", dapClient.pollEvent(TIMEOUT));

    assertTrue("launch (VM connected and waiting)", request(new LaunchRequest()).isSuccess());

    String fixture = fixtureDir().resolve("EvalMain.hx").toString();
    SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
    SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(fixture);
    bpArgs.setSource(source);
    SourceBreakpoint breakpoint = new SourceBreakpoint();
    breakpoint.setLine(BREAK_LINE);
    bpArgs.setBreakpoints(List.of(breakpoint));
    setBreakpoints.setArguments(bpArgs);
    assertTrue("setBreakpoints", request(setBreakpoints).isSuccess());

    assertTrue("configurationDone releases the waiting VM",
               request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent stopped = awaitStopped();
    assertEquals("stopped by the breakpoint", "breakpoint", stopped.getBody().getReason());
    int threadId = stopped.getBody().getThreadId();

    assertTrue("threads", request(new ThreadsRequest()).isSuccess());

    StackTraceRequest stackTrace = new StackTraceRequest();
    StackTraceArguments stArgs = new StackTraceArguments();
    stArgs.setThreadId(threadId);
    stackTrace.setArguments(stArgs);
    StackTraceResponse stResponse = (StackTraceResponse)request(stackTrace);
    assertTrue("stackTrace", stResponse.isSuccess());
    List<StackFrame> frames = stResponse.getBody().getStackFrames();
    assertFalse("frames at the stop", frames.isEmpty());
    StackFrame top = frames.get(0);
    assertEquals("stopped on the break line", BREAK_LINE, top.getLine());
    assertNotNull("top frame has a source", top.getSource());
    assertTrue("top frame is the fixture",
               DapPaths.toForwardSlashes(top.getSource().getPath()).endsWith("EvalMain.hx"));

    ScopesRequest scopes = new ScopesRequest();
    ScopesArguments scArgs = new ScopesArguments();
    scArgs.setFrameId(top.getId());
    scopes.setArguments(scArgs);
    ScopesResponse scResponse = (ScopesResponse)request(scopes);
    assertTrue("scopes", scResponse.isSuccess());
    assertFalse("scopes present", scResponse.getBody().getScopes().isEmpty());

    boolean sawGreeting = false;
    for (Scope scope : scResponse.getBody().getScopes()) {
      VariablesRequest variables = new VariablesRequest();
      VariablesArguments vArgs = new VariablesArguments();
      vArgs.setVariablesReference(scope.getVariablesReference());
      variables.setArguments(vArgs);
      VariablesResponse vResponse = (VariablesResponse)request(variables);
      assertTrue("variables of scope " + scope.getName(), vResponse.isSuccess());
      for (Variable variable : vResponse.getBody().getVariables()) {
        if ("greeting".equals(variable.getName())) {
          sawGreeting = true;
          assertTrue("greeting holds its value (was " + variable.getValue() + ")",
                     variable.getValue().contains("hello"));
        }
      }
    }
    assertTrue("local 'greeting' visible through DAP", sawGreeting);

    EvaluateRequest evaluate = new EvaluateRequest();
    EvaluateArguments eArgs = new EvaluateArguments();
    eArgs.setExpression("greeting.length + 1");
    eArgs.setFrameId(top.getId());
    evaluate.setArguments(eArgs);
    EvaluateResponse eResponse = (EvaluateResponse)request(evaluate);
    assertTrue("evaluate", eResponse.isSuccess());
    assertEquals("greeting.length + 1 == 6", "6", eResponse.getBody().getResult());

    // step over the break line and land on the next one, still in main
    NextRequest next = new NextRequest();
    NextArguments nArgs = new NextArguments();
    nArgs.setThreadId(threadId);
    next.setArguments(nArgs);
    assertTrue("next", request(next).isSuccess());
    StoppedEvent afterStep = awaitStopped();
    StackTraceResponse stepStack = (StackTraceResponse)request(stackTrace);
    assertEquals("landed on the line after the breakpoint", BREAK_LINE + 1,
                 stepStack.getBody().getStackFrames().get(0).getLine());

    ContinueRequest resume = new ContinueRequest();
    ContinueArguments cArgs = new ContinueArguments();
    cArgs.setThreadId(afterStep.getBody().getThreadId());
    resume.setArguments(cArgs);
    Response resumeResponse = request(resume);
    assertTrue("continue failed: " + resumeResponse.getMessage(), resumeResponse.isSuccess());

    assertTrue("terminated event when the script finishes", awaitTerminated());
    assertTrue("haxe exited", haxe.waitFor(TIMEOUT, TimeUnit.MILLISECONDS));
    assertEquals("clean exit", 0, haxe.exitValue());
  }

  @Test
  public void singleStepIntoEntersTheNestedCallee() throws Exception {
    // `var nested = outer(inner(3));` — eval's raw stepIn is sub-expression
    // granular (it stops on each column of the line before entering a callee),
    // so a plain step-into would need several presses. The adapter coalesces
    // those same-line sub-steps: ONE DAP step-into must land inside inner().
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue("initialize", request(initialize).isSuccess());
    dapClient.pollEvent(TIMEOUT);
    assertTrue("launch", request(new LaunchRequest()).isSuccess());

    String fixture = fixtureDir().resolve("EvalMain.hx").toString();
    SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
    SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(fixture);
    bpArgs.setSource(source);
    SourceBreakpoint breakpoint = new SourceBreakpoint();
    breakpoint.setLine(NESTED_CALL_LINE);
    bpArgs.setBreakpoints(List.of(breakpoint));
    setBreakpoints.setArguments(bpArgs);
    assertTrue("setBreakpoints", request(setBreakpoints).isSuccess());
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent atCall = awaitStopped();
    int threadId = atCall.getBody().getThreadId();
    StackTraceRequest stackTrace = new StackTraceRequest();
    StackTraceArguments stArgs = new StackTraceArguments();
    stArgs.setThreadId(threadId);
    stackTrace.setArguments(stArgs);
    StackFrame atBreak = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertEquals("stopped on the nested-call line", NESTED_CALL_LINE, atBreak.getLine());
    assertTrue("stopped in main", atBreak.getName().endsWith("main"));

    StepInRequest stepIn = new StepInRequest();
    StepInArguments siArgs = new StepInArguments();
    siArgs.setThreadId(threadId);
    stepIn.setArguments(siArgs);
    assertTrue("stepIn", request(stepIn).isSuccess());

    StoppedEvent afterStep = awaitStopped();
    assertEquals("step stop", "step", afterStep.getBody().getReason());
    StackFrame landed = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertTrue("ONE step-into entered inner() (was " + landed.getName() + " line " + landed.getLine() + ")",
               landed.getName().endsWith("inner"));
    assertEquals("landed inside inner()", INNER_LINE, landed.getLine());
  }

  @Test
  public void smartStepIntoSkipsToTheChosenCallee() throws Exception {
    // `var nested = outer(inner(3));` — smart-step to OUTER must walk the
    // line's sub-expressions, step THROUGH inner() without reporting it, and
    // land inside outer() (the eval protocol has no smart-step; the adapter
    // emulates custom/stepIntoFunction with sub-expression steps).
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue("initialize", request(initialize).isSuccess());
    dapClient.pollEvent(TIMEOUT);
    assertTrue("launch", request(new LaunchRequest()).isSuccess());

    String fixture = fixtureDir().resolve("EvalMain.hx").toString();
    SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
    SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(fixture);
    bpArgs.setSource(source);
    SourceBreakpoint breakpoint = new SourceBreakpoint();
    breakpoint.setLine(NESTED_CALL_LINE);
    bpArgs.setBreakpoints(List.of(breakpoint));
    setBreakpoints.setArguments(bpArgs);
    assertTrue("setBreakpoints", request(setBreakpoints).isSuccess());
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent atCall = awaitStopped();
    int threadId = atCall.getBody().getThreadId();

    StepIntoFunctionRequest smartStep = new StepIntoFunctionRequest();
    StepIntoFunctionArguments ssArgs = new StepIntoFunctionArguments();
    ssArgs.setThreadId(threadId);
    ssArgs.setClassName("EvalMain");
    ssArgs.setFunctionName("outer");
    ssArgs.setOccurrence(1);
    smartStep.setArguments(ssArgs);
    assertTrue("stepIntoFunction", request(smartStep).isSuccess());

    StoppedEvent afterStep = awaitStopped();
    assertEquals("step stop", "step", afterStep.getBody().getReason());
    StackTraceRequest stackTrace = new StackTraceRequest();
    StackTraceArguments stArgs = new StackTraceArguments();
    stArgs.setThreadId(threadId);
    stackTrace.setArguments(stArgs);
    StackFrame landed = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertTrue("smart-step landed in outer(), skipping inner() (was "
               + landed.getName() + " line " + landed.getLine() + ")",
               landed.getName().endsWith("outer"));
    assertEquals("on outer's executable line", OUTER_LINE, landed.getLine());
  }

  @Test
  public void chainedCallsStepOutReturnsMidLineForTheNextPick() throws Exception {
    // `cfg.test1(1).test2().test3().test1(2);` — the user-reported flow:
    // smart-step into test2, step OUT, and be back ON THE CHAIN LINE (not
    // dragged through test3/test1) to pick the next call or leave the line.
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue("initialize", request(initialize).isSuccess());
    dapClient.pollEvent(TIMEOUT);
    assertTrue("launch", request(new LaunchRequest()).isSuccess());

    String fixture = fixtureDir().resolve("EvalMain.hx").toString();
    SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
    SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(fixture);
    bpArgs.setSource(source);
    SourceBreakpoint breakpoint = new SourceBreakpoint();
    breakpoint.setLine(CHAIN_LINE);
    bpArgs.setBreakpoints(List.of(breakpoint));
    setBreakpoints.setArguments(bpArgs);
    assertTrue("setBreakpoints", request(setBreakpoints).isSuccess());
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent atChain = awaitStopped();
    int threadId = atChain.getBody().getThreadId();
    StackTraceRequest stackTrace = new StackTraceRequest();
    StackTraceArguments stArgs = new StackTraceArguments();
    stArgs.setThreadId(threadId);
    stackTrace.setArguments(stArgs);

    // smart-step into test2 (skipping test1(1))
    StepIntoFunctionRequest smartStep = new StepIntoFunctionRequest();
    StepIntoFunctionArguments ssArgs = new StepIntoFunctionArguments();
    ssArgs.setThreadId(threadId);
    ssArgs.setClassName("Chain");
    ssArgs.setFunctionName("test2");
    ssArgs.setOccurrence(1);
    smartStep.setArguments(ssArgs);
    assertTrue("stepIntoFunction test2", request(smartStep).isSuccess());
    awaitStopped();
    StackFrame inTest2 = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertTrue("in test2 (was " + inTest2.getName() + ")", inTest2.getName().endsWith("test2"));

    // Eval has NO caller-position stop between chained calls, so a literal
    // "stand on the line again" cannot exist; step-out instead HOPS: one
    // press = the entry of the next chain element, never running it silently.
    StepOutRequest stepOut = new StepOutRequest();
    StepOutArguments soArgs = new StepOutArguments();
    soArgs.setThreadId(threadId);
    stepOut.setArguments(soArgs);
    assertTrue("stepOut", request(stepOut).isSuccess());
    awaitStopped();
    StackFrame hop1 = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertTrue("one step-out hops to test3's entry (was " + hop1.getName() + " line " + hop1.getLine() + ")",
               hop1.getName().endsWith("test3"));

    assertTrue("stepOut", request(stepOut).isSuccess());
    awaitStopped();
    StackFrame hop2 = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertTrue("next step-out hops to test1(2)'s entry (was " + hop2.getName() + ")",
               hop2.getName().endsWith("test1"));

    assertTrue("stepOut", request(stepOut).isSuccess());
    awaitStopped();
    StackFrame afterChain = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertTrue("final step-out returns to chain() (was " + afterChain.getName() + ")",
               afterChain.getName().endsWith("chain"));
    assertEquals("on the line after the chain", CHAIN_AFTER_LINE, afterChain.getLine());
  }

  @Test
  public void expressionSteppingModeStepsOneSubExpressionWithSpans() throws Exception {
    // toggle ON: a single DAP stepIn performs ONE raw interpreter sub-step
    // (same line, position advanced) and the frame carries the exact span
    // (endColumn) of the expression about to run - the highlight's data.
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue("initialize", request(initialize).isSuccess());
    dapClient.pollEvent(TIMEOUT);
    assertTrue("launch", request(new LaunchRequest()).isSuccess());

    String fixture = fixtureDir().resolve("EvalMain.hx").toString();
    SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
    SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(fixture);
    bpArgs.setSource(source);
    SourceBreakpoint breakpoint = new SourceBreakpoint();
    breakpoint.setLine(NESTED_CALL_LINE);
    bpArgs.setBreakpoints(List.of(breakpoint));
    setBreakpoints.setArguments(bpArgs);
    assertTrue("setBreakpoints", request(setBreakpoints).isSuccess());
    assertTrue("expression stepping ON", request(SetExpressionSteppingRequest.of(true)).isSuccess());
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent atCall = awaitStopped();
    int threadId = atCall.getBody().getThreadId();
    StackTraceRequest stackTrace = new StackTraceRequest();
    StackTraceArguments stArgs = new StackTraceArguments();
    stArgs.setThreadId(threadId);
    stackTrace.setArguments(stArgs);
    StackFrame before = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertNotNull("expression span present at the stop", before.getEndColumn());

    StepInRequest stepIn = new StepInRequest();
    StepInArguments siArgs = new StepInArguments();
    siArgs.setThreadId(threadId);
    stepIn.setArguments(siArgs);
    assertTrue("raw stepIn", request(stepIn).isSuccess());
    awaitStopped();
    StackFrame after = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertTrue("still in main (one SUB-step, not a callee: was " + after.getName() + ")",
               after.getName().endsWith("main"));
    assertEquals("same line", NESTED_CALL_LINE, after.getLine());
    assertTrue("position advanced within the line (col " + before.getColumn() + " -> " + after.getColumn() + ")",
               after.getColumn() != before.getColumn());
    assertNotNull("span still present", after.getEndColumn());
  }

  @Test
  public void setVariableEditsALocalThroughTheVariablesView() throws Exception {
    // the variables view's inline Set Value: DAP setVariable against the
    // scope's variablesReference, answered with the variable's NEW state,
    // and the change must actually stick in the debuggee
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue("initialize", request(initialize).isSuccess());
    dapClient.pollEvent(TIMEOUT);
    assertTrue("launch", request(new LaunchRequest()).isSuccess());

    String fixture = fixtureDir().resolve("EvalMain.hx").toString();
    SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
    SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(fixture);
    bpArgs.setSource(source);
    SourceBreakpoint breakpoint = new SourceBreakpoint();
    breakpoint.setLine(BREAK_LINE);
    bpArgs.setBreakpoints(List.of(breakpoint));
    setBreakpoints.setArguments(bpArgs);
    assertTrue("setBreakpoints", request(setBreakpoints).isSuccess());
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent stopped = awaitStopped();
    int threadId = stopped.getBody().getThreadId();
    StackTraceRequest stackTrace = new StackTraceRequest();
    StackTraceArguments stArgs = new StackTraceArguments();
    stArgs.setThreadId(threadId);
    stackTrace.setArguments(stArgs);
    StackFrame top = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);

    ScopesRequest scopes = new ScopesRequest();
    ScopesArguments scArgs = new ScopesArguments();
    scArgs.setFrameId(top.getId());
    scopes.setArguments(scArgs);
    ScopesResponse scResponse = (ScopesResponse)request(scopes);
    assertTrue("scopes", scResponse.isSuccess());

    // find the scope that holds the local 'greeting'
    Integer greetingScope = null;
    for (Scope scope : scResponse.getBody().getScopes()) {
      VariablesRequest variables = new VariablesRequest();
      VariablesArguments vArgs = new VariablesArguments();
      vArgs.setVariablesReference(scope.getVariablesReference());
      variables.setArguments(vArgs);
      VariablesResponse vResponse = (VariablesResponse)request(variables);
      assertTrue("variables of scope " + scope.getName(), vResponse.isSuccess());
      for (Variable variable : vResponse.getBody().getVariables()) {
        if ("greeting".equals(variable.getName())) {
          greetingScope = scope.getVariablesReference();
        }
      }
    }
    assertNotNull("found the scope holding 'greeting'", greetingScope);

    SetVariableRequest setVariable = new SetVariableRequest();
    SetVariableArguments svArgs = new SetVariableArguments();
    svArgs.setVariablesReference(greetingScope);
    svArgs.setName("greeting");
    svArgs.setValue("\"edited\";"); // trailing ';' must be cleaned like evaluate's
    setVariable.setArguments(svArgs);
    SetVariableResponse svResponse = (SetVariableResponse)request(setVariable);
    assertTrue("setVariable succeeded: " + svResponse.getMessage(), svResponse.isSuccess());
    assertTrue("response carries the NEW value (was " + svResponse.getBody().getValue() + ")",
               svResponse.getBody().getValue().contains("edited"));

    // the edit must be visible to the debuggee, not just echoed back
    EvaluateRequest evaluate = new EvaluateRequest();
    EvaluateArguments eArgs = new EvaluateArguments();
    eArgs.setExpression("greeting");
    eArgs.setFrameId(top.getId());
    evaluate.setArguments(eArgs);
    EvaluateResponse eResponse = (EvaluateResponse)request(evaluate);
    assertTrue("evaluate after the edit", eResponse.isSuccess());
    assertTrue("the edit stuck (was " + eResponse.getBody().getResult() + ")",
               eResponse.getBody().getResult().contains("edited"));
  }

  @Test
  public void setVariableEditsAnArrayElementByItsBracketName() throws Exception {
    // array children are named "[0]"/"[1]"/... by the VM and edited against
    // the ARRAY's variablesReference with that bracket name — exactly what
    // the variables view sends for an element row
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue("initialize", request(initialize).isSuccess());
    dapClient.pollEvent(TIMEOUT);
    assertTrue("launch", request(new LaunchRequest()).isSuccess());

    String fixture = fixtureDir().resolve("EvalMain.hx").toString();
    SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
    SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(fixture);
    bpArgs.setSource(source);
    SourceBreakpoint breakpoint = new SourceBreakpoint();
    breakpoint.setLine(COLL_LINE);
    bpArgs.setBreakpoints(List.of(breakpoint));
    setBreakpoints.setArguments(bpArgs);
    assertTrue("setBreakpoints", request(setBreakpoints).isSuccess());
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent stopped = awaitStopped();
    int threadId = stopped.getBody().getThreadId();
    StackTraceRequest stackTrace = new StackTraceRequest();
    StackTraceArguments stArgs = new StackTraceArguments();
    stArgs.setThreadId(threadId);
    stackTrace.setArguments(stArgs);
    StackFrame top = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);

    ScopesRequest scopes = new ScopesRequest();
    ScopesArguments scArgs = new ScopesArguments();
    scArgs.setFrameId(top.getId());
    scopes.setArguments(scArgs);
    ScopesResponse scResponse = (ScopesResponse)request(scopes);
    assertTrue("scopes", scResponse.isSuccess());

    Variable items = null;
    for (Scope scope : scResponse.getBody().getScopes()) {
      VariablesRequest variables = new VariablesRequest();
      VariablesArguments vArgs = new VariablesArguments();
      vArgs.setVariablesReference(scope.getVariablesReference());
      variables.setArguments(vArgs);
      for (Variable variable : ((VariablesResponse)request(variables)).getBody().getVariables()) {
        if ("items".equals(variable.getName())) {
          items = variable;
        }
      }
    }
    assertNotNull("found the 'items' array local", items);
    assertTrue("items is expandable", items.getVariablesReference() > 0);

    // the element rows carry the VM's bracket names
    VariablesRequest elements = new VariablesRequest();
    VariablesArguments elArgs = new VariablesArguments();
    elArgs.setVariablesReference(items.getVariablesReference());
    elements.setArguments(elArgs);
    List<Variable> children = ((VariablesResponse)request(elements)).getBody().getVariables();
    assertEquals("three elements", 3, children.size());
    assertEquals("bracket-named element", "[1]", children.get(1).getName());

    SetVariableRequest setVariable = new SetVariableRequest();
    SetVariableArguments svArgs = new SetVariableArguments();
    svArgs.setVariablesReference(items.getVariablesReference());
    svArgs.setName("[1]");
    svArgs.setValue("99");
    setVariable.setArguments(svArgs);
    SetVariableResponse svResponse = (SetVariableResponse)request(setVariable);
    assertTrue("setVariable on the element succeeded: " + svResponse.getMessage(), svResponse.isSuccess());
    assertEquals("response carries the new element value", "99", svResponse.getBody().getValue());

    EvaluateRequest evaluate = new EvaluateRequest();
    EvaluateArguments eArgs = new EvaluateArguments();
    eArgs.setExpression("items[1]");
    eArgs.setFrameId(top.getId());
    evaluate.setArguments(eArgs);
    EvaluateResponse eResponse = (EvaluateResponse)request(evaluate);
    assertTrue("evaluate after the edit", eResponse.isSuccess());
    assertEquals("the element edit stuck", "99", eResponse.getBody().getResult());

    // replacing the WHOLE array through its scope: the response must carry a
    // FRESH variablesReference whose children are the new elements — the
    // view adopts it, or an expanded row keeps showing the old array
    Integer itemsScope = null;
    for (Scope scope : ((ScopesResponse)request(scopesRequestFor(top.getId()))).getBody().getScopes()) {
      for (Variable variable : requestChildren(scope.getVariablesReference())) {
        if ("items".equals(variable.getName())) {
          itemsScope = scope.getVariablesReference();
        }
      }
    }
    assertNotNull("scope holding items", itemsScope);
    SetVariableRequest replaceAll = new SetVariableRequest();
    SetVariableArguments raArgs = new SetVariableArguments();
    raArgs.setVariablesReference(itemsScope);
    raArgs.setName("items");
    raArgs.setValue("[0, 10, 30]");
    replaceAll.setArguments(raArgs);
    SetVariableResponse raResponse = (SetVariableResponse)request(replaceAll);
    assertTrue("whole-array replace succeeded: " + raResponse.getMessage(), raResponse.isSuccess());
    int freshReference = raResponse.getBody().getVariablesReference();
    assertTrue("replacement carries a fresh expandable reference", freshReference > 0);
    List<Variable> fresh = requestChildren(freshReference);
    assertEquals("new array has three elements", 3, fresh.size());
    assertEquals("new middle element", "10", fresh.get(1).getValue());
  }

  @Test
  public void settingAStringsDerivedRowsIsRefusedWithoutTouchingTheVm() throws Exception {
    // a String expands to length/byteLength; WRITING those crashes the eval
    // VM ("Cannot run Haxe code in a non-Haxe thread" assert, then a 10s
    // timeout killed the session — user-reported). The adapter must refuse
    // fast, and the VM must stay healthy afterwards.
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue("initialize", request(initialize).isSuccess());
    dapClient.pollEvent(TIMEOUT);
    assertTrue("launch", request(new LaunchRequest()).isSuccess());

    String fixture = fixtureDir().resolve("EvalMain.hx").toString();
    SetBreakpointsRequest setBreakpoints = new SetBreakpointsRequest();
    SetBreakpointsArguments bpArgs = new SetBreakpointsArguments();
    Source source = new Source();
    source.setPath(fixture);
    bpArgs.setSource(source);
    SourceBreakpoint breakpoint = new SourceBreakpoint();
    breakpoint.setLine(BREAK_LINE);
    bpArgs.setBreakpoints(List.of(breakpoint));
    setBreakpoints.setArguments(bpArgs);
    assertTrue("setBreakpoints", request(setBreakpoints).isSuccess());
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent stopped = awaitStopped();
    StackTraceRequest stackTrace = new StackTraceRequest();
    StackTraceArguments stArgs = new StackTraceArguments();
    stArgs.setThreadId(stopped.getBody().getThreadId());
    stackTrace.setArguments(stArgs);
    StackFrame top = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);

    // the String local 'greeting' hands out an expandable reference
    EvaluateRequest evaluate = new EvaluateRequest();
    EvaluateArguments eArgs = new EvaluateArguments();
    eArgs.setExpression("greeting");
    eArgs.setFrameId(top.getId());
    evaluate.setArguments(eArgs);
    EvaluateResponse eResponse = (EvaluateResponse)request(evaluate);
    assertTrue("evaluate greeting", eResponse.isSuccess());
    int stringReference = eResponse.getBody().getVariablesReference();
    assertTrue("a String is expandable in eval", stringReference > 0);

    SetVariableRequest write = new SetVariableRequest();
    SetVariableArguments wArgs = new SetVariableArguments();
    wArgs.setVariablesReference(stringReference);
    wArgs.setName("length");
    wArgs.setValue("9");
    write.setArguments(wArgs);
    long before = System.currentTimeMillis();
    Response refused = request(write);
    long elapsed = System.currentTimeMillis() - before;
    assertFalse("the write is refused", refused.isSuccess());
    assertTrue("refused FAST, not via a VM timeout (was " + elapsed + "ms)", elapsed < 5_000);

    // and the VM survived: a normal request still answers
    EvaluateResponse after = (EvaluateResponse)request(evaluate);
    assertTrue("VM still healthy after the refused write", after.isSuccess());
  }

  private ScopesRequest scopesRequestFor(int frameId) {
    ScopesRequest scopes = new ScopesRequest();
    ScopesArguments scArgs = new ScopesArguments();
    scArgs.setFrameId(frameId);
    scopes.setArguments(scArgs);
    return scopes;
  }

  private List<Variable> requestChildren(int variablesReference) throws Exception {
    VariablesRequest variables = new VariablesRequest();
    VariablesArguments vArgs = new VariablesArguments();
    vArgs.setVariablesReference(variablesReference);
    variables.setArguments(vArgs);
    return ((VariablesResponse)request(variables)).getBody().getVariables();
  }
}
