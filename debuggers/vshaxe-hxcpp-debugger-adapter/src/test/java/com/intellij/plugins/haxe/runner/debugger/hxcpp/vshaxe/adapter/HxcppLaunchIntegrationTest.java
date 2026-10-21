package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SourceBreakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The full DAP lifecycle against the real hxcpp-debug-server, driving the
 * Main.hx fixture (built by the buildHxcppFixtureFixture gradle task).
 *
 * The whole lifecycle is one test: an hxcpp session is expensive to start
 * and every stage depends on the previous one anyway.
 */
@DisplayName("HXCPP debugger (vshaxe): launch (integration)")
public class HxcppLaunchIntegrationTest extends HxcppIntegrationTestBase {
  @BeforeEach
  public void setUp() throws Exception {
    launchFixture("hxcpp.fixture.exe", "Main.hx");
  }

  @Test
  @DisplayName("full debug lifecycle against the real server")
  public void fullDebugLifecycleAgainstTheRealServer() throws Exception {
    initializeAndLaunch();

    // --- breakpoint before the program runs --------------------------------
    int accumulateLine = lineOfMarker("accumulate");
    SetBreakpointsResponse breakpoints = setBreakpointLines(accumulateLine);
    assertEquals(1, breakpoints.getBody().getBreakpoints().size());
    assertTrue(breakpoints.getBody().getBreakpoints().get(0).isVerified());

    // the program is held before main until configurationDone continues it
    require(new ConfigurationDoneRequest());

    // --- first hit ----------------------------------------------------------
    Stop stop = awaitStopAtLine(accumulateLine);
    int threadId = stop.threadId();
    ThreadsResponse threads = require(new ThreadsRequest());
    assertFalse(threads.getBody().getThreads().isEmpty());

    List<StackFrame> frames = stop.frames();
    StackFrame top = stop.top();
    assertTrue(top.getName().contains("accumulate"), "top frame is '" + top.getName() + "', expected accumulate");
    assertNotNull(top.getSource());
    assertTrue(top.getSource().getPath().endsWith("Main.hx"));

    // --- scopes + variables: known first-iteration values -------------------
    ScopesArguments scopesArguments = new ScopesArguments();
    scopesArguments.setFrameId(top.getId());
    ScopesRequest scopesRequest = new ScopesRequest();
    scopesRequest.setArguments(scopesArguments);
    ScopesResponse scopes = require(scopesRequest);
    assertFalse(scopes.getBody().getScopes().isEmpty(), "no scopes");

    Variable doubled = null;
    for (Scope scope : scopes.getBody().getScopes()) {
      VariablesArguments variablesArguments = new VariablesArguments();
      variablesArguments.setVariablesReference(scope.getVariablesReference());
      VariablesRequest variablesRequest = new VariablesRequest();
      variablesRequest.setArguments(variablesArguments);
      VariablesResponse variables = require(variablesRequest);
      for (Variable variable : variables.getBody().getVariables()) {
        if ("doubled".equals(variable.getName())) {
          doubled = variable;
        }
      }
    }
    assertNotNull(doubled, "local 'doubled' not found in any scope");
    // first iteration: v = items[0] = 0, doubled = 0
    assertEquals("0", doubled.getValue().trim());

    // --- evaluate in the stopped frame --------------------------------------
    assertEquals("0", evaluate("acc", top.getId()).getBody().getResult().trim());

    // --- assignment through evaluate must WRITE (the n = 100 bug) -----------
    // a TOP-frame local is writable; the changed value is verified by
    // read-back here and by the program's own trace output at the end
    // (doubled = 55 in iteration 1 makes the final total 115, not 60)
    require(evaluateRequest("doubled = 55", top.getId()));
    assertEquals("55", evaluate("doubled", top.getId()).getBody().getResult().trim(), "assignment did not stick");

    // a CALLER-frame variable is not writable (the server hardcodes the top
    // frame and would silently ignore it) — the adapter must say so
    assertTrue(frames.size() >= 2, "expected a caller frame");
    Response callerAssign = dapClient.sendRequest(evaluateRequest("n = 100", frames.get(1).getId()), TIMEOUT);
    assertFalse(callerAssign.isSuccess(), "caller-frame write should be refused, not silently ignored");
    assertTrue(callerAssign.getMessage().contains("TOP stack frame"), callerAssign.getMessage());

    // --- step over stays in the program -------------------------------------
    NextArguments nextArguments = new NextArguments();
    nextArguments.setThreadId(threadId);
    NextRequest nextRequest = new NextRequest();
    nextRequest.setArguments(nextArguments);
    require(nextRequest);
    awaitEvent(StoppedEvent.class);

    // --- second breakpoint hit on continue -----------------------------------
    sendContinue(threadId);
    awaitStopAtLine(accumulateLine);

    // --- run-to-cursor mechanism: replace the file's set with the target ----
    int doneLine = lineOfMarker("done");
    setBreakpointLines(doneLine);
    sendContinue(threadId);
    awaitStopAtLine(doneLine);

    // --- clear breakpoints, run to completion --------------------------------
    setBreakpointLines(/* none */);
    sendContinue(threadId);

    awaitEvent(TerminatedEvent.class);
    assertTrue(debuggee.waitFor(TIMEOUT, TimeUnit.MILLISECONDS), "debuggee did not exit");
    assertEquals(0, debuggee.exitValue(), "debuggee output:\n" + output());
    // 115, not 60: the doubled = 55 write in iteration 1 flowed into the sum —
    // execution-level proof that evaluate assignments reach the debuggee
    assertTrue(output().contains("total=115 title=fixture"), "expected trace output, got:\n" + output());
  }

  @Test
  @DisplayName("conditional breakpoint stops only when the condition is true")
  public void conditionalBreakpointStopsOnlyWhenTheConditionIsTrue() throws Exception {
    initializeAndLaunch();

    // items are [0, 10, 20]; the condition is true only on the third iteration
    SourceBreakpoint conditional = new SourceBreakpoint();
    conditional.setLine(lineOfMarker("accumulate"));
    conditional.setCondition("v == 20");
    setBreakpoints(conditional);

    require(new ConfigurationDoneRequest());

    Stop stop = awaitStopAtLine(lineOfMarker("accumulate"));
    int threadId = stop.threadId();
    int frameId = stop.top().getId();
    assertEquals("20", evaluate("v", frameId).getBody().getResult().trim(), "stopped on the wrong iteration");
    // acc after two un-stopped iterations: 0 + 0*2 + 10*2 = 20
    assertEquals("20", evaluate("acc", frameId).getBody().getResult().trim(), "earlier iterations should not have stopped");

    sendContinue(threadId);
    awaitEvent(TerminatedEvent.class);
    assertTrue(debuggee.waitFor(TIMEOUT, TimeUnit.MILLISECONDS), "debuggee did not exit");
    assertTrue(output().contains("total=60 title=fixture"), "expected trace output, got:\n" + output());
  }

  private EvaluateResponse evaluate(String expression, int frameId) throws Exception {
    return require(evaluateRequest(expression, frameId));
  }

  private static EvaluateRequest evaluateRequest(String expression, int frameId) {
    EvaluateArguments arguments = new EvaluateArguments();
    arguments.setExpression(expression);
    arguments.setFrameId(frameId);
    EvaluateRequest request = new EvaluateRequest();
    request.setArguments(arguments);
    return request;
  }
}
