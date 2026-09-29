package com.intellij.plugins.haxe.runner.debugger.eval;

import com.intellij.plugins.haxe.runner.debugger.dap.DapPaths;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.client.DapClient;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Drives {@link EvalDebugAdapter} end to end: a real {@link DapClient} on one
 * side, the REAL haxe eval VM (spawned with -D eval-debugger) on the other.
 * The full IDE flow — initialize, launch, breakpoints, configurationDone,
 * stop, stack/scopes/variables, evaluate, step, resume, terminate — against
 * the fixture in test-fixtures/EvalMain.hx. Skips when haxe is not on PATH.
 */
@DisplayName("Eval debugger: debug adapter (live)")
public class EvalDebugAdapterLiveTest extends EvalLiveTestBase {

  private static final String FIXTURE = "EvalMain.hx";

  private static final int BREAK_LINE = 10;
  private static final int NESTED_CALL_LINE = 11; // `var nested = outer(inner(3));`
  private static final int INNER_LINE = 16;       // first EXECUTABLE line inside inner() (the return)
  private static final int OUTER_LINE = 20;       // first executable line inside outer()
  private static final int CHAIN_LINE = 25;       // `cfg.test1(1).test2().test3().test1(2);`
  private static final int CHAIN_AFTER_LINE = 26; // the println after the chain
  private static final int COLL_LINE = 51;        // Coll.collections println (items array live)

