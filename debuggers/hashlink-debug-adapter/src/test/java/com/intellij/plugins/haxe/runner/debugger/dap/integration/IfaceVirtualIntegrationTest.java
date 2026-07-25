package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Interfaces and virtuals. genhl gives a class one EMPTY-NAMED HVirtual field
 * per implemented interface — the runtime cache for that interface view
 * (hl_to_virtual) — and an interface-typed local is a vvirtual wrapping the
 * instance. Neither is useful as a structural member list: the interface's
 * own members are properties and methods, whose virtual slots hold no data
 * (a method's slot holds the function's CODE pointer, which must never be
 * dereferenced as a value). Both therefore resolve to the INSTANCE.
 */
public class IfaceVirtualIntegrationTest extends DapIntegrationTestBase {

  @Test
  public void anInterfaceTypedLocalShowsTheInstanceAndItsValues() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_IFACE, FIXTURE_IFACE_LINE);
    List<Variable> locals = topFrameVariables(stopped.getBody().getThreadId());

    Variable asIface = findVariable(locals, "asIface");
    assertNotNull("local asIface present", asIface);
    assertEquals("the interface view resolves to the runtime class", "AnimTask", asIface.getValue());
    assertTrue("the instance is expandable", asIface.getVariablesReference() > 0);

    // the instance's own fields, with real values - not the interface's
    // properties/methods, which carry none
    Map<String, String> fields = variablesByName(asIface.getVariablesReference());
    assertEquals("inherited field", "8", fields.get("baseId"));
    assertEquals("own field", "4", fields.get("count"));
    assertEquals("own field", "\"anim4\"", fields.get("name"));
  }

  /** The per-interface cache field is compiler-internal and stays hidden. */
  @Test
  public void theHiddenInterfaceCacheFieldIsNotShown() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_IFACE, FIXTURE_IFACE_LINE);
    List<Variable> locals = topFrameVariables(stopped.getBody().getThreadId());

    Variable task = findVariable(locals, "task");
    assertNotNull("local task present", task);
    List<Variable> children = variables(task.getVariablesReference());
    for (Variable child : children) {
      assertFalse("no empty-named field is shown (" + child.getValue() + ")",
                  child.getName() == null || child.getName().isEmpty());
    }
    Map<String, String> fields = variablesByName(task.getVariablesReference());
    assertEquals("the object's own fields still decode", "4", fields.get("count"));
    assertEquals("including the inherited one", "8", fields.get("baseId"));
  }
}
