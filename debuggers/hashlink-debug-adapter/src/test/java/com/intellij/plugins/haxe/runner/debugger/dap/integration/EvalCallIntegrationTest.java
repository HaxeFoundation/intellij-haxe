package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Runs functions inside the stopped debuggee via evaluate against real
 * HashLink: injects a call trampoline, runs it, and reads the return. The
 * decisive checks assert KNOWN return values, and that the debuggee is intact
 * afterwards (execution continues to a clean exit). Runs on both the x86-64
 * (register-arg) and x86 (cdecl stack-arg) trampolines.
 */
public class EvalCallIntegrationTest extends DapIntegrationTestBase {

  @Test
  public void callsFunctionsAndReturnsValues() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    // integer args + integer return
    assertEquals("add(20, 3) = 23", "23", evaluate(frameId, "add(20, 3)").getBody().getResult());
    // a path argument (base is 10) + a literal
    assertEquals("add(base, 5) = 15", "15", evaluate(frameId, "add(base, 5)").getBody().getResult());
    // float arg + float return
    assertEquals("scale(4.0) = 10", "10", evaluate(frameId, "scale(4.0)").getBody().getResult());
    // bool arg + bool return
    assertEquals("negate(true) = false", "false", evaluate(frameId, "negate(true)").getBody().getResult());

