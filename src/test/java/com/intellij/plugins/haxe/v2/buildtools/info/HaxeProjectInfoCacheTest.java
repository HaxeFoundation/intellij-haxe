package com.intellij.plugins.haxe.v2.buildtools.info;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeProjectInfoCache.Key;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeProjectInfoCache.Outcome;
import com.intellij.testFramework.PlatformTestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Build tools: project info cache")
public class HaxeProjectInfoCacheTest extends HaxeCodeInsightFixtureTestCase {
  private static final Key KEY = new Key("/project/project.xml", "html5", "haxelib");
  private static final long STAMP = 1;

  private HaxeProjectInfoCache<String> cache;
  private final CountDownLatch release = new CountDownLatch(1);

  @Override
  protected String getBasePath() {
    return "";
  }

  @BeforeEach
  public void createCache() {
    cache = new HaxeProjectInfoCache<>(getProject(), "HaxeProjectInfoCacheTest", null);
  }

  @AfterEach
  public void shutDownCache() {
    release.countDown();
    cache.shutdown();
  }

  @Test
  @DisplayName("callback asked again while in flight runs once")
  public void testCallbackAskedAgainWhileInFlightRunsOnce() {
    AtomicInteger refreshes = new AtomicInteger();
    Runnable refresh = refreshes::incrementAndGet;

    cache.getCachedOrSchedule(KEY, STAMP, this::evaluateAfterRelease, refresh);
    cache.getCachedOrSchedule(KEY, STAMP, this::evaluateAfterRelease, refresh);
    cache.getCachedOrSchedule(KEY, STAMP, this::evaluateAfterRelease, refresh);
    landEvaluation(refreshes);

    assertEquals(1, refreshes.get());
  }

  @Test
  @DisplayName("every distinct callback runs")
  public void testEveryDistinctCallbackRuns() {
    AtomicInteger refreshes = new AtomicInteger();

    cache.getCachedOrSchedule(KEY, STAMP, this::evaluateAfterRelease, refreshes::incrementAndGet);
    cache.getCachedOrSchedule(KEY, STAMP, this::evaluateAfterRelease, refreshes::incrementAndGet);
    landEvaluation(refreshes);

    assertEquals(2, refreshes.get());
  }

  /** Lets the blocked evaluation finish and pumps the EDT until its callbacks have run. */
  private void landEvaluation(AtomicInteger refreshes) {
    release.countDown();
    PlatformTestUtil.waitWithEventsDispatching("the evaluation never landed", () -> refreshes.get() > 0, 10);
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
  }

  /** An evaluation that blocks until the test releases it, so later asks find it in flight. */
  private Outcome<String> evaluateAfterRelease() {
    try {
      release.await(10, TimeUnit.SECONDS);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return new Outcome<>("evaluated", true);
  }
}
