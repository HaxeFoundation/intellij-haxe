package com.intellij.plugins.haxe.profiler.tracy.connect;

import com.intellij.plugins.haxe.profiler.tracy.wire.TracyProtocolVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("tracy receiver: version strategy")
public class TracyVersionStrategyTest {

  @Test
  @DisplayName("a pinned version is offered alone")
  public void testAPinnedVersionIsOfferedAlone() {
    TracyVersionStrategy strategy = TracyVersionStrategy.pinned(TracyProtocolVersion.V69);

    assertEquals(TracyProtocolVersion.V69, strategy.next(List.of()));
    assertNull(strategy.next(List.of(TracyProtocolVersion.V69)));
  }

  @Test
  @DisplayName("the probe ladder walks the order until every version was refused")
  public void testTheProbeLadderWalksTheOrderUntilEveryVersionWasRefused() {
    TracyVersionStrategy strategy = TracyVersionStrategy.detect(() -> null);

    assertEquals(TracyProtocolVersion.V76, strategy.next(List.of()));
    assertEquals(TracyProtocolVersion.V74, strategy.next(List.of(TracyProtocolVersion.V76)));
    assertEquals(TracyProtocolVersion.V82, strategy.next(List.of(TracyProtocolVersion.V76, TracyProtocolVersion.V74)));
    assertNull(strategy.next(TracyProtocolVersion.PROBE_ORDER));
  }

  @Test
  @DisplayName("an announced version jumps the ladder, even when it arrives mid-way")
  public void testAnAnnouncedVersionJumpsTheLadderEvenWhenItArrivesMidWay() {
    AtomicReference<TracyProtocolVersion> announced = new AtomicReference<>();
    TracyVersionStrategy strategy = TracyVersionStrategy.detect(announced::get);
    assertEquals(TracyProtocolVersion.V76, strategy.next(List.of()));

    announced.set(TracyProtocolVersion.V69);
    assertEquals(TracyProtocolVersion.V69, strategy.next(List.of(TracyProtocolVersion.V76)));
    assertEquals(TracyProtocolVersion.V74, strategy.next(List.of(TracyProtocolVersion.V76, TracyProtocolVersion.V69)));
  }
}
