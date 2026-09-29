package com.intellij.plugins.haxe.debugger.hxcppserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Object labels via the debuggee's own toString(), toggled LIVE through the
 * custom {@code custom/setToStringRendering} request (the IDE's
 * Variables-view gear toggle): off (the default) labels every object with
 * its class name and runs NO user code; on, a class chain that declares
 * toString renders through it, and a THROWING toString degrades back to the
 * class name without harming the session. A self-recursing toString is
 * deliberately untested against the real runtime - on hxcpp that stack
 * overflow kills the process before any handler runs (probe-verified),
 * which is exactly the risk the off-default guards; the eval-target unit
 * tests (ValuesTest) cover the recursion path where it IS catchable.
 */
@DisplayName("HXCPP debugger: to string rendering (integration)")
public class ToStringRenderingIT {

  @Test
  @DisplayName("labels follow the live toggle")
  public void labelsFollowTheLiveToggle() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("tostring")) {
      int threadId = stopAtToStringLine(session);
      int frameId = session.topFrame(threadId).getId();

      // default OFF: class names, even for classes that declare toString
      List<Variable> before = session.variables(session.localsReference(frameId));
      assertEquals("Labeled", session.variable(before, "labeled").getValue(), "off: class-name label despite a declared toString");
      assertEquals("PlainBox", session.variable(before, "plain").getValue(), "off: plain class label");

      // toggle ON (live, mid-stop): the SAME stop re-lists with new labels
      assertTrue(session.request(SetToStringRenderingRequest.of(true)).isSuccess(), "toggle on accepted");

      List<Variable> on = session.variables(session.localsReference(frameId));
      assertEquals("Labeled#7", session.variable(on, "labeled").getValue(), "on: the object's own toString");
      assertEquals("PlainBox", session.variable(on, "plain").getValue(), "on: no toString declared still means the class name (no code runs)");
      assertEquals("MoodyLabel", session.variable(on, "moody").getValue(), "on: a THROWING toString degrades to the class name");

      // maps switch from the entry count to their own content preview. The
      // preview comes from the map's std toString, whose punctuation varies by
      // haxe version ([k => v] on 4.3+, { k => v } on 4.1/4.2), so assert only
      // that the entries' keys and values are present — never the separators.
      String mapPreview = session.variable(on, "meta").getValue();
      boolean showsEveryEntry = mapPreview.contains("build") && mapPreview.contains("92")
                                && mapPreview.contains("name") && mapPreview.contains("7");
      assertTrue(showsEveryEntry, "on: a map renders its entries as a content preview, got: " + mapPreview);

      // toggle OFF again: back to class names - the flag is truly live
      assertTrue(session.request(SetToStringRenderingRequest.of(false)).isSuccess(), "toggle off accepted");
      List<Variable> after = session.variables(session.localsReference(frameId));
      assertEquals("Labeled", session.variable(after, "labeled").getValue(), "off again: class-name label");

      // the throwing toString left the server healthy: run to a clean exit
      session.resume(threadId);
      assertEquals(0, session.awaitExit(), "clean exit after the toggles");
    }
  }

  @Test
  @DisplayName("maps list their entries")
  public void mapsListTheirEntries() throws Exception {
    // A haxe.ds map's raw fields are its native hash handle ("h = Dynamic"),
    // which is what used to render — entries must list instead, like the
    // HashLink adapter's map handling (found live on an OpenFL StringMap).
    try (FixtureSession session = FixtureSession.launchScenario("tostring")) {
      int threadId = stopAtToStringLine(session);
      int frameId = session.topFrame(threadId).getId();

      List<Variable> locals = session.variables(session.localsReference(frameId));
      Variable meta = session.variable(locals, "meta");
      assertEquals("Map(2)", meta.getValue(), "entry-count summary, not the class name");
      assertTrue(meta.getVariablesReference() > 0, "a populated map expands");

      List<Variable> entries = session.variables(meta.getVariablesReference());
      assertEquals("92", session.variable(entries, "\"build\"").getValue());
      assertEquals("7", session.variable(entries, "\"name\"").getValue());

      session.resume(threadId);
      assertEquals(0, session.awaitExit(), "clean exit");
    }
  }

  /** Runs the session to the breakpoint on the tostring fixture line and returns the stopped thread id. */
  private static int stopAtToStringLine(FixtureSession session) throws Exception {
    session.initialize();
    session.setBreakpoints(FixtureSession.EX_SOURCE, new int[]{FixtureSession.TOSTRING_LINE}, null);
    session.configurationDone();
    return session.stoppedThread(session.awaitStopped());
  }
}
