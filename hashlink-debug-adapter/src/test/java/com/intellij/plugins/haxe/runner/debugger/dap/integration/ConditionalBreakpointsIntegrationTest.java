package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.ExitedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.OutputEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.DisconnectRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Conditional breakpoints: the adapter evaluates the IDE's breakpoint
 * Condition expression at each hit and only stops when it is true. Verified
 * against real HashLink on the Main.hx loop (line 18: total = add(total, i),
 * i in 0..3), where at i==2 the accumulated total is exactly 1.
 */
public class ConditionalBreakpointsIntegrationTest extends DapIntegrationTestBase {

  @Test
  public void stopsOnlyWhenTheConditionIsTrue() throws Exception {
    initialize();
    assertTrue("launch", launch().isSuccess());
    assertTrue("conditional breakpoint set", setConditionalBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE, "i == 2").isSuccess());
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent stopped = awaitStopped();
    Map<String, String> locals = localsInTopFrame(stopped.getBody().getThreadId());
    assertEquals("stopped on the i==2 iteration", "2", locals.get("i"));
    assertEquals("total accumulated 0+1 by then", "1", locals.get("total"));

    // no further hit (i only reaches 2): the program runs to completion
    String output = continueToExit(stopped.getBody().getThreadId());
    assertTrue("ran to completion (" + output + ")", output.contains("fixture-total:3"));
  }

  @Test
  public void conditionCanBeAnExpressionOverSeveralLocals() throws Exception {
    initialize();
    assertTrue("launch", launch().isSuccess());
    // true on i==1 and i==2 → two stops (total is 0 then 1)
    assertTrue("conditional breakpoint set", setConditionalBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE, "i >= 1 && total < 5").isSuccess());
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());

    StoppedEvent first = awaitStopped();
    assertEquals("first stop at i==1", "1", localsInTopFrame(first.getBody().getThreadId()).get("i"));

    assertTrue("continue", request(continueRequest(first.getBody().getThreadId())).isSuccess());
    StoppedEvent second = awaitStopped();
    assertEquals("second stop at i==2", "2", localsInTopFrame(second.getBody().getThreadId()).get("i"));

    String output = continueToExit(second.getBody().getThreadId());
    assertTrue("ran to completion (" + output + ")", output.contains("fixture-total:3"));
  }

  @Test
  public void anAlwaysFalseConditionNeverStops() throws Exception {
    initialize();
    assertTrue("launch", launch().isSuccess());
    assertTrue("conditional breakpoint set", setConditionalBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE, "i == 99").isSuccess());
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());

    // never stops: the program runs straight through to its exit
    boolean exited = false;
    List<String> output = new ArrayList<>();
    while (!exited) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull("expected an event before exit", event);
      if (event instanceof StoppedEvent) {
        throw new AssertionError("a false condition must not stop the debuggee");
      } else if (event instanceof OutputEvent out) {
        output.add(out.getBody().getOutput());
      } else if (event instanceof ExitedEvent) {
        exited = true;
      }
    }
    assertTrue("ran to completion (" + output + ")", String.join("", output).contains("fixture-total:3"));
  }

  @Test
  public void abrokenConditionFailsSafeByStopping() throws Exception {
    initialize();
    assertTrue("launch", launch().isSuccess());
    // `nope` is not in scope: the adapter cannot evaluate the condition and must
    // FAIL SAFE by stopping (a note explains why), never silently skip the hit
    assertTrue("conditional breakpoint set", setConditionalBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE, "nope > 0").isSuccess());
    assertTrue("configurationDone", request(new ConfigurationDoneRequest()).isSuccess());

    boolean stopped = false;
    boolean sawNote = false;
    while (!stopped) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull("expected a stop from the fail-safe", event);
      if (event instanceof OutputEvent out) {
        String text = out.getBody().getOutput();
        if (text != null && text.contains("could not be evaluated")) {
          sawNote = true;
        }
      } else if (event instanceof StoppedEvent) {
        stopped = true;
      }
    }
    assertTrue("a note explained the failed condition", sawNote);

    request(new DisconnectRequest());
  }

  private String continueToExit(int threadId) throws Exception {
    assertTrue("continue", request(continueRequest(threadId)).isSuccess());
    List<String> output = new ArrayList<>();
    while (true) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull("expected an event before exit", event);
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
