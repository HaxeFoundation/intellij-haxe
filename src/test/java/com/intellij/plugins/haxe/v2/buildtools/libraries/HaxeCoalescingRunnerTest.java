package com.intellij.plugins.haxe.v2.buildtools.libraries;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Build tools: coalescing runner")
public class HaxeCoalescingRunnerTest {

  /** The completion handles of the runs started so far, in order. */
  private final List<Runnable> runs = new ArrayList<>();
  private final List<String> finished = new ArrayList<>();
  private final HaxeCoalescingRunner runner = new HaxeCoalescingRunner(runs::add);

  @Test
  @DisplayName("requests during a run fold into one follow-up run")
  public void testRequestsDuringARunFoldIntoOneFollowUpRun() {
    runner.request(() -> finished.add("first"));
    runner.request(() -> finished.add("second"));
    runner.request(() -> finished.add("third"));
    assertEquals(1, runs.size(), "one run at a time");
    assertEquals(List.of(), finished);

    runs.get(0).run();
    assertEquals(List.of("first"), finished, "the first request's callback follows its run");
    assertEquals(2, runs.size(), "the two requests made during the run share one follow-up");

    runs.get(1).run();
    assertEquals(List.of("first", "second", "third"), finished);
    assertEquals(2, runs.size(), "nothing was requested during the follow-up");
  }

  @Test
  @DisplayName("a request without a callback still gets a follow-up run")
  public void testARequestWithoutACallbackStillGetsAFollowUpRun() {
    runner.request(null);
    runner.request(null);
    assertEquals(1, runs.size());

    runs.get(0).run();
    assertEquals(2, runs.size());

    runs.get(1).run();
    assertEquals(2, runs.size(), "no request arrived during the follow-up");
  }

  @Test
  @DisplayName("a request after the runs completed starts a new run")
  public void testARequestAfterTheRunsCompletedStartsANewRun() {
    runner.request(null);
    runs.get(0).run();

    runner.request(null);

    assertEquals(2, runs.size());
  }

  @Test
  @DisplayName("a callback requesting again gets one more run")
  public void testACallbackRequestingAgainGetsOneMoreRun() {
    runner.request(() -> runner.request(() -> finished.add("again")));

    runs.get(0).run();
    assertEquals(2, runs.size(), "the request made from the callback starts a run after the callback");

    runs.get(1).run();
    assertEquals(List.of("again"), finished);
  }
}
