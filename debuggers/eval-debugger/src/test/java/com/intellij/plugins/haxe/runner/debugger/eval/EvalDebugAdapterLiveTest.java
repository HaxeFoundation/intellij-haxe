package com.intellij.plugins.haxe.runner.debugger.eval;

import com.intellij.plugins.haxe.runner.debugger.dap.DapPaths;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives {@link EvalDebugAdapter} end to end: a real {@link DapClient} on one
 * side, the REAL haxe eval VM (spawned with -D eval-debugger) on the other.
 * The full IDE flow — initialize, launch, breakpoints, configurationDone,
 * stop, stack/scopes/variables, evaluate, step, resume, terminate — against
 * the fixture in test-fixtures/EvalMain.hx. Skips when haxe is not on PATH.
 */
public class EvalDebugAdapterLiveTest extends EvalLiveTestBase {

  private static final int BREAK_LINE = 10;
  private static final int NESTED_CALL_LINE = 11; // `var nested = outer(inner(3));`
  private static final int INNER_LINE = 16;       // first EXECUTABLE line inside inner() (the return)
  private static final int OUTER_LINE = 20;       // first executable line inside outer()
  private static final int CHAIN_LINE = 25;       // `cfg.test1(1).test2().test3().test1(2);`
  private static final int CHAIN_AFTER_LINE = 26; // the println after the chain
  private static final int COLL_LINE = 51;        // Coll.collections println (items array live)

  @Test
  public void fullSessionBreakpointInspectStepAndFinish() throws Exception {
    InitializeRequest initialize = new InitializeRequest();
    InitializeRequestArguments initArgs = new InitializeRequestArguments();
    initArgs.setAdapterID("intellij-haxe-eval");
    initialize.setArguments(initArgs);
    assertTrue(request(initialize).isSuccess(), "initialize");
    assertNotNull(dapClient.pollEvent(TIMEOUT), "initialized event");

    launch();

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
    assertTrue(request(setBreakpoints).isSuccess(), "setBreakpoints");

    assertTrue(request(new ConfigurationDoneRequest()).isSuccess(), "configurationDone releases the waiting VM");

    StoppedEvent stopped = awaitStopped();
    assertEquals("breakpoint", stopped.getBody().getReason(), "stopped by the breakpoint");
    int threadId = stopped.getBody().getThreadId();

    assertTrue(request(new ThreadsRequest()).isSuccess(), "threads");

    StackTraceRequest stackTrace = stackTraceRequest(threadId);
    StackTraceResponse stResponse = (StackTraceResponse)request(stackTrace);
    assertTrue(stResponse.isSuccess(), "stackTrace");
    List<StackFrame> frames = stResponse.getBody().getStackFrames();
    assertFalse(frames.isEmpty(), "frames at the stop");

    StackFrame top = frames.get(0);
    assertEquals(BREAK_LINE, top.getLine(), "stopped on the break line");
    assertNotNull(top.getSource(), "top frame has a source");
    assertTrue(DapPaths.toForwardSlashes(top.getSource().getPath()).endsWith("EvalMain.hx"), "top frame is the fixture");

    ScopesRequest scopes = scopesRequest(top.getId());
    ScopesResponse scResponse = (ScopesResponse)request(scopes);
    assertTrue(scResponse.isSuccess(), "scopes");
    assertFalse(scResponse.getBody().getScopes().isEmpty(), "scopes present");

    boolean sawGreeting = false;

    for (Scope scope : scResponse.getBody().getScopes()) {
      VariablesRequest variables = variablesRequest(scope.getVariablesReference());
      VariablesResponse vResponse = (VariablesResponse)request(variables);
      assertTrue(vResponse.isSuccess(), "variables of scope " + scope.getName());

      for (Variable variable : vResponse.getBody().getVariables()) {
        if ("greeting".equals(variable.getName())) {
          sawGreeting = true;
          assertTrue(variable.getValue().contains("hello"), "greeting holds its value (was " + variable.getValue() + ")");
        }
      }
    }
    assertTrue(sawGreeting, "local 'greeting' visible through DAP");

    EvaluateRequest evaluate = evaluateRequest(top.getId(), "greeting.length + 1");
    EvaluateResponse eResponse = (EvaluateResponse)request(evaluate);
    assertTrue(eResponse.isSuccess(), "evaluate");
    assertEquals("6", eResponse.getBody().getResult(), "greeting.length + 1 == 6");

    // step over the break line and land on the next one, still in main
    NextRequest next = nextRequest(threadId);
    assertTrue(request(next).isSuccess(), "next");
    StoppedEvent afterStep = awaitStopped();
    StackTraceResponse stepStack = (StackTraceResponse)request(stackTrace);
    assertEquals(BREAK_LINE + 1, stepStack.getBody().getStackFrames().get(0).getLine(), "landed on the line after the breakpoint");

    ContinueRequest resume = continueRequest(afterStep.getBody().getThreadId());
    Response resumeResponse = request(resume);
    assertTrue(resumeResponse.isSuccess(), "continue failed: " + resumeResponse.getMessage());

    awaitTerminated();
    assertTrue(haxe.waitFor(TIMEOUT, TimeUnit.MILLISECONDS), "haxe exited");
    assertEquals(0, haxe.exitValue(), "clean exit");
  }

