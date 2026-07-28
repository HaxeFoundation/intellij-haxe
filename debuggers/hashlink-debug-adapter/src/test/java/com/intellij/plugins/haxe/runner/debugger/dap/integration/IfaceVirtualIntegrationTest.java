package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Interfaces and virtuals. genhl gives a class one EMPTY-NAMED HVirtual field
 * per implemented interface — the runtime cache for that interface view
 * (hl_to_virtual) — and an interface-typed local is a vvirtual wrapping the
 * instance. Neither is useful as a structural member list: the interface's
 * own members are properties and methods, whose virtual slots hold no data
 * (a method's slot holds the function's CODE pointer, which must never be
 * dereferenced as a value). Both therefore resolve to the INSTANCE.
 */
@DisplayName("HashLink debugger: iface virtual (integration)")
public class IfaceVirtualIntegrationTest extends DapIntegrationTestBase {

  @Test
  @DisplayName("an interface typed local shows the instance and its values")
  public void anInterfaceTypedLocalShowsTheInstanceAndItsValues() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_IFACE, FIXTURE_IFACE_LINE);
    List<Variable> locals = topFrameVariables(stopped.getBody().getThreadId());

    Variable asIface = findVariable(locals, "asIface");
    assertNotNull(asIface, "local asIface present");
    assertEquals("AnimTask", asIface.getValue(), "the interface view resolves to the runtime class");
    assertTrue(asIface.getVariablesReference() > 0, "the instance is expandable");

    // the instance's own fields, with real values - not the interface's
    // properties/methods, which carry none
    Map<String, String> fields = variablesByName(asIface.getVariablesReference());
    assertEquals("8", fields.get("baseId"), "inherited field");
    assertEquals("4", fields.get("count"), "own field");
    assertEquals("\"anim4\"", fields.get("name"), "own field");
  }

  /** The per-interface cache field is compiler-internal and stays hidden. */
  @Test
  @DisplayName("the hidden interface cache field is not shown")
  public void theHiddenInterfaceCacheFieldIsNotShown() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_IFACE, FIXTURE_IFACE_LINE);
    List<Variable> locals = topFrameVariables(stopped.getBody().getThreadId());

    Variable task = findVariable(locals, "task");
    assertNotNull(task, "local task present");
    List<Variable> children = variables(task.getVariablesReference());
    for (Variable child : children) {
      assertFalse(child.getName() == null || child.getName().isEmpty(), "no empty-named field is shown (" + child.getValue() + ")");
    }
    Map<String, String> fields = variablesByName(task.getVariablesReference());
    assertEquals("4", fields.get("count"), "the object's own fields still decode");
    assertEquals("8", fields.get("baseId"), "including the inherited one");
  }
}
