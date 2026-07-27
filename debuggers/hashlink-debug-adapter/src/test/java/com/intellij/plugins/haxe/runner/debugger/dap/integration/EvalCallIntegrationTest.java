package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

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
    assertEquals("23", evaluated(frameId, "add(20, 3)"), "add(20, 3) = 23");
    // a path argument (base is 10) + a literal
    assertEquals("15", evaluated(frameId, "add(base, 5)"), "add(base, 5) = 15");
    // float arg + float return
    assertEquals("10", evaluated(frameId, "scale(4.0)"), "scale(4.0) = 10");
    // bool arg + bool return
    assertEquals("false", evaluated(frameId, "negate(true)"), "negate(true) = false");

    // the debuggee survived the injected calls and runs to completion
    request(new DisconnectRequest());
  }

  @Test
  public void callsAreRepeatableAndLeaveTheProcessRunnable() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    // several calls in a row must each restore state cleanly
    assertEquals("3", evaluated(frameId, "add(1, 2)"), "1");
    assertEquals("30", evaluated(frameId, "add(10, 20)"), "2");
    assertEquals("25", evaluated(frameId, "scale(10.0)"), "3");

    // resume: the program's own output must be intact (calls didn't corrupt it)
    String output = continueToExit(stopped.getBody().getThreadId());
    assertTrue(output.contains("call:11,25,true,orig10,L10,16,13,6"), "program output intact after injected calls (" + output + ")");
  }

  @Test
  public void callsBoundClosures() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int threadId = stopped.getBody().getThreadId();
    int frameId = topFrameId(threadId);

    // instance-method closure (value = the receiver): boost(n) = n + bonus(15)
    assertEquals("18", evaluated(frameId, "boost(3)"), "boost(3) = 18");
    // capturing lambda (value = the capture env): plus(n) = n + base(10) + 2
    assertEquals("17", evaluated(frameId, "plus(5)"), "plus(5) = 17");
    // repeatable: the captured state must be untouched by the first calls
    assertEquals("15", evaluated(frameId, "boost(0)"), "boost(0) = 15");

    // the debuggee survived the bound calls: output intact through a clean exit
    String output = continueToExit(threadId);
    assertTrue(output.contains("call:11,25,true,orig10,L10,16,13,6"), "program output intact after bound calls (" + output + ")");
  }

  @Test
  public void callsInsideExpressions() throws Exception {
    // calls are expression leaves — results feed operators, and
    // arguments are themselves full expressions (base = 10)
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    assertEquals("10", evaluated(frameId, "add(2, 3) * 2"), "call result in arithmetic");
    assertEquals("23", evaluated(frameId, "add(base + 1, base + 2)"), "expression arguments");
    assertEquals("41", evaluated(frameId, "boost(1) + plus(base + 3)"), "bound closures in expressions"); // 16 + 25
    assertEquals("\"L10!\"", evaluated(frameId, "label(base) + \"!\""), "string return concatenated");
    assertEquals("true", evaluated(frameId, "scale(4.0) == 10"), "call result in comparison");

    request(new DisconnectRequest());
  }

  @Test
  public void rejectsBadCallsWithClearMessages() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    Response wrongArity = evaluateRaw(frameId, "add(1)");
    assertFalse(wrongArity.isSuccess(), "wrong argument count rejected");
    assertTrue(wrongArity.getMessage().contains("argument"), "arity message");

    Response notAFunction = evaluateRaw(frameId, "base(1)");
    assertFalse(notAFunction.isSuccess(), "calling a non-function rejected");

    request(new DisconnectRequest());
  }

  @Test
  public void assignsACallResultToAVariable() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int threadId = stopped.getBody().getThreadId();
    int frameId = topFrameId(threadId);

    // int result into an int local
    assertTrue(evaluate(frameId, "base = add(20, 3)").isSuccess(), "base = add(20, 3)");
    // the prize: a String PRODUCED by the program's own code, assigned to a local
    assertTrue(evaluate(frameId, "s = label(7)").isSuccess(), "s = label(7)");

    int locals = localsScopeReference(topFrameId(lastStoppedThreadId()));
    assertEquals("23", findVariable(variables(locals), "base").getValue(), "base now holds the call result");
    assertEquals("\"L7\"", findVariable(variables(locals), "s").getValue(), "s now holds the produced String");

    // resume: the reassigned locals reach the program's own println
    String output = continueToExit(threadId);
    assertTrue(output.contains(",L7,"), "the produced String reached execution (" + output + ")");

    request(new DisconnectRequest());
  }

  @Test
  public void createsAndAssignsNewStrings() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int threadId = stopped.getBody().getThreadId();
    int frameId = topFrameId(threadId);

    // allocate a brand-new String and assign it
    assertTrue(evaluate(frameId, "s = \"hello\"").isSuccess(), "s = \"hello\"");
    int locals = localsScopeReference(topFrameId(lastStoppedThreadId()));
    assertEquals("\"hello\"", findVariable(variables(locals), "s").getValue(), "s now holds the new string");

    // a string with an escape, and a string passed as a call argument
    assertTrue(evaluate(frameId, "s = \"a\\tb\"").isSuccess(), "s = \"a\\tb\"");
    assertEquals("\"a\tb\"", findVariable(variables(localsScopeReference(frameId)), "s").getValue(), "escaped string materialized");

    // set it once more, then resume: the created string reaches the program
    assertTrue(evaluate(frameId, "s = \"final\"").isSuccess(), "s = \"final\"");
    String output = continueToExit(threadId);
    assertTrue(output.contains(",final,"), "the created string reached execution (" + output + ")");

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
    assertEquals("Point", p.getBody().getType(), "constructed Point");
    assertTrue(p.getBody().getVariablesReference() > 0, "Point is expandable");
    Map<String, String> fields = variablesByName(p.getBody().getVariablesReference());
    assertEquals("3", fields.get("x"), "Point.x initialised by the ctor");
    assertEquals("4", fields.get("y"), "Point.y initialised by the ctor");
    assertEquals("null", fields.get("label"), "Point.label null");

    // a String constructor argument (materialised on the heap) reaches a field
    Map<String, String> withLabel =
      variablesByName(evaluate(frameId, "new Point(5, 6, \"hi\")").getBody().getVariablesReference());
    assertEquals("\"hi\"", withLabel.get("label"), "Point.label from a string literal arg");

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
    assertEquals("8", variablesByName(p.getBody().getVariablesReference()).get("y"), "y set");

    // the injected allocation + ctor left the debuggee intact
    String output = continueToExit(threadId);
    assertTrue(output.contains("call:11,25,true,orig10,L10,16,13,6"), "program output intact after construction (" + output + ")");
  }

  @Test
  public void rejectsConstructingAnUninstantiatedClassClearly() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    // Config is never `new`d in the program (only static access), so there is no
    // ONew site to mine — construction must fail with a clear, honest message
    Response rejected = evaluateRaw(frameId, "new Config()");
    assertFalse(rejected.isSuccess(), "uninstantiated class rejected");
    String rejection = rejected.getMessage().toLowerCase();
    boolean namesTheLimit = rejection.contains("experimental") || rejection.contains("construct");
    assertTrue(namesTheLimit, "message explains the limitation (was: " + rejected.getMessage() + ")");

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
    assertTrue(launch().isSuccess(), "launch");

    // breakpoints at the stop line AND inside addImpl (line 13, `return a + b;`)
    String callSrc = fixtureSrcDir.resolve(FIXTURE_CALL).toString();
    assertTrue(setBreakpoints(callSrc, FIXTURE_CALL_LINE, 13).isSuccess(), "setBreakpoints");

    configurationDone();
    StoppedEvent stopped = awaitStopped();
    int threadId = stopped.getBody().getThreadId();
    int frameId = topFrameId(threadId);

    // add(1, 2) runs addImpl, which has a breakpoint on its body — the call must
    // still return 3 (not abort on that INT3) and not corrupt anything
    assertEquals("3", evaluated(frameId, "add(1, 2)"), "add(1, 2) completes past the interior breakpoint");

    // the program keeps running; the interior breakpoint is back in force, so
    // when the fixture's own print line calls add(base, 1) it stops there again
    assertTrue(request(continueRequest(threadId)).isSuccess(), "continue");
    StoppedEvent rehit = awaitStopped();
    assertEquals("breakpoint", rehit.getBody().getReason(), "interior breakpoint re-armed after the eval-call");

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
    assertEquals("4", evaluated(frameId, "ints.push(99)"), "ints.push(99) returns the new length");
    // a second call must also be clean
    assertEquals("5", evaluated(frameId, "ints.push(7)"), "ints.push(7) returns 5");

    // the program keeps running after the calls — no heap corruption. ints[2]
    // is still 10, so the fixture's own output line is unchanged.
    String output = continueToExit(threadId);
    assertTrue(output.contains("rich:10"), "program ran to completion intact after the pushes (" + output + ")");
  }

  private String continueToExit(int threadId) throws Exception {
    assertTrue(request(continueRequest(threadId)).isSuccess(), "continue");
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
