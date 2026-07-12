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
    assertTrue("program output intact after injected calls (" + output + ")", output.contains("call:11,25,true,3"));
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
