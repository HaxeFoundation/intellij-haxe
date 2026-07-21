package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import java.util.List;
import org.junit.Test;

/**
 * Replays the IDE's request sequence around a step: render the frame (scopes,
 * inline first-scope variables, expand Registers), step over, render again.
 * Guards the invariants the IDE depends on: the scope ORDER (the IDE inlines
 * whichever scope comes first), the scope contents after the step, and that a
 * stale pre-step variablesReference resolves to NOTHING — reference numbers
 * must never be reused across stops, or a stale request silently aliases onto
 * whatever the new stop allocated under the same number.
 */
public class StepScopeConsistencyTest extends DapIntegrationTestBase {

  @Test
  public void scopesStayOrderedAndStaleReferencesDieAcrossAStep() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);
    int threadId = stopped.getBody().getThreadId();
    int frameId = topFrameId(threadId);

    // initial render, as the IDE does it
    List<Scope> before = scopesOf(frameId);
    assertEquals("first scope before step", "Locals", before.get(0).getName());
    assertEquals("last scope before step", "Registers", before.get(before.size() - 1).getName());
    List<Variable> localsBefore = variables(before.get(0).getVariablesReference());
    assertNotNull("locals contain i", findVariable(localsBefore, "i"));
    int registersRefBefore = before.get(before.size() - 1).getVariablesReference();
    assertNotNull("registers contain SP", findVariable(variables(registersRefBefore), "SP"));

    // step over
    request(nextRequest(threadId));
    StoppedEvent stepped = awaitStopped();
    int frameId2 = topFrameId(stepped.getBody().getThreadId());

    // re-render: same shape, fresh references
    List<Scope> after = scopesOf(frameId2);
    assertEquals("first scope after step", "Locals", after.get(0).getName());
    assertEquals("last scope after step", "Registers", after.get(after.size() - 1).getName());
    List<Variable> localsAfter = variables(after.get(0).getVariablesReference());
    assertNotNull("locals still contain i after step", findVariable(localsAfter, "i"));
    assertEquals("no CPU rows leaked into Locals", null, findVariable(localsAfter, "SP"));
    assertNotNull("registers still contain SP after step",
                  findVariable(variables(after.get(after.size() - 1).getVariablesReference()), "SP"));

    // the pre-step reference is dead, not aliased
    List<Variable> stale = variables(registersRefBefore);
    assertEquals("a stale pre-step reference resolves to nothing (got " + names(stale) + ")",
                 0, stale.size());

    request(new DisconnectRequest());
  }

  private List<Scope> scopesOf(int frameId) throws Exception {
    ScopesRequest request = new ScopesRequest();
    ScopesArguments args = new ScopesArguments();
    args.setFrameId(frameId);
    request.setArguments(args);
    return ((ScopesResponse)request(request)).getBody().getScopes();
  }

  private static String names(List<Variable> variables) {
    StringBuilder sb = new StringBuilder();
    for (Variable v : variables) sb.append(v.getName()).append(",");
    return sb.toString();
  }
}
