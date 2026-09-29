package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Conditional breakpoints: the adapter evaluates the IDE's breakpoint
 * Condition expression at each hit and only stops when it is true. Verified
 * against real HashLink on the Main.hx loop (line 18: total = add(total, i),
 * i in 0..3), where at i==2 the accumulated total is exactly 1.
 */
@DisplayName("HashLink debugger: conditional breakpoints (integration)")
public class ConditionalBreakpointsIntegrationTest extends DapIntegrationTestBase {

  @Test
  @DisplayName("stops only when the condition is true")
  public void stopsOnlyWhenTheConditionIsTrue() throws Exception {
    initialize();
    assertTrue(launch().isSuccess(), "launch");
    assertTrue(setConditionalBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE, "i == 2").isSuccess(), "conditional breakpoint set");
    configurationDone();

    StoppedEvent stopped = awaitStopped();
    Map<String, String> locals = localsInTopFrame(stopped.getBody().getThreadId());
    assertEquals("2", locals.get("i"), "stopped on the i==2 iteration");
    assertEquals("1", locals.get("total"), "total accumulated 0+1 by then");

    // no further hit (i only reaches 2): the program runs to completion
    String output = continueToExit(stopped.getBody().getThreadId());
    assertTrue(output.contains("fixture-total:3"), "ran to completion (" + output + ")");
  }

  @Test
  @DisplayName("condition can be an expression over several locals")
  public void conditionCanBeAnExpressionOverSeveralLocals() throws Exception {
    initialize();
    assertTrue(launch().isSuccess(), "launch");
    // true on i==1 and i==2 → two stops (total is 0 then 1)
    assertTrue(setConditionalBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE, "i >= 1 && total < 5").isSuccess(), "conditional breakpoint set");
    configurationDone();

    StoppedEvent first = awaitStopped();
    assertEquals("1", localsInTopFrame(first.getBody().getThreadId()).get("i"), "first stop at i==1");

    assertTrue(request(continueRequest(first.getBody().getThreadId())).isSuccess(), "continue");
    StoppedEvent second = awaitStopped();
    assertEquals("2", localsInTopFrame(second.getBody().getThreadId()).get("i"), "second stop at i==2");

    String output = continueToExit(second.getBody().getThreadId());
    assertTrue(output.contains("fixture-total:3"), "ran to completion (" + output + ")");
  }

  @Test
  @DisplayName("an always false condition never stops")
  public void anAlwaysFalseConditionNeverStops() throws Exception {
    initialize();
    assertTrue(launch().isSuccess(), "launch");
    assertTrue(setConditionalBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE, "i == 99").isSuccess(), "conditional breakpoint set");
    configurationDone();

    // never stops: the program runs straight through to its exit
    boolean exited = false;
    List<String> output = new ArrayList<>();

    while (!exited) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull(event, "expected an event before exit");
      if (event instanceof StoppedEvent) {
        throw new AssertionError("a false condition must not stop the debuggee");
      } else if (event instanceof OutputEvent out) {
        output.add(out.getBody().getOutput());
      } else if (event instanceof ExitedEvent) {
        exited = true;
      }
    }
    assertTrue(String.join("", output).contains("fixture-total:3"), "ran to completion (" + output + ")");
  }

  @Test
  @DisplayName("abroken condition fails safe by stopping")
  public void abrokenConditionFailsSafeByStopping() throws Exception {
    initialize();
    assertTrue(launch().isSuccess(), "launch");
    // `nope` is not in scope: the adapter cannot evaluate the condition and must
    // FAIL SAFE by stopping (a note explains why), never silently skip the hit
    assertTrue(setConditionalBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE, "nope > 0").isSuccess(), "conditional breakpoint set");
    configurationDone();

    boolean stopped = false;
    boolean sawNote = false;
    while (!stopped) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull(event, "expected a stop from the fail-safe");
      if (event instanceof OutputEvent out) {
        String text = out.getBody().getOutput();
        if (text != null && text.contains("could not be evaluated")) {
          sawNote = true;
        }
      } else if (event instanceof StoppedEvent) {
        stopped = true;
      }
    }
    assertTrue(sawNote, "a note explained the failed condition");

    request(new DisconnectRequest());
  }

}