  @Test
  public void singleStepIntoEntersTheNestedCallee() throws Exception {
    // `var nested = outer(inner(3));` — eval's raw stepIn is sub-expression
    // granular (it stops on each column of the line before entering a callee),
    // so a plain step-into would need several presses. The adapter coalesces
    // those same-line sub-steps: ONE DAP step-into must land inside inner().
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue(request(initialize).isSuccess(), "initialize");
    dapClient.pollEvent(TIMEOUT);
    launch();

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
    assertTrue(request(setBreakpoints).isSuccess(), "setBreakpoints");
    configurationDone();

    StoppedEvent atCall = awaitStopped();
    int threadId = atCall.getBody().getThreadId();
    StackTraceRequest stackTrace = stackTraceRequest(threadId);
    StackFrame atBreak = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertEquals(NESTED_CALL_LINE, atBreak.getLine(), "stopped on the nested-call line");
    assertTrue(atBreak.getName().endsWith("main"), "stopped in main");

    StepInRequest stepIn = stepInRequest(threadId);
    assertTrue(request(stepIn).isSuccess(), "stepIn");

    StoppedEvent afterStep = awaitStopped();
    assertEquals("step", afterStep.getBody().getReason(), "step stop");
    StackFrame landed = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertTrue(landed.getName().endsWith("inner"), "ONE step-into entered inner() (was " + landed.getName() + " line " + landed.getLine() + ")");
    assertEquals(INNER_LINE, landed.getLine(), "landed inside inner()");
  }

  @Test
  public void smartStepIntoSkipsToTheChosenCallee() throws Exception {
    // `var nested = outer(inner(3));` — smart-step to OUTER must walk the
    // line's sub-expressions, step THROUGH inner() without reporting it, and
    // land inside outer() (the eval protocol has no smart-step; the adapter
    // emulates custom/stepIntoFunction with sub-expression steps).
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue(request(initialize).isSuccess(), "initialize");
    dapClient.pollEvent(TIMEOUT);
    launch();

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
    assertTrue(request(setBreakpoints).isSuccess(), "setBreakpoints");
    configurationDone();

    StoppedEvent atCall = awaitStopped();
    int threadId = atCall.getBody().getThreadId();
    StepIntoFunctionRequest smartStep = new StepIntoFunctionRequest();
    StepIntoFunctionArguments ssArgs = new StepIntoFunctionArguments();
    ssArgs.setThreadId(threadId);
    ssArgs.setClassName("EvalMain");
    ssArgs.setFunctionName("outer");
    ssArgs.setOccurrence(1);
    smartStep.setArguments(ssArgs);
    assertTrue(request(smartStep).isSuccess(), "stepIntoFunction");

    StoppedEvent afterStep = awaitStopped();
    assertEquals("step", afterStep.getBody().getReason(), "step stop");
    StackTraceRequest stackTrace = stackTraceRequest(threadId);
    StackFrame landed = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertTrue(landed.getName().endsWith("outer"), "smart-step landed in outer(), skipping inner() (was "
               + landed.getName() + " line " + landed.getLine() + ")");
    assertEquals(OUTER_LINE, landed.getLine(), "on outer's executable line");
  }

