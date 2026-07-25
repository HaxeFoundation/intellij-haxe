package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.EvaluateArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.EvaluateRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.EvaluateResponse;
import org.junit.Test;

/**
 * Operator expressions over statics. A bare path is read through the
 * evaluator's own tolerant lookup, but an OPERATOR expression reads each
 * operand through the write-target resolver — so a root that is not a local
 * and not a field of `this` (a class name!) took a different code path, and
 * probing "is this.<root> a field?" threw instead of falling through to the
 * class-prefix resolution. In an instance frame that turned every
 * `Cls.member + x` into `"this.<pkg-root>" cannot be resolved to a writable
 * location`; a static frame has no `this` to probe and was unaffected.
 */
public class EvaluateExpressionIntegrationTest extends DapIntegrationTestBase {
  private static final int DEEP_INSTANCE_LINE = 21; // pkg/Deep.hx readMarker()

  @Test
  public void classQualifiedStaticsCombineInsideAnInstanceFrame() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_POINT, FIXTURE_POINT_METHOD_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    assertEquals("qualified static in an operator expression", "3", evaluate(frameId, "Point.axes + 1"));
    assertEquals("two qualified statics", "4", evaluate(frameId, "Point.axes + Point.axes"));
    assertEquals("unqualified static in an operator expression", "3", evaluate(frameId, "axes + 1"));
    assertEquals("mixed with an instance field", "12", evaluate(frameId, "x + Point.axes"));
  }

  /** The packaged shape the user hits: a dotted class path as the root. */
  @Test
  public void packageQualifiedStaticsCombineInsideAnInstanceFrame() throws Exception {
    StoppedEvent stopped = runToBreakpoint("pkg/Deep.hx", DEEP_INSTANCE_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    assertEquals("dotted class path + literal", "100", evaluate(frameId, "pkg.Deep.marker + 1"));
    assertEquals("two dotted class paths", "141", evaluate(frameId, "pkg.Deep.marker + pkg.Deep.CONSTANT"));
    assertEquals("unqualified statics", "141", evaluate(frameId, "marker + CONSTANT"));
  }

  /** A static frame has no `this`, so these always worked — keep them working. */
  @Test
  public void staticFramesKeepCombiningStatics() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CONFIG, FIXTURE_STATICS_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    assertEquals("qualified", "8", evaluate(frameId, "Config.version + 1"));
    assertEquals("unqualified", "8", evaluate(frameId, "version + 1"));
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
