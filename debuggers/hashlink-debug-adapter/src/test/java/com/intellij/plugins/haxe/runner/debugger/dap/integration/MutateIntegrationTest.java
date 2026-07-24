package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * Verifies value modification end to end against real HashLink: writes reach
 * the debuggee's memory, are read back correctly, and — the decisive check —
 * steer execution (a written bool takes a branch; written locals/fields/
 * elements change the printed result).
 */
public class MutateIntegrationTest extends DapIntegrationTestBase {

  @Test
  public void writesLocalsFieldsAndElementsAndSteersExecution() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MUTATE, FIXTURE_MUTATE_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());
    int locals = localsScopeReference(frameId);

    // primitive local + the bool that gates the branch
    assertEquals("n set to 99", "99", setVariable(locals, "n", "99"));
    assertEquals("flag set true", "true", setVariable(locals, "flag", "true"));

    // object field: obj.x, through obj's own reference
    Variable obj = findVariable(variables(locals), "obj");
    assertNotNull("obj present", obj);
    assertEquals("obj.x set to 42", "42", setVariable(obj.getVariablesReference(), "x", "42"));

    // array element: arr[1], through arr's reference
    Variable arr = findVariable(variables(locals), "arr");
    assertNotNull("arr present", arr);
    assertEquals("arr[1] set to 77", "77", setVariable(arr.getVariablesReference(), "1", "77"));

    String output = continueToExit(stopped.getBody().getThreadId());
    assertTrue("the written bool took the branch (" + output + ")", output.contains("mutate-branch-taken"));
    assertTrue("written local/field/element reached the result (" + output + ")",
               output.contains("mutate-result:99,42,77"));
  }

  @Test
  public void assignsThroughEvaluateExpression() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MUTATE, FIXTURE_MUTATE_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    // the "evaluate expression" route the user asked for: path = value
    assertTrue("n = 7 accepted", evaluate(frameId, "n = 7").isSuccess());
    assertTrue("obj.x = 3 accepted", evaluate(frameId, "obj.x = 3").isSuccess());
    assertTrue("copy from another variable accepted", evaluate(frameId, "n = idx").isSuccess());

    // reading back reflects the writes (idx is 1)
    int locals = localsScopeReference(topFrameId(lastStoppedThreadId()));
    assertEquals("n now holds idx's value", "1", findVariable(variables(locals), "n").getValue());

    request(new DisconnectRequest());
  }

  @Test
  public void rejectsAllocatingAndMistypedWritesWithClearMessages() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MUTATE, FIXTURE_MUTATE_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());
    int locals = localsScopeReference(frameId);

    // creating a new String needs allocation — not available in this session
    Response newString = evaluate(frameId, "n = \"hello\"");
    assertFalse("assigning a string literal is rejected", newString.isSuccess());

    // a boolean word into an int slot is a type error, reported clearly
    Response mistyped = setVariableRaw(locals, "n", "true");
    assertFalse("bool into an int slot is rejected", mistyped.isSuccess());
    assertNotNull("rejection carries a message", mistyped.getMessage());

    request(new DisconnectRequest());
  }

  @Test
  public void writeOnTheUseLineTakesEffect() throws Exception {
    // stop ON the line that uses the parameter (a prior use one line earlier,
    // no call between): the write must still reach the executed code. Pins
    // that HL 1.15's jitted code re-reads the stack slot here — if this ever
    // fails, the JIT started caching values in CPU registers across opcodes
    // and value writes need a rethink (we cannot write arbitrary CPU regs).
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MUTATE, FIXTURE_CACHED_LINE);
    int locals = localsScopeReference(topFrameId(stopped.getBody().getThreadId()));
    assertEquals("v set on the use line", "100", setVariable(locals, "v", "100"));

    String output = continueToExit(stopped.getBody().getThreadId());
    // v=5, doubled already computed as 10; the use line must see v=100
    assertTrue("the use line read the written value (" + output + ")", output.contains("cached:110"));
  }

  @Test
  public void writeToFloatArgOnItsUseLineTakesEffect() throws Exception {
    // the user-reported case: a Float parameter traced on the callee's FIRST
    // line. Float args ARRIVE in XMM0 and the trace consumes that register,
    // not the stack slot — the write must patch XMM0 too (the one arrival
    // register hl_debug_write_register exposes).
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MUTATE, FIXTURE_FLOAT_LINE);
    int locals = localsScopeReference(topFrameId(stopped.getBody().getThreadId()));
    assertEquals("y set on its use line", "9.5", setVariable(locals, "y", "9.5"));

    String output = continueToExit(stopped.getBody().getThreadId());
    assertTrue("the trace printed the written float (" + output + ")", output.contains("float-was:9.5"));
  }

  @Test
  public void writeToRegisterPassedIntArgWarnsAndAppliesToLaterUses() throws Exception {
    // the arrival-register caveat is an x86-64 calling-convention behavior;
    // 32-bit HL passes args on the stack, so it does not exist there
    org.junit.Assume.assumeFalse("known limitation: register-passed args are x86-64 only - skipping on x86 hl", isX86Hl());
    // same shape with an Int argument: its arrival register (RCX) is NOT
    // writable through the debug API, so the current line still sees the old
    // value — the slot IS updated (later uses see it) and the adapter says so
    // in a console note. Pins the documented limitation; if the output ever
    // shows 99 here, HL started re-reading the slot and the note can go.
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MUTATE, FIXTURE_INT_ARG_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());
    int locals = localsScopeReference(frameId);
    assertEquals("k slot updated", "99", setVariable(locals, "k", "99"));
    assertEquals("re-read confirms the slot", "99",
                 findVariable(variables(localsScopeReference(frameId)), "k").getValue());

    String output = continueToExit(stopped.getBody().getThreadId());
    assertTrue("the current line used the arrival register (" + output + ")", output.contains("int-was:12"));
    assertTrue("the adapter warned about the register-passed argument (" + output + ")",
               output.contains("register-passed argument"));
  }

  // --- helpers ---

  private String setVariable(int containerReference, String name, String value) throws Exception {
    Response response = setVariableRaw(containerReference, name, value);
    assertTrue("setVariable " + name + "=" + value + " succeeds: " + response.getMessage(), response.isSuccess());
    return ((SetVariableResponse)response).getBody().getValue();
  }

  private Response setVariableRaw(int containerReference, String name, String value) throws Exception {
    SetVariableRequest request = new SetVariableRequest();
    SetVariableArguments args = new SetVariableArguments();
    args.setVariablesReference(containerReference);
    args.setName(name);
    args.setValue(value);
    request.setArguments(args);
    return request(request);
  }

  private Response evaluate(int frameId, String expression) throws Exception {
    EvaluateRequest request = new EvaluateRequest();
    EvaluateArguments args = new EvaluateArguments();
    args.setExpression(expression);
    args.setFrameId(frameId);
    request.setArguments(args);
    return request(request);
  }

  private String continueToExit(int threadId) throws Exception {
    assertTrue("continue after writes", request(continueRequest(threadId)).isSuccess());
    List<String> output = new ArrayList<>();
    while (true) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull("expected more events before exit", event);
      if (event instanceof OutputEvent out) {
        output.add(out.getBody().getOutput());
      } else if (event instanceof StoppedEvent stopped) {
        // any further breakpoint (none expected): keep going
        request(continueRequest(stopped.getBody().getThreadId()));
      } else if (event instanceof ExitedEvent) {
        break;
      }
    }
    return String.join("", output);
  }
}