  @Test
  public void chainedCallsStepOutReturnsMidLineForTheNextPick() throws Exception {
    // `cfg.test1(1).test2().test3().test1(2);` — the user-reported flow:
    // smart-step into test2, step OUT, and be back ON THE CHAIN LINE (not
    // dragged through test3/test1) to pick the next call or leave the line.
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue(request(initialize).isSuccess(), "initialize");
    dapClient.pollEvent(TIMEOUT);
    launch();

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
    assertTrue(request(setBreakpoints).isSuccess(), "setBreakpoints");
    configurationDone();

    StoppedEvent atChain = awaitStopped();
    int threadId = atChain.getBody().getThreadId();
    StackTraceRequest stackTrace = stackTraceRequest(threadId);

    // smart-step into test2 (skipping test1(1))
    StepIntoFunctionRequest smartStep = new StepIntoFunctionRequest();
    StepIntoFunctionArguments ssArgs = new StepIntoFunctionArguments();
    ssArgs.setThreadId(threadId);
    ssArgs.setClassName("Chain");
    ssArgs.setFunctionName("test2");
    ssArgs.setOccurrence(1);
    smartStep.setArguments(ssArgs);

    assertTrue(request(smartStep).isSuccess(), "stepIntoFunction test2");
    awaitStopped();

    StackFrame inTest2 = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertTrue(inTest2.getName().endsWith("test2"), "in test2 (was " + inTest2.getName() + ")");

    // Eval has NO caller-position stop between chained calls, so a literal
    // "stand on the line again" cannot exist; step-out instead HOPS: one
    // press = the entry of the next chain element, never running it silently.
    StepOutRequest stepOut = stepOutRequest(threadId);
    assertTrue(request(stepOut).isSuccess(), "stepOut");
    awaitStopped();
    StackFrame hop1 = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertTrue(hop1.getName().endsWith("test3"), "one step-out hops to test3's entry (was " + hop1.getName() + " line " + hop1.getLine() + ")");

    assertTrue(request(stepOut).isSuccess(), "stepOut");
    awaitStopped();
    StackFrame hop2 = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertTrue(hop2.getName().endsWith("test1"), "next step-out hops to test1(2)'s entry (was " + hop2.getName() + ")");

    assertTrue(request(stepOut).isSuccess(), "stepOut");
    awaitStopped();
    StackFrame afterChain = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertTrue(afterChain.getName().endsWith("chain"), "final step-out returns to chain() (was " + afterChain.getName() + ")");
    assertEquals(CHAIN_AFTER_LINE, afterChain.getLine(), "on the line after the chain");
  }

  @Test
  public void expressionSteppingModeStepsOneSubExpressionWithSpans() throws Exception {
    // toggle ON: a single DAP stepIn performs ONE raw interpreter sub-step
    // (same line, position advanced) and the frame carries the exact span
    // (endColumn) of the expression about to run - the highlight's data.
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue(request(initialize).isSuccess(), "initialize");
    dapClient.pollEvent(TIMEOUT);
    launch();

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
    assertTrue(request(setBreakpoints).isSuccess(), "setBreakpoints");
    assertTrue(request(SetExpressionSteppingRequest.of(true)).isSuccess(), "expression stepping ON");
    configurationDone();

    StoppedEvent atCall = awaitStopped();
    int threadId = atCall.getBody().getThreadId();
    StackTraceRequest stackTrace = stackTraceRequest(threadId);
    StackFrame before = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertNotNull(before.getEndColumn(), "expression span present at the stop");

    StepInRequest stepIn = stepInRequest(threadId);

    assertTrue(request(stepIn).isSuccess(), "raw stepIn");
    awaitStopped();

    StackFrame after = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    assertTrue(after.getName().endsWith("main"), "still in main (one SUB-step, not a callee: was " + after.getName() + ")");
    assertEquals(NESTED_CALL_LINE, after.getLine(), "same line");
    assertTrue(after.getColumn() != before.getColumn(), "position advanced within the line (col " + before.getColumn() + " -> " + after.getColumn() + ")");
    assertNotNull(after.getEndColumn(), "span still present");
  }

