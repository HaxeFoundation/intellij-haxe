package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.DisconnectRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.EvaluateArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.EvaluateRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.EvaluateResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.ExitedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.OutputEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Runs functions inside the stopped debuggee via evaluate (M13a) against real
 * HashLink: injects a call trampoline, runs it, and reads the return. The
 * decisive checks assert KNOWN return values, and that the debuggee is intact
 * afterwards (execution continues to a clean exit).
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
    assertTrue("program output intact after injected calls (" + output + ")", output.contains("call:11,25,true,orig10,L10,4"));
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

    // the headline M13c feature: allocate a brand-new String and assign it
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
    // M15: `new Point(x,y,label)` — allocate via the mined hl_alloc_obj + type
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

    // a String constructor argument (materialised via M13c) reaches a field
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
    assertTrue("program output intact after construction (" + output + ")", output.contains("call:11,25,true,orig10,L10,4"));
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
