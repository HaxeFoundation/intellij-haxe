package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import java.util.List;
import org.junit.jupiter.api.Test;

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
    assertEquals("Locals", before.get(0).getName(), "first scope before step");
    assertEquals("Registers", before.get(before.size() - 1).getName(), "last scope before step");
    List<Variable> localsBefore = variables(before.get(0).getVariablesReference());
    assertNotNull(findVariable(localsBefore, "i"), "locals contain i");
    int registersRefBefore = before.get(before.size() - 1).getVariablesReference();
    assertNotNull(findVariable(variables(registersRefBefore), "SP"), "registers contain SP");

    // step over
    request(nextRequest(threadId));
    StoppedEvent stepped = awaitStopped();
    int frameId2 = topFrameId(stepped.getBody().getThreadId());

    // re-render: same shape, fresh references
    List<Scope> after = scopesOf(frameId2);
    assertEquals("Locals", after.get(0).getName(), "first scope after step");
    assertEquals("Registers", after.get(after.size() - 1).getName(), "last scope after step");
    List<Variable> localsAfter = variables(after.get(0).getVariablesReference());
    assertNotNull(findVariable(localsAfter, "i"), "locals still contain i after step");
    assertEquals(null, findVariable(localsAfter, "SP"), "no CPU rows leaked into Locals");
    assertNotNull(findVariable(variables(after.get(after.size() - 1).getVariablesReference()), "SP"), "registers still contain SP after step");

    // the pre-step reference is dead, not aliased
    List<Variable> stale = variables(registersRefBefore);
    assertEquals(0, stale.size(), "a stale pre-step reference resolves to nothing (got " + names(stale) + ")");

    request(new DisconnectRequest());
  }

  private List<Scope> scopesOf(int frameId) throws Exception {
    ScopesRequest request = scopesRequest(frameId);
    return ((ScopesResponse)request(request)).getBody().getScopes();
  }

  private static String names(List<Variable> variables) {
    StringBuilder sb = new StringBuilder();
    for (Variable v : variables) sb.append(v.getName()).append(",");
    return sb.toString();
  }
}