  @Test
  public void setVariableEditsALocalThroughTheVariablesView() throws Exception {
    // the variables view's inline Set Value: DAP setVariable against the
    // scope's variablesReference, answered with the variable's NEW state,
    // and the change must actually stick in the debuggee
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue(request(initialize).isSuccess(), "initialize");
    dapClient.pollEvent(TIMEOUT);
    launch();

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
    assertTrue(request(setBreakpoints).isSuccess(), "setBreakpoints");
    configurationDone();

    StoppedEvent stopped = awaitStopped();
    int threadId = stopped.getBody().getThreadId();
    StackTraceRequest stackTrace = stackTraceRequest(threadId);
    StackFrame top = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    ScopesRequest scopes = scopesRequest(top.getId());
    ScopesResponse scResponse = (ScopesResponse)request(scopes);
    assertTrue(scResponse.isSuccess(), "scopes");

    // find the scope that holds the local 'greeting'
    Integer greetingScope = null;

    for (Scope scope : scResponse.getBody().getScopes()) {
      VariablesRequest variables = variablesRequest(scope.getVariablesReference());
      VariablesResponse vResponse = (VariablesResponse)request(variables);
      assertTrue(vResponse.isSuccess(), "variables of scope " + scope.getName());

      for (Variable variable : vResponse.getBody().getVariables()) {
        if ("greeting".equals(variable.getName())) {
          greetingScope = scope.getVariablesReference();
        }
      }
    }
    assertNotNull(greetingScope, "found the scope holding 'greeting'");

    // the trailing ';' must be cleaned like evaluate's
    SetVariableRequest setVariable = setVariableRequest(greetingScope, "greeting", "\"edited\";");
    SetVariableResponse svResponse = (SetVariableResponse)request(setVariable);
    assertTrue(svResponse.isSuccess(), "setVariable succeeded: " + svResponse.getMessage());
    assertTrue(svResponse.getBody().getValue().contains("edited"), "response carries the NEW value (was " + svResponse.getBody().getValue() + ")");

    // the edit must be visible to the debuggee, not just echoed back
    EvaluateRequest evaluate = evaluateRequest(top.getId(), "greeting");
    EvaluateResponse eResponse = (EvaluateResponse)request(evaluate);
    assertTrue(eResponse.isSuccess(), "evaluate after the edit");
    assertTrue(eResponse.getBody().getResult().contains("edited"), "the edit stuck (was " + eResponse.getBody().getResult() + ")");
  }

  @Test
  public void setVariableEditsAnArrayElementByItsBracketName() throws Exception {
    // array children are named "[0]"/"[1]"/... by the VM and edited against
    // the ARRAY's variablesReference with that bracket name — exactly what
    // the variables view sends for an element row
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue(request(initialize).isSuccess(), "initialize");
    dapClient.pollEvent(TIMEOUT);
    launch();

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
    assertTrue(request(setBreakpoints).isSuccess(), "setBreakpoints");
    configurationDone();

    StoppedEvent stopped = awaitStopped();
    int threadId = stopped.getBody().getThreadId();
    StackTraceRequest stackTrace = stackTraceRequest(threadId);
    StackFrame top = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);
    ScopesRequest scopes = scopesRequest(top.getId());
    ScopesResponse scResponse = (ScopesResponse)request(scopes);
    assertTrue(scResponse.isSuccess(), "scopes");

    Variable items = null;
    for (Scope scope : scResponse.getBody().getScopes()) {
      VariablesRequest variables = variablesRequest(scope.getVariablesReference());
      for (Variable variable : ((VariablesResponse)request(variables)).getBody().getVariables()) {
        if ("items".equals(variable.getName())) {
          items = variable;
        }
      }
    }
    assertNotNull(items, "found the 'items' array local");
    assertTrue(items.getVariablesReference() > 0, "items is expandable");

    // the element rows carry the VM's bracket names
    VariablesRequest elements = variablesRequest(items.getVariablesReference());
    List<Variable> children = ((VariablesResponse)request(elements)).getBody().getVariables();
    assertEquals(3, children.size(), "three elements");
    assertEquals("[1]", children.get(1).getName(), "bracket-named element");

