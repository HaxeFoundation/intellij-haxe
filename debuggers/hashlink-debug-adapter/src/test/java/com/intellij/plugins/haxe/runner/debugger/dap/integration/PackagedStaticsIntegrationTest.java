package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * A class's own statics resolve WITHOUT a class prefix, the way Haxe source
 * writes them. hl `$`-prefixes the LAST segment of the statics container
 * (`pkg.Cls` keeps its statics on `pkg.$Cls`), so a naive `"$" + name` finds
 * the container of a top-level class but never of a PACKAGED one: an instance
 * frame there had no statics at all — neither in evaluate nor in the Statics
 * scope — while its static methods worked (those are bindings of the
 * container itself).
 */
public class PackagedStaticsIntegrationTest extends DapIntegrationTestBase {
  private static final int DEEP_INSTANCE_LINE = 21; // pkg/Deep.hx readMarker()

  @Test
  public void unqualifiedStaticsResolveInAPackagedInstanceFrame() throws Exception {
    StoppedEvent stopped = runToBreakpoint("pkg/Deep.hx", DEEP_INSTANCE_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    assertEquals("99", evaluated(frameId, "marker"), "static var, unqualified");
    assertEquals("42", evaluated(frameId, "CONSTANT"), "static final, unqualified");
    assertEquals("99", evaluated(frameId, "pkg.Deep.marker"), "still resolvable qualified");
  }

  /** The same mapping drives the Statics scope, so it must appear here too. */
  @Test
  public void theStaticsScopeAppearsInAPackagedInstanceFrame() throws Exception {
    StoppedEvent stopped = runToBreakpoint("pkg/Deep.hx", DEEP_INSTANCE_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    Scope statics = scopeByPrefix(frameId, "Statics");
    assertNotNull(statics, "a packaged instance frame has a Statics scope");
    Map<String, String> byName = variablesByName(statics.getVariablesReference());
    assertEquals("99", byName.get("marker"), "the class's static var");
    assertEquals("42", byName.get("CONSTANT"), "the class's static final");
  }

  /** Guards the shapes that already worked: top-level, and static frames. */
  @Test
  public void unqualifiedStaticsKeepWorkingElsewhere() throws Exception {
    StoppedEvent atInstance = runToBreakpoint(FIXTURE_POINT, FIXTURE_POINT_METHOD_LINE);
    assertEquals("2", evaluated(topFrameId(atInstance.getBody().getThreadId()), "axes"), "top-level class, instance frame");

    StoppedEvent atStatic = runToBreakpoint(FIXTURE_CONFIG, FIXTURE_STATICS_LINE);
    assertEquals("7", evaluated(topFrameId(atStatic.getBody().getThreadId()), "version"), "top-level class, static frame");
  }
}
