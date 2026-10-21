package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.display.client.HaxeDisplayClient.RequestObserver;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** The IDE-side request counters: pending follows the observer's two calls, and a cleared server never goes negative. */
@DisplayName("Compilation server: request metrics")
public class HaxeServerMetricsTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("pending counts the requests sent and not yet answered")
  public void testPendingCountsTheRequestsSentAndNotYetAnswered() {
    HaxeServerMetrics metrics = HaxeServerMetrics.getInstance(myFixture.getProject());
    RequestObserver observer = metrics.observerFor("pending-server");

    observer.beforeRequest("display/hover");
    observer.beforeRequest("display/hover");
    assertEquals(2, metrics.snapshot("pending-server").pending());

    observer.afterRequest("display/hover", 40, null);
    observer.afterRequest("display/hover", 60, new DisplayRequestException("connection refused"));
    assertEquals(new HaxeServerMetrics.Snapshot(2, 1, 60, 50, 0), metrics.snapshot("pending-server"));
  }

  @Test
  @DisplayName("a failed request is announced with its reason")
  public void testAFailedRequestIsAnnouncedWithItsReason() {
    Project project = myFixture.getProject();
    List<String> announced = new ArrayList<>();
    HaxeServerMetricsListener listener = new HaxeServerMetricsListener() {
      @Override
      public void metricsChanged() {
      }

      @Override
      public void requestFailed(@NotNull String serverId, @NotNull String method, long millis, @NotNull String reason) {
        announced.add(serverId + " " + method + " " + millis + " " + reason);
      }
    };
    project.getMessageBus().connect(myFixture.getTestRootDisposable()).subscribe(HaxeServerMetricsListener.TOPIC, listener);
    RequestObserver observer = HaxeServerMetrics.getInstance(project).observerFor("failing-server");

    observer.afterRequest("display/hover", 40, null);
    observer.afterRequest("display/hover", 60, new DisplayRequestException("connection refused"));

    assertEquals(List.of("failing-server display/hover 60 connection refused"), announced);
  }

  @Test
  @DisplayName("a request answered after its server was cleared leaves nothing pending")
  public void testARequestAnsweredAfterItsServerWasClearedLeavesNothingPending() {
    HaxeServerMetrics metrics = HaxeServerMetrics.getInstance(myFixture.getProject());
    RequestObserver observer = metrics.observerFor("cleared-server");
    observer.beforeRequest("display/hover");

    metrics.clear("cleared-server");
    observer.afterRequest("display/hover", 40, new DisplayRequestException("timed out"));

    assertEquals(new HaxeServerMetrics.Snapshot(1, 1, 40, 40, 0), metrics.snapshot("cleared-server"));
  }

  @Test
  @DisplayName("every change is announced on the metrics topic")
  public void testEveryChangeIsAnnouncedOnTheMetricsTopic() {
    Project project = myFixture.getProject();
    AtomicInteger announced = new AtomicInteger();
    project.getMessageBus()
      .connect(myFixture.getTestRootDisposable())
      .subscribe(HaxeServerMetricsListener.TOPIC, announced::incrementAndGet);
    HaxeServerMetrics metrics = HaxeServerMetrics.getInstance(project);

    metrics.requestStarted("announced-server");
    metrics.requestFinished("announced-server", 1, true);
    metrics.clear("announced-server");

    assertEquals(3, announced.get());
  }
}
