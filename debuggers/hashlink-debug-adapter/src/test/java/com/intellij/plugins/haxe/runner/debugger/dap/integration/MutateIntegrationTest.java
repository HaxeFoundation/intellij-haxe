package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies value modification end to end against real HashLink: writes reach
 * the debuggee's memory, are read back correctly, and — the decisive check —
 * steer execution (a written bool takes a branch; written locals/fields/
 * elements change the printed result).
 */
@DisplayName("HashLink debugger: mutate (integration)")
public class MutateIntegrationTest extends DapIntegrationTestBase {

  @Test
  @DisplayName("writes locals fields and elements and steers execution")
  public void writesLocalsFieldsAndElementsAndSteersExecution() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MUTATE, FIXTURE_MUTATE_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());
    int locals = localsScopeReference(frameId);

    // primitive local + the bool that gates the branch
    assertEquals("99", setVariable(locals, "n", "99"), "n set to 99");
    assertEquals("true", setVariable(locals, "flag", "true"), "flag set true");

    // object field: obj.x, through obj's own reference
    Variable obj = findVariable(variables(locals), "obj");
    assertNotNull(obj, "obj present");
    assertEquals("42", setVariable(obj.getVariablesReference(), "x", "42"), "obj.x set to 42");

    // array element: arr[1], through arr's reference
    Variable arr = findVariable(variables(locals), "arr");
    assertNotNull(arr, "arr present");
    assertEquals("77", setVariable(arr.getVariablesReference(), "1", "77"), "arr[1] set to 77");

    String output = continueToExit(stopped.getBody().getThreadId());
    assertTrue(output.contains("mutate-branch-taken"), "the written bool took the branch (" + output + ")");
    assertTrue(output.contains("mutate-result:99,42,77"), "written local/field/element reached the result (" + output + ")");
  }

  @Test
  @DisplayName("assigns through evaluate expression")
  public void assignsThroughEvaluateExpression() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MUTATE, FIXTURE_MUTATE_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    // the "evaluate expression" route the user asked for: path = value
    assertTrue(evaluateRaw(frameId, "n = 7").isSuccess(), "n = 7 accepted");
    assertTrue(evaluateRaw(frameId, "obj.x = 3").isSuccess(), "obj.x = 3 accepted");
    assertTrue(evaluateRaw(frameId, "n = idx").isSuccess(), "copy from another variable accepted");

    // reading back reflects the writes (idx is 1)
    int locals = localsScopeReference(topFrameId(lastStoppedThreadId()));
    assertEquals("1", findVariable(variables(locals), "n").getValue(), "n now holds idx's value");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("rejects allocating and mistyped writes with clear messages")
  public void rejectsAllocatingAndMistypedWritesWithClearMessages() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MUTATE, FIXTURE_MUTATE_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());
    int locals = localsScopeReference(frameId);

    // creating a new String needs allocation — not available in this session
    Response newString = evaluateRaw(frameId, "n = \"hello\"");
    assertFalse(newString.isSuccess(), "assigning a string literal is rejected");

    // a boolean word into an int slot is a type error, reported clearly
    Response mistyped = request(setVariableRequest(locals, "n", "true"));
    assertFalse(mistyped.isSuccess(), "bool into an int slot is rejected");
    assertNotNull(mistyped.getMessage(), "rejection carries a message");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("write on the use line takes effect")
  public void writeOnTheUseLineTakesEffect() throws Exception {
    // stop ON the line that uses the parameter (a prior use one line earlier,
    // no call between): the write must still reach the executed code. Pins
    // that HL 1.15's jitted code re-reads the stack slot here — if this ever
    // fails, the JIT started caching values in CPU registers across opcodes
    // and value writes need a rethink (we cannot write arbitrary CPU regs).
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MUTATE, FIXTURE_CACHED_LINE);
    int locals = localsScopeReference(topFrameId(stopped.getBody().getThreadId()));
    assertEquals("100", setVariable(locals, "v", "100"), "v set on the use line");

    String output = continueToExit(stopped.getBody().getThreadId());
    // v=5, doubled already computed as 10; the use line must see v=100
    assertTrue(output.contains("cached:110"), "the use line read the written value (" + output + ")");
  }

  @Test
  @DisplayName("write to float arg on its use line takes effect")
  public void writeToFloatArgOnItsUseLineTakesEffect() throws Exception {
    // the user-reported case: a Float parameter traced on the callee's FIRST
    // line. Float args ARRIVE in XMM0 and the trace consumes that register,
    // not the stack slot — the write must patch XMM0 too (the one arrival
    // register hl_debug_write_register exposes).
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MUTATE, FIXTURE_FLOAT_LINE);
    int locals = localsScopeReference(topFrameId(stopped.getBody().getThreadId()));
    assertEquals("9.5", setVariable(locals, "y", "9.5"), "y set on its use line");

    String output = continueToExit(stopped.getBody().getThreadId());
    assertTrue(output.contains("float-was:9.5"), "the trace printed the written float (" + output + ")");
  }

  @Test
  @DisplayName("write to register passed int arg warns and applies to later uses")
  public void writeToRegisterPassedIntArgWarnsAndAppliesToLaterUses() throws Exception {
    // the arrival-register caveat is an x86-64 calling-convention behavior;
    // 32-bit HL passes args on the stack, so it does not exist there
    Assumptions.assumeFalse(isX86Hl(), "known limitation: register-passed args are x86-64 only - skipping on x86 hl");
    // same shape with an Int argument: its arrival register (RCX) is NOT
    // writable through the debug API, so the current line still sees the old
    // value — the slot IS updated (later uses see it) and the adapter says so
    // in a console note. Pins the documented limitation; if the output ever
    // shows 99 here, HL started re-reading the slot and the note can go.
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MUTATE, FIXTURE_INT_ARG_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());
    int locals = localsScopeReference(frameId);
    assertEquals("99", setVariable(locals, "k", "99"), "k slot updated");
    assertEquals("99", findVariable(variables(localsScopeReference(frameId)), "k").getValue(), "re-read confirms the slot");

    String output = continueToExit(stopped.getBody().getThreadId());
    assertTrue(output.contains("int-was:12"), "the current line used the arrival register (" + output + ")");
    assertTrue(output.contains("register-passed argument"), "the adapter warned about the register-passed argument (" + output + ")");
  }

  // --- helpers ---

  private String setVariable(int containerReference, String name, String value) throws Exception {
    Response response = request(setVariableRequest(containerReference, name, value));
    assertTrue(response.isSuccess(), "setVariable " + name + "=" + value + " succeeds: " + response.getMessage());
    return ((SetVariableResponse)response).getBody().getValue();
  }

  private String continueToExit(int threadId) throws Exception {
    assertTrue(request(continueRequest(threadId)).isSuccess(), "continue after writes");

    List<String> output = new ArrayList<>();

    while (true) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull(event, "expected more events before exit");
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
