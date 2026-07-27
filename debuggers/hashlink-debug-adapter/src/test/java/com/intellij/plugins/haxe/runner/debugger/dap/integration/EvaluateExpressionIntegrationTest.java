package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import org.junit.jupiter.api.Test;

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

    assertEquals("3", evaluated(frameId, "Point.axes + 1"), "qualified static in an operator expression");
    assertEquals("4", evaluated(frameId, "Point.axes + Point.axes"), "two qualified statics");
    assertEquals("3", evaluated(frameId, "axes + 1"), "unqualified static in an operator expression");
    assertEquals("12", evaluated(frameId, "x + Point.axes"), "mixed with an instance field");
  }

  /** The packaged shape the user hits: a dotted class path as the root. */
  @Test
  public void packageQualifiedStaticsCombineInsideAnInstanceFrame() throws Exception {
    StoppedEvent stopped = runToBreakpoint("pkg/Deep.hx", DEEP_INSTANCE_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    assertEquals("100", evaluated(frameId, "pkg.Deep.marker + 1"), "dotted class path + literal");
    assertEquals("141", evaluated(frameId, "pkg.Deep.marker + pkg.Deep.CONSTANT"), "two dotted class paths");
    assertEquals("141", evaluated(frameId, "marker + CONSTANT"), "unqualified statics");
  }

  /** A static frame has no `this`, so these always worked — keep them working. */
  @Test
  public void staticFramesKeepCombiningStatics() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CONFIG, FIXTURE_STATICS_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());

    assertEquals("8", evaluated(frameId, "Config.version + 1"), "qualified");
    assertEquals("8", evaluated(frameId, "version + 1"), "unqualified");
  }
}
