package com.intellij.plugins.haxe.debugger.hxcppserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.util.List;
import org.junit.Test;

/**
 * Object labels via the debuggee's own toString(), toggled LIVE through the
 * custom {@code intellij/setToStringRendering} request (the IDE's
 * Variables-view gear toggle): off (the default) labels every object with
 * its class name and runs NO user code; on, a class chain that declares
 * toString renders through it, and a THROWING toString degrades back to the
 * class name without harming the session. A self-recursing toString is
 * deliberately untested against the real runtime - on hxcpp that stack
 * overflow kills the process before any handler runs (probe-verified),
 * which is exactly the risk the off-default guards; the eval-target unit
 * tests (ValuesTest) cover the recursion path where it IS catchable.
 */
public class ToStringRenderingIT {

  @Test
  public void labelsFollowTheLiveToggle() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("tostring")) {
      session.initialize();
      session.setBreakpoints(FixtureSession.EX_SOURCE, new int[]{FixtureSession.TOSTRING_LINE}, null);
      session.configurationDone();
      int threadId = session.stoppedThread(session.awaitStopped());
      int frameId = session.topFrame(threadId).getId();

      // default OFF: class names, even for classes that declare toString
      List<Variable> before = session.variables(session.localsReference(frameId));
      assertEquals("off: class-name label despite a declared toString",
                   "Labeled", session.variable(before, "labeled").getValue());
      assertEquals("off: plain class label", "PlainBox", session.variable(before, "plain").getValue());

      // toggle ON (live, mid-stop): the SAME stop re-lists with new labels
      assertTrue("toggle on accepted",
                 session.request(SetToStringRenderingRequest.of(true)).isSuccess());
      List<Variable> on = session.variables(session.localsReference(frameId));
      assertEquals("on: the object's own toString", "Labeled#7", session.variable(on, "labeled").getValue());
      assertEquals("on: no toString declared still means the class name (no code runs)",
                   "PlainBox", session.variable(on, "plain").getValue());
      assertEquals("on: a THROWING toString degrades to the class name",
                   "MoodyLabel", session.variable(on, "moody").getValue());
      // maps switch from the entry count to their own content preview. The
      // preview comes from the map's std toString, whose punctuation varies by
      // haxe version ([k => v] on 4.3+, { k => v } on 4.1/4.2), so assert only
      // that the entries' keys and values are present — never the separators.
      String mapPreview = session.variable(on, "meta").getValue();
      assertTrue("on: a map renders its entries as a content preview, got: " + mapPreview,
                 mapPreview.contains("build") && mapPreview.contains("92")
                 && mapPreview.contains("name") && mapPreview.contains("7"));

      // toggle OFF again: back to class names - the flag is truly live
      assertTrue("toggle off accepted",
                 session.request(SetToStringRenderingRequest.of(false)).isSuccess());
      List<Variable> after = session.variables(session.localsReference(frameId));
      assertEquals("off again: class-name label", "Labeled", session.variable(after, "labeled").getValue());

      // the throwing toString left the server healthy: run to a clean exit
      session.resume(threadId);
      assertEquals("clean exit after the toggles", 0, session.awaitExit());
    }
  }

  @Test
  public void mapsListTheirEntries() throws Exception {
    // A haxe.ds map's raw fields are its native hash handle ("h = Dynamic"),
    // which is what used to render — entries must list instead, like the
    // HashLink adapter's map handling (found live on an OpenFL StringMap).
    try (FixtureSession session = FixtureSession.launchScenario("tostring")) {
      session.initialize();
      session.setBreakpoints(FixtureSession.EX_SOURCE, new int[]{FixtureSession.TOSTRING_LINE}, null);
      session.configurationDone();
      int threadId = session.stoppedThread(session.awaitStopped());
      int frameId = session.topFrame(threadId).getId();

      List<Variable> locals = session.variables(session.localsReference(frameId));
      Variable meta = session.variable(locals, "meta");
      assertEquals("entry-count summary, not the class name", "Map(2)", meta.getValue());
      assertTrue("a populated map expands", meta.getVariablesReference() > 0);

      List<Variable> entries = session.variables(meta.getVariablesReference());
      assertEquals("92", session.variable(entries, "\"build\"").getValue());
      assertEquals("7", session.variable(entries, "\"name\"").getValue());

      session.resume(threadId);
      assertEquals("clean exit", 0, session.awaitExit());
    }
  }
}
