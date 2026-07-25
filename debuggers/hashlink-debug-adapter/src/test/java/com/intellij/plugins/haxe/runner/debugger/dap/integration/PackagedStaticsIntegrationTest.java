package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.EvaluateArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.EvaluateRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.EvaluateResponse;
import java.util.Map;
import org.junit.Test;

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

    assertEquals("static var, unqualified", "99", evaluate(frameId, "marker"));
    assertEquals("static final, unqualified", "42", evaluate(frameId, "CONSTANT"));
    assertEquals("still resolvable qualified", "99", evaluate(frameId, "pkg.Deep.marker"));
  }

  /** The same mapping drives the Statics scope, so it must appear here too. */
  @Test
  public void theStaticsScopeAppearsInAPackagedInstanceFrame() throws Exception {
    StoppedEvent stopped = runToBreakpoint("pkg/Deep.hx", DEEP_INSTANCE_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    Scope statics = scopeByPrefix(frameId, "Statics");
    assertNotNull("a packaged instance frame has a Statics scope", statics);
    Map<String, String> byName = variablesByName(statics.getVariablesReference());
    assertEquals("the class's static var", "99", byName.get("marker"));
    assertEquals("the class's static final", "42", byName.get("CONSTANT"));
  }

  /** Guards the shapes that already worked: top-level, and static frames. */
  @Test
  public void unqualifiedStaticsKeepWorkingElsewhere() throws Exception {
    StoppedEvent atInstance = runToBreakpoint(FIXTURE_POINT, FIXTURE_POINT_METHOD_LINE);
    assertEquals("top-level class, instance frame", "2",
                 evaluate(topFrameId(atInstance.getBody().getThreadId()), "axes"));

    StoppedEvent atStatic = runToBreakpoint(FIXTURE_CONFIG, FIXTURE_STATICS_LINE);
    assertEquals("top-level class, static frame", "7",
                 evaluate(topFrameId(atStatic.getBody().getThreadId()), "version"));
  }

  private String evaluate(int frameId, String expression) throws Exception {
    EvaluateRequest request = new EvaluateRequest();
    EvaluateArguments args = new EvaluateArguments();
    args.setExpression(expression);
    args.setFrameId(frameId);
    request.setArguments(args);
    Response response = request(request);
    assertTrue("evaluate '" + expression + "': " + response.getMessage(), response.isSuccess());
    return ((EvaluateResponse)response).getBody().getResult();
  }
}