  @Test
  @DisplayName("full session breakpoint inspect step and finish")
  public void fullSessionBreakpointInspectStepAndFinish() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE, BREAK_LINE);
    int threadId = stopped.getBody().getThreadId();

    assertTrue(request(new ThreadsRequest()).isSuccess(), "threads");

    StackFrame top = topFrame(threadId);
    assertEquals(BREAK_LINE, top.getLine(), "stopped on the break line");
    assertNotNull(top.getSource(), "top frame has a source");
    assertTrue(DapPaths.toForwardSlashes(top.getSource().getPath()).endsWith(FIXTURE), "top frame is the fixture");

    Variable greeting = findLocal(top.getId(), "greeting").variable();
    assertTrue(greeting.getValue().contains("hello"), "greeting holds its value (was " + greeting.getValue() + ")");

    EvaluateRequest evaluate = evaluateRequest(top.getId(), "greeting.length + 1");
    EvaluateResponse eResponse = (EvaluateResponse)request(evaluate);
    assertTrue(eResponse.isSuccess(), "evaluate");
    assertEquals("6", eResponse.getBody().getResult(), "greeting.length + 1 == 6");

    // step over the break line and land on the next one, still in main
    assertTrue(request(nextRequest(threadId)).isSuccess(), "next");
    StoppedEvent afterStep = awaitStopped();
    assertEquals(BREAK_LINE + 1, topFrame(threadId).getLine(), "landed on the line after the breakpoint");

    ContinueRequest resume = continueRequest(afterStep.getBody().getThreadId());
    Response resumeResponse = request(resume);
    assertTrue(resumeResponse.isSuccess(), "continue failed: " + resumeResponse.getMessage());

    awaitTerminated();
    assertTrue(haxe.waitFor(TIMEOUT, TimeUnit.MILLISECONDS), "haxe exited");
    assertEquals(0, haxe.exitValue(), "clean exit");
  }

  @Test
  @DisplayName("single step into enters the nested callee")
  public void singleStepIntoEntersTheNestedCallee() throws Exception {
    // `var nested = outer(inner(3));` — eval's raw stepIn is sub-expression
    // granular (it stops on each column of the line before entering a callee),
    // so a plain step-into would need several presses. The adapter coalesces
    // those same-line sub-steps: ONE DAP step-into must land inside inner().
    int threadId = runToBreakpoint(FIXTURE, NESTED_CALL_LINE).getBody().getThreadId();

    StackFrame atBreak = topFrame(threadId);
    assertEquals(NESTED_CALL_LINE, atBreak.getLine(), "stopped on the nested-call line");
    assertTrue(atBreak.getName().endsWith("main"), "stopped in main");

    assertTrue(request(stepInRequest(threadId)).isSuccess(), "stepIn");

    StoppedEvent afterStep = awaitStopped();
    assertEquals("step", afterStep.getBody().getReason(), "step stop");
    StackFrame landed = topFrame(threadId);
    assertTrue(landed.getName().endsWith("inner"), "ONE step-into entered inner() (was " + landed.getName() + " line " + landed.getLine() + ")");
    assertEquals(INNER_LINE, landed.getLine(), "landed inside inner()");
  }

  @Test
  @DisplayName("smart step into skips to the chosen callee")
  public void smartStepIntoSkipsToTheChosenCallee() throws Exception {
    // `var nested = outer(inner(3));` — smart-step to OUTER must walk the
    // line's sub-expressions, step THROUGH inner() without reporting it, and
    // land inside outer() (the eval protocol has no smart-step; the adapter
    // emulates custom/stepIntoFunction with sub-expression steps).
    int threadId = runToBreakpoint(FIXTURE, NESTED_CALL_LINE).getBody().getThreadId();

    assertTrue(request(stepIntoFunctionRequest(threadId, "EvalMain", "outer")).isSuccess(), "stepIntoFunction");

    StoppedEvent afterStep = awaitStopped();
    assertEquals("step", afterStep.getBody().getReason(), "step stop");
    StackFrame landed = topFrame(threadId);
    assertTrue(landed.getName().endsWith("outer"), "smart-step landed in outer(), skipping inner() (was "
               + landed.getName() + " line " + landed.getLine() + ")");
    assertEquals(OUTER_LINE, landed.getLine(), "on outer's executable line");
  }

  @Test
  @DisplayName("chained calls step out returns mid line for the next pick")
  public void chainedCallsStepOutReturnsMidLineForTheNextPick() throws Exception {
    // `cfg.test1(1).test2().test3().test1(2);` — the user-reported flow:
    // smart-step into test2, step OUT, and be back ON THE CHAIN LINE (not
    // dragged through test3/test1) to pick the next call or leave the line.
    int threadId = runToBreakpoint(FIXTURE, CHAIN_LINE).getBody().getThreadId();

    // smart-step into test2 (skipping test1(1))
    assertTrue(request(stepIntoFunctionRequest(threadId, "Chain", "test2")).isSuccess(), "stepIntoFunction test2");
    awaitStopped();

    StackFrame inTest2 = topFrame(threadId);
    assertTrue(inTest2.getName().endsWith("test2"), "in test2 (was " + inTest2.getName() + ")");

    // Eval has NO caller-position stop between chained calls, so a literal
    // "stand on the line again" cannot exist; step-out instead HOPS: one
    // press = the entry of the next chain element, never running it silently.
    StepOutRequest stepOut = stepOutRequest(threadId);
    assertTrue(request(stepOut).isSuccess(), "stepOut");
    awaitStopped();
    StackFrame hop1 = topFrame(threadId);
    assertTrue(hop1.getName().endsWith("test3"), "one step-out hops to test3's entry (was " + hop1.getName() + " line " + hop1.getLine() + ")");

    assertTrue(request(stepOut).isSuccess(), "stepOut");
    awaitStopped();
    StackFrame hop2 = topFrame(threadId);
    assertTrue(hop2.getName().endsWith("test1"), "next step-out hops to test1(2)'s entry (was " + hop2.getName() + ")");

    assertTrue(request(stepOut).isSuccess(), "stepOut");
    awaitStopped();
    StackFrame afterChain = topFrame(threadId);
    assertTrue(afterChain.getName().endsWith("chain"), "final step-out returns to chain() (was " + afterChain.getName() + ")");
    assertEquals(CHAIN_AFTER_LINE, afterChain.getLine(), "on the line after the chain");
  }

  @Test
  @DisplayName("expression stepping mode steps one sub expression with spans")
  public void expressionSteppingModeStepsOneSubExpressionWithSpans() throws Exception {
    // toggle ON: a single DAP stepIn performs ONE raw interpreter sub-step
    // (same line, position advanced) and the frame carries the exact span
    // (endColumn) of the expression about to run - the highlight's data.
    initialize();
    launch();
    setBreakpoints(FIXTURE, NESTED_CALL_LINE);
    assertTrue(request(SetExpressionSteppingRequest.of(true)).isSuccess(), "expression stepping ON");
    configurationDone();

    int threadId = awaitStopped().getBody().getThreadId();
    StackFrame before = topFrame(threadId);
    assertNotNull(before.getEndColumn(), "expression span present at the stop");

    assertTrue(request(stepInRequest(threadId)).isSuccess(), "raw stepIn");
    awaitStopped();

    StackFrame after = topFrame(threadId);
    assertTrue(after.getName().endsWith("main"), "still in main (one SUB-step, not a callee: was " + after.getName() + ")");
    assertEquals(NESTED_CALL_LINE, after.getLine(), "same line");
    assertTrue(after.getColumn() != before.getColumn(), "position advanced within the line (col " + before.getColumn() + " -> " + after.getColumn() + ")");
    assertNotNull(after.getEndColumn(), "span still present");
  }

  @Test
  @DisplayName("set variable edits a local through the variables view")
  public void setVariableEditsALocalThroughTheVariablesView() throws Exception {
    // the variables view's inline Set Value: DAP setVariable against the
    // scope's variablesReference, answered with the variable's NEW state,
    // and the change must actually stick in the debuggee
    int threadId = runToBreakpoint(FIXTURE, BREAK_LINE).getBody().getThreadId();
    StackFrame top = topFrame(threadId);
    int greetingScope = findLocal(top.getId(), "greeting").scopeReference();

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
  @DisplayName("set variable edits an array element by its bracket name")
  public void setVariableEditsAnArrayElementByItsBracketName() throws Exception {
    // array children are named "[0]"/"[1]"/... by the VM and edited against
    // the ARRAY's variablesReference with that bracket name — exactly what
    // the variables view sends for an element row
    int threadId = runToBreakpoint(FIXTURE, COLL_LINE).getBody().getThreadId();
    StackFrame top = topFrame(threadId);

    Variable items = findLocal(top.getId(), "items").variable();
    assertTrue(items.getVariablesReference() > 0, "items is expandable");

    // the element rows carry the VM's bracket names
    List<Variable> children = requestChildren(items.getVariablesReference());
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
  }

  @Test
  @DisplayName("whole array replacement carries a fresh reference")
  public void wholeArrayReplacementCarriesAFreshReference() throws Exception {
    // replacing the WHOLE array through its scope: the response must carry a
    // FRESH variablesReference whose children are the new elements — the
    // view adopts it, or an expanded row keeps showing the old array
    int threadId = runToBreakpoint(FIXTURE, COLL_LINE).getBody().getThreadId();
    int itemsScope = findLocal(topFrame(threadId).getId(), "items").scopeReference();

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
  @DisplayName("setting a strings derived rows is refused without touching the vm")
  public void settingAStringsDerivedRowsIsRefusedWithoutTouchingTheVm() throws Exception {
    // a String expands to length/byteLength; WRITING those crashes the eval
    // VM ("Cannot run Haxe code in a non-Haxe thread" assert, then a 10s
    // timeout killed the session — user-reported). The adapter must refuse
    // fast, and the VM must stay healthy afterwards.
    int threadId = runToBreakpoint(FIXTURE, BREAK_LINE).getBody().getThreadId();
    StackFrame top = topFrame(threadId);

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

  private static StepIntoFunctionRequest stepIntoFunctionRequest(int threadId, String className, String functionName) {
    StepIntoFunctionRequest request = new StepIntoFunctionRequest();
    StepIntoFunctionArguments arguments = new StepIntoFunctionArguments();
    arguments.setThreadId(threadId);
    arguments.setClassName(className);
    arguments.setFunctionName(functionName);
    arguments.setOccurrence(1);
    request.setArguments(arguments);
    return request;
  }

  private List<Variable> requestChildren(int variablesReference) throws Exception {
    VariablesRequest variables = variablesRequest(variablesReference);
    return ((VariablesResponse)request(variables)).getBody().getVariables();
  }
}
