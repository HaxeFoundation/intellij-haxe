package com.intellij.plugins.haxe.v2.buildtools.info;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildEventListener;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Build tools: build tool failures")
public class HaxeBuildToolFailuresTest extends HaxeLightFixtureTestCase {

  private static final String PROJECT_XML_PATH = "/work/app/project.xml";
  private static final String NMML_PATH = "/work/app/build.nmml";

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("keeps the last failure per file until a success clears it")
  public void testKeepsTheLastFailurePerFileUntilASuccessClearsIt() {
    HaxeBuildToolFailures failures = HaxeBuildToolFailures.getInstance(getProject());

    failures.record(PROJECT_XML_PATH, "lime display failed: missing lib");
    failures.record(NMML_PATH, "nme prepare failed: exit code 1");
    assertEquals("lime display failed: missing lib", failures.lastFailure(PROJECT_XML_PATH));
    assertEquals(Map.of(PROJECT_XML_PATH, "lime display failed: missing lib", NMML_PATH, "nme prepare failed: exit code 1"),
                 failures.snapshot());

    failures.record(PROJECT_XML_PATH, null);
    assertNull(failures.lastFailure(PROJECT_XML_PATH));
    assertEquals(Map.of(NMML_PATH, "nme prepare failed: exit code 1"), failures.snapshot());

    failures.record(NMML_PATH, null);
  }

  @Test
  @DisplayName("repaints the tool window only when a failure changes")
  public void testRepaintsTheToolWindowOnlyWhenAFailureChanges() {
    AtomicInteger repaints = new AtomicInteger();
    HaxeBuildEventListener listener = new HaxeBuildEventListener() {
      @Override
      public void failureRecorded(@NotNull String key) {
        repaints.incrementAndGet();
      }
    };
    getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(HaxeBuildEventListener.TOPIC, listener);
    HaxeBuildToolFailures failures = HaxeBuildToolFailures.getInstance(getProject());

    failures.record(NMML_PATH, null);
    assertEquals(0, repaints.get(), "a success on a file without failure changes nothing");

    failures.record(NMML_PATH, "nme prepare failed: exit code 1");
    failures.record(NMML_PATH, "nme prepare failed: exit code 1");
    assertEquals(1, repaints.get(), "the same failure again is no change");

    failures.record(NMML_PATH, null);
    assertEquals(2, repaints.get(), "clearing a failure is a change");
  }
}
