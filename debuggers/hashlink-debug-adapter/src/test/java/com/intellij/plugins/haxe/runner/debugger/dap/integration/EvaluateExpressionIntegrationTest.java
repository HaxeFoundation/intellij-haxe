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

    assertEquals("qualified static in an operator expression", "3", evaluated(frameId, "Point.axes + 1"));
    assertEquals("two qualified statics", "4", evaluated(frameId, "Point.axes + Point.axes"));
    assertEquals("unqualified static in an operator expression", "3", evaluated(frameId, "axes + 1"));
    assertEquals("mixed with an instance field", "12", evaluated(frameId, "x + Point.axes"));
  }

  /** The packaged shape the user hits: a dotted class path as the root. */
  @Test
  public void packageQualifiedStaticsCombineInsideAnInstanceFrame() throws Exception {
    StoppedEvent stopped = runToBreakpoint("pkg/Deep.hx", DEEP_INSTANCE_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    assertEquals("dotted class path + literal", "100", evaluated(frameId, "pkg.Deep.marker + 1"));
    assertEquals("two dotted class paths", "141", evaluated(frameId, "pkg.Deep.marker + pkg.Deep.CONSTANT"));
    assertEquals("unqualified statics", "141", evaluated(frameId, "marker + CONSTANT"));
  }

  /** A static frame has no `this`, so these always worked — keep them working. */
  @Test
  public void staticFramesKeepCombiningStatics() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CONFIG, FIXTURE_STATICS_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    assertEquals("qualified", "8", evaluated(frameId, "Config.version + 1"));
    assertEquals("unqualified", "8", evaluated(frameId, "version + 1"));
  }
}
