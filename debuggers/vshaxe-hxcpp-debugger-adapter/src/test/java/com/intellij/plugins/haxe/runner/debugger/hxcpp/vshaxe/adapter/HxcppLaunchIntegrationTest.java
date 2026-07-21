package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.junit.Before;
import org.junit.Test;

/**
 * The full DAP lifecycle against the real hxcpp-debug-server, driving the
 * Main.hx fixture (built by the buildHxcppFixtureFixture gradle task).
 *
 * The whole lifecycle is one test: an hxcpp session is expensive to start
 * and every stage depends on the previous one anyway.
 */
public class HxcppLaunchIntegrationTest extends HxcppIntegrationTestBase {

  @Before
  public void setUp() throws Exception {
    launchFixture("hxcpp.fixture.exe", "Main.hx");
  }

  private EvaluateResponse evaluate(String expression, int frameId) throws Exception {
    EvaluateArguments arguments = new EvaluateArguments();
    arguments.setExpression(expression);
    arguments.setFrameId(frameId);
    EvaluateRequest request = new EvaluateRequest();
    request.setArguments(arguments);
    return require(request);
  }

  @Test
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
    assertTrue("top frame is '" + top.getName() + "', expected accumulate",
               top.getName().contains("accumulate"));
    assertNotNull(top.getSource());
    assertTrue(top.getSource().getPath().endsWith("Main.hx"));

    // --- scopes + variables: known first-iteration values -------------------
    ScopesArguments scopesArguments = new ScopesArguments();
    scopesArguments.setFrameId(top.getId());
    ScopesRequest scopesRequest = new ScopesRequest();
    scopesRequest.setArguments(scopesArguments);
    ScopesResponse scopes = require(scopesRequest);
    assertFalse("no scopes", scopes.getBody().getScopes().isEmpty());

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
    assertNotNull("local 'doubled' not found in any scope", doubled);
    // first iteration: v = items[0] = 0, doubled = 0
    assertEquals("0", doubled.getValue().trim());

    // --- evaluate in the stopped frame --------------------------------------
    assertEquals("0", evaluate("acc", top.getId()).getBody().getResult().trim());

    // --- assignment through evaluate must WRITE (the n = 100 bug) -----------
    // a TOP-frame local is writable; the changed value is verified by
    // read-back here and by the program's own trace output at the end
    // (doubled = 55 in iteration 1 makes the final total 115, not 60)
    require(assignmentRequest("doubled = 55", top.getId()));
    assertEquals("assignment did not stick", "55",
                 evaluate("doubled", top.getId()).getBody().getResult().trim());

    // a CALLER-frame variable is not writable (the server hardcodes the top
    // frame and would silently ignore it) — the adapter must say so
    assertTrue("expected a caller frame", frames.size() >= 2);
    Response callerAssign = dapClient.sendRequest(assignmentRequest("n = 100", frames.get(1).getId()), TIMEOUT);
    assertFalse("caller-frame write should be refused, not silently ignored", callerAssign.isSuccess());
    assertTrue(callerAssign.getMessage(), callerAssign.getMessage().contains("TOP stack frame"));

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
    assertTrue("debuggee did not exit", debuggee.waitFor(TIMEOUT, TimeUnit.MILLISECONDS));
    assertEquals("debuggee output:\n" + output(), 0, debuggee.exitValue());
    // 115, not 60: the doubled = 55 write in iteration 1 flowed into the sum —
    // execution-level proof that evaluate assignments reach the debuggee
    assertTrue("expected trace output, got:\n" + output(),
               output().contains("total=115 title=fixture"));
  }

  @Test
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
    assertEquals("stopped on the wrong iteration", "20",
                 evaluate("v", frameId).getBody().getResult().trim());
    // acc after two un-stopped iterations: 0 + 0*2 + 10*2 = 20
    assertEquals("earlier iterations should not have stopped", "20",
                 evaluate("acc", frameId).getBody().getResult().trim());

    sendContinue(threadId);
    awaitEvent(TerminatedEvent.class);
    assertTrue("debuggee did not exit", debuggee.waitFor(TIMEOUT, TimeUnit.MILLISECONDS));
    assertTrue("expected trace output, got:\n" + output(),
               output().contains("total=60 title=fixture"));
  }

  private EvaluateRequest assignmentRequest(String expression, int frameId) {
    EvaluateArguments arguments = new EvaluateArguments();
    arguments.setExpression(expression);
    arguments.setFrameId(frameId);
    EvaluateRequest request = new EvaluateRequest();
    request.setArguments(arguments);
    return request;
  }
}