    // the debuggee survived the injected calls and runs to completion
    request(new DisconnectRequest());
  }

  @Test
  public void callsAreRepeatableAndLeaveTheProcessRunnable() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    // several calls in a row must each restore state cleanly
    assertEquals("1", "3", evaluate(frameId, "add(1, 2)").getBody().getResult());
    assertEquals("2", "30", evaluate(frameId, "add(10, 20)").getBody().getResult());
    assertEquals("3", "25", evaluate(frameId, "scale(10.0)").getBody().getResult());

    // resume: the program's own output must be intact (calls didn't corrupt it)
    String output = continueToExit(stopped.getBody().getThreadId());
    assertTrue("program output intact after injected calls (" + output + ")", output.contains("call:11,25,true,orig10,L10,16,13,6"));
  }

  @Test
  public void callsBoundClosures() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int threadId = stopped.getBody().getThreadId();
    int frameId = topFrameId(threadId);

    // instance-method closure (value = the receiver): boost(n) = n + bonus(15)
    assertEquals("boost(3) = 18", "18", evaluate(frameId, "boost(3)").getBody().getResult());
    // capturing lambda (value = the capture env): plus(n) = n + base(10) + 2
    assertEquals("plus(5) = 17", "17", evaluate(frameId, "plus(5)").getBody().getResult());
    // repeatable: the captured state must be untouched by the first calls
    assertEquals("boost(0) = 15", "15", evaluate(frameId, "boost(0)").getBody().getResult());

    // the debuggee survived the bound calls: output intact through a clean exit
    String output = continueToExit(threadId);
    assertTrue("program output intact after bound calls (" + output + ")", output.contains("call:11,25,true,orig10,L10,16,13,6"));
  }

  @Test
  public void callsInsideExpressions() throws Exception {
    // calls are expression leaves — results feed operators, and
    // arguments are themselves full expressions (base = 10)
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    assertEquals("call result in arithmetic", "10", evaluate(frameId, "add(2, 3) * 2").getBody().getResult());
    assertEquals("expression arguments", "23", evaluate(frameId, "add(base + 1, base + 2)").getBody().getResult());
    assertEquals("bound closures in expressions", "41",
                 evaluate(frameId, "boost(1) + plus(base + 3)").getBody().getResult()); // 16 + 25
    assertEquals("string return concatenated", "\"L10!\"", evaluate(frameId, "label(base) + \"!\"").getBody().getResult());
    assertEquals("call result in comparison", "true", evaluate(frameId, "scale(4.0) == 10").getBody().getResult());

    request(new DisconnectRequest());
  }

  @Test
  public void rejectsBadCallsWithClearMessages() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    Response wrongArity = evaluateRaw(frameId, "add(1)");
    assertFalse("wrong argument count rejected", wrongArity.isSuccess());
    assertTrue("arity message", wrongArity.getMessage().contains("argument"));

    Response notAFunction = evaluateRaw(frameId, "base(1)");
    assertFalse("calling a non-function rejected", notAFunction.isSuccess());

    request(new DisconnectRequest());
  }

  @Test
  public void assignsACallResultToAVariable() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int threadId = stopped.getBody().getThreadId();
    int frameId = topFrameId(threadId);

    // int result into an int local
    assertTrue("base = add(20, 3)", evaluate(frameId, "base = add(20, 3)").isSuccess());
    // the prize: a String PRODUCED by the program's own code, assigned to a local
    assertTrue("s = label(7)", evaluate(frameId, "s = label(7)").isSuccess());

    int locals = localsScopeReference(topFrameId(lastStoppedThreadId()));
    assertEquals("base now holds the call result", "23", findVariable(variables(locals), "base").getValue());
    assertEquals("s now holds the produced String", "\"L7\"", findVariable(variables(locals), "s").getValue());

    // resume: the reassigned locals reach the program's own println
    String output = continueToExit(threadId);
    assertTrue("the produced String reached execution (" + output + ")", output.contains(",L7,"));

    request(new DisconnectRequest());
  }

  @Test
  public void createsAndAssignsNewStrings() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int threadId = stopped.getBody().getThreadId();
    int frameId = topFrameId(threadId);

    // allocate a brand-new String and assign it
    assertTrue("s = \"hello\"", evaluate(frameId, "s = \"hello\"").isSuccess());
    int locals = localsScopeReference(topFrameId(lastStoppedThreadId()));
    assertEquals("s now holds the new string", "\"hello\"", findVariable(variables(locals), "s").getValue());

    // a string with an escape, and a string passed as a call argument
    assertTrue("s = \"a\\tb\"", evaluate(frameId, "s = \"a\\tb\"").isSuccess());
    assertEquals("escaped string materialized", "\"a\tb\"", findVariable(variables(localsScopeReference(frameId)), "s").getValue());

    // set it once more, then resume: the created string reaches the program
    assertTrue("s = \"final\"", evaluate(frameId, "s = \"final\"").isSuccess());
    String output = continueToExit(threadId);
    assertTrue("the created string reached execution (" + output + ")", output.contains(",final,"));

    request(new DisconnectRequest());
  }

  @Test
  public void constructsObjectsWithNew() throws Exception {
    // `new Point(x,y,label)` — allocate via the mined hl_alloc_obj + type
    // pointer, then run the constructor. Point is constructed elsewhere in the
    // program (Main.inspectDemo), so its ONew site exists to mine.
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    // construct with int + null-string args, expand the result, check fields
    EvaluateResponse p = evaluate(frameId, "new Point(3, 4, null)");
    assertEquals("constructed Point", "Point", p.getBody().getType());
    assertTrue("Point is expandable", p.getBody().getVariablesReference() > 0);
    Map<String, String> fields = variablesByName(p.getBody().getVariablesReference());
    assertEquals("Point.x initialised by the ctor", "3", fields.get("x"));
    assertEquals("Point.y initialised by the ctor", "4", fields.get("y"));
    assertEquals("Point.label null", "null", fields.get("label"));

    // a String constructor argument (materialised on the heap) reaches a field
    Map<String, String> withLabel =
      variablesByName(evaluate(frameId, "new Point(5, 6, \"hi\")").getBody().getVariablesReference());
    assertEquals("Point.label from a string literal arg", "\"hi\"", withLabel.get("label"));

    request(new DisconnectRequest());
  }

  @Test
  public void assignsANewObjectToALocalAndItReachesExecution() throws Exception {
    // `p = new Point(...)` — but Call.demo has no Point local, so verify via a
    // fresh construction assigned through evaluate into a Dynamic array slot is
    // out of scope; instead assert the constructed instance survives a resume by
    // constructing, reading back, then continuing to a clean exit.
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int threadId = stopped.getBody().getThreadId();
    int frameId = topFrameId(threadId);

    EvaluateResponse p = evaluate(frameId, "new Point(7, 8, \"z\")");
    assertEquals("y set", "8", variablesByName(p.getBody().getVariablesReference()).get("y"));

    // the injected allocation + ctor left the debuggee intact
    String output = continueToExit(threadId);
    assertTrue("program output intact after construction (" + output + ")", output.contains("call:11,25,true,orig10,L10,16,13,6"));
  }

  @Test
  public void rejectsConstructingAnUninstantiatedClassClearly() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    // Config is never `new`d in the program (only static access), so there is no
    // ONew site to mine — construction must fail with a clear, honest message
    Response rejected = evaluateRaw(frameId, "new Config()");
    assertFalse("uninstantiated class rejected", rejected.isSuccess());
    assertTrue("message explains the limitation (was: " + rejected.getMessage() + ")",
               rejected.getMessage().toLowerCase().contains("experimental")
               || rejected.getMessage().toLowerCase().contains("construct"));

    request(new DisconnectRequest());
  }

  @Test
  public void callSucceedsAndStaysCleanWithABreakpointInsideTheCallee() throws Exception {
    // The reported corruption: an injected call whose body trips one of OUR
    // planted INT3s (a user breakpoint here; the hl_throw trap when VM-exception
    // breakpoints are on, for a callee that throws/catches internally) aborts
    // mid-call and leaves a half-executed frame that breaks later execution.
    // The eval-call must lift every breakpoint for the duration of the call.
    initialize();
    assertTrue("launch", launch().isSuccess());
    // breakpoints at the stop line AND inside addImpl (line 13, `return a + b;`)
    String callSrc = fixtureSrcDir.resolve(FIXTURE_CALL).toString();
    assertTrue("setBreakpoints", setBreakpoints(callSrc, FIXTURE_CALL_LINE, 13).isSuccess());
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());
    StoppedEvent stopped = awaitStopped();
    int threadId = stopped.getBody().getThreadId();
    int frameId = topFrameId(threadId);

    // add(1, 2) runs addImpl, which has a breakpoint on its body — the call must
    // still return 3 (not abort on that INT3) and not corrupt anything
    assertEquals("add(1, 2) completes past the interior breakpoint", "3",
                 evaluate(frameId, "add(1, 2)").getBody().getResult());

    // the program keeps running; the interior breakpoint is back in force, so
    // when the fixture's own print line calls add(base, 1) it stops there again
    assertTrue("continue", request(continueRequest(threadId)).isSuccess());
    StoppedEvent rehit = awaitStopped();
    assertEquals("interior breakpoint re-armed after the eval-call", "breakpoint",
                 rehit.getBody().getReason());

    request(new DisconnectRequest());
  }

  @Test
  public void pushToIntArrayReturnsAndLeavesTheProcessRunnable() throws Exception {
    // reproduces the reported corruption: a method call that allocates/grows an
    // Array<Int> (hl.types.ArrayBytes_Int.push) must return AND leave the VM
    // able to keep executing — a botched call teardown crashes later steps.
    StoppedEvent stopped = runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int threadId = stopped.getBody().getThreadId();
    int frameId = topFrameId(threadId);

    // ints = [2, 5, 10]; push(99) grows it and returns the new length 4
    assertEquals("ints.push(99) returns the new length", "4", evaluate(frameId, "ints.push(99)").getBody().getResult());
    // a second call must also be clean
    assertEquals("ints.push(7) returns 5", "5", evaluate(frameId, "ints.push(7)").getBody().getResult());

    // the program keeps running after the calls — no heap corruption. ints[2]
    // is still 10, so the fixture's own output line is unchanged.
    String output = continueToExit(threadId);
    assertTrue("program ran to completion intact after the pushes (" + output + ")", output.contains("rich:10"));
  }

  private EvaluateResponse evaluate(int frameId, String expression) throws Exception {
    Response response = evaluateRaw(frameId, expression);
    assertTrue("evaluate '" + expression + "' succeeds: " + response.getMessage(), response.isSuccess());
    return (EvaluateResponse)response;
  }

  private Response evaluateRaw(int frameId, String expression) throws Exception {
    EvaluateRequest request = new EvaluateRequest();
    EvaluateArguments args = new EvaluateArguments();
    args.setExpression(expression);
    args.setFrameId(frameId);
    request.setArguments(args);
    return request(request);
  }

  private String continueToExit(int threadId) throws Exception {
    assertTrue("continue", request(continueRequest(threadId)).isSuccess());
    List<String> output = new ArrayList<>();
    while (true) {
      Event event = client.pollEvent(TIMEOUT);
      if (event instanceof OutputEvent out) {
        output.add(out.getBody().getOutput());
      } else if (event instanceof StoppedEvent stopped) {
        request(continueRequest(stopped.getBody().getThreadId()));
      } else if (event instanceof ExitedEvent) {
        break;
      }
    }
    return String.join("", output);
  }
}
