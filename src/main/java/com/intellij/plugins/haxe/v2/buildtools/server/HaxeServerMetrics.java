package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.display.client.HaxeDisplayClient.RequestObserver;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Request statistics per compilation-server instance, measured by the IDE
 * because the server reports no timings over the protocol: the cumulative
 * counts and timings of answered requests, and the requests sent but not
 * yet answered (waiting for the channel or in flight at the server). Every
 * request through the display client counts, plain compiles included. The
 * client's observer ({@link #observerFor}) feeds the counters, the server
 * console's status view shows them, and every change is announced on
 * {@link HaxeServerMetricsListener#TOPIC}.
 */
@Service(Service.Level.PROJECT)
public final class HaxeServerMetrics {

  /** One server's counters at a point in time. */
  public record Snapshot(long requests, long failures, long lastMillis, long averageMillis, long pending) {
    public static final Snapshot EMPTY = new Snapshot(0, 0, 0, 0, 0);
  }

  private static final class Counters {
    long requests;
    long failures;
    long lastMillis;
    long totalMillis;
    long pending;
  }

  private final Project project;
  private final Map<String, Counters> byServer = new ConcurrentHashMap<>();

  public HaxeServerMetrics(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  public static HaxeServerMetrics getInstance(@NotNull Project project) {
    return project.getService(HaxeServerMetrics.class);
  }

  /** The display client's observer that counts one server's requests here. */
  @NotNull
  public RequestObserver observerFor(@NotNull String serverId) {
    return new RequestObserver() {
      @Override
      public void beforeRequest(String method) {
        requestStarted(serverId);
      }

      @Override
      public void afterRequest(String method, long millis, boolean success) {
        requestFinished(serverId, millis, success);
      }
    };
  }

  public void requestStarted(@NotNull String serverId) {
    Counters counters = countersOf(serverId);
    synchronized (counters) {
      counters.pending++;
    }
    announce();
  }

  /** A request started before its server's counters were cleared (a restart) has nothing pending left to release. */
  public void requestFinished(@NotNull String serverId, long millis, boolean success) {
    Counters counters = countersOf(serverId);
    synchronized (counters) {
      counters.requests++;
      if (!success) counters.failures++;
      counters.lastMillis = millis;
      counters.totalMillis += millis;
      counters.pending = Math.max(0, counters.pending - 1);
    }
    announce();
  }

  /** Forgets one server's counters, so a stopped or restarted server starts from zero. */
  public void clear(@NotNull String serverId) {
    byServer.remove(serverId);
    announce();
  }

  @NotNull
  public Snapshot snapshot(@NotNull String serverId) {
    Counters counters = byServer.get(serverId);
    if (counters == null) return Snapshot.EMPTY;
    synchronized (counters) {
      long average = counters.requests == 0 ? 0 : counters.totalMillis / counters.requests;
      return new Snapshot(counters.requests, counters.failures, counters.lastMillis, average, counters.pending);
    }
  }

  @NotNull
  private Counters countersOf(@NotNull String serverId) {
    return byServer.computeIfAbsent(serverId, ignored -> new Counters());
  }

  private void announce() {
    if (project.isDisposed()) return;
    project.getMessageBus().syncPublisher(HaxeServerMetricsListener.TOPIC).metricsChanged();
  }
}