    SetVariableRequest setVariable = setVariableRequest(items.getVariablesReference(), "[1]", "99");
    SetVariableResponse svResponse = (SetVariableResponse)request(setVariable);
    assertTrue(svResponse.isSuccess(), "setVariable on the element succeeded: " + svResponse.getMessage());
    assertEquals("99", svResponse.getBody().getValue(), "response carries the new element value");

    EvaluateRequest evaluate = evaluateRequest(top.getId(), "items[1]");
    EvaluateResponse eResponse = (EvaluateResponse)request(evaluate);
    assertTrue(eResponse.isSuccess(), "evaluate after the edit");
    assertEquals("99", eResponse.getBody().getResult(), "the element edit stuck");

    // replacing the WHOLE array through its scope: the response must carry a
    // FRESH variablesReference whose children are the new elements — the
    // view adopts it, or an expanded row keeps showing the old array
    Integer itemsScope = null;
    for (Scope scope : ((ScopesResponse)request(scopesRequest(top.getId()))).getBody().getScopes()) {
      for (Variable variable : requestChildren(scope.getVariablesReference())) {
        if ("items".equals(variable.getName())) {
          itemsScope = scope.getVariablesReference();
        }
      }
    }
    assertNotNull(itemsScope, "scope holding items");
    SetVariableRequest replaceAll = setVariableRequest(itemsScope, "items", "[0, 10, 30]");
    SetVariableResponse raResponse = (SetVariableResponse)request(replaceAll);
    assertTrue(raResponse.isSuccess(), "whole-array replace succeeded: " + raResponse.getMessage());
    int freshReference = raResponse.getBody().getVariablesReference();
    assertTrue(freshReference > 0, "replacement carries a fresh expandable reference");
    List<Variable> fresh = requestChildren(freshReference);
    assertEquals(3, fresh.size(), "new array has three elements");
    assertEquals("10", fresh.get(1).getValue(), "new middle element");
  }

  @Test
  public void settingAStringsDerivedRowsIsRefusedWithoutTouchingTheVm() throws Exception {
    // a String expands to length/byteLength; WRITING those crashes the eval
    // VM ("Cannot run Haxe code in a non-Haxe thread" assert, then a 10s
    // timeout killed the session — user-reported). The adapter must refuse
    // fast, and the VM must stay healthy afterwards.
    InitializeRequest initialize = new InitializeRequest();
    initialize.setArguments(new InitializeRequestArguments());
    assertTrue(request(initialize).isSuccess(), "initialize");
    dapClient.pollEvent(TIMEOUT);
    launch();

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
    assertTrue(request(setBreakpoints).isSuccess(), "setBreakpoints");
    configurationDone();

    StoppedEvent stopped = awaitStopped();
    StackTraceRequest stackTrace = stackTraceRequest(stopped.getBody().getThreadId());
    StackFrame top = ((StackTraceResponse)request(stackTrace)).getBody().getStackFrames().get(0);

    // the String local 'greeting' hands out an expandable reference
    EvaluateRequest evaluate = evaluateRequest(top.getId(), "greeting");
    EvaluateResponse eResponse = (EvaluateResponse)request(evaluate);
    assertTrue(eResponse.isSuccess(), "evaluate greeting");
    int stringReference = eResponse.getBody().getVariablesReference();
    assertTrue(stringReference > 0, "a String is expandable in eval");

    SetVariableRequest write = setVariableRequest(stringReference, "length", "9");
    long before = System.currentTimeMillis();
    Response refused = request(write);
    long elapsed = System.currentTimeMillis() - before;
    assertFalse(refused.isSuccess(), "the write is refused");
    assertTrue(elapsed < 5_000, "refused FAST, not via a VM timeout (was " + elapsed + "ms)");

    // and the VM survived: a normal request still answers
    EvaluateResponse after = (EvaluateResponse)request(evaluate);
    assertTrue(after.isSuccess(), "VM still healthy after the refused write");
  }

  @Override
  protected String fixtureMain() {
    return "EvalMain";
  }

  private List<Variable> requestChildren(int variablesReference) throws Exception {
    VariablesRequest variables = variablesRequest(variablesReference);
    return ((VariablesResponse)request(variables)).getBody().getVariables();
  }
}
