package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cumulative request statistics per compilation-server instance. The IDE
 * measures them itself, because the server reports no timings over the
 * protocol. The display client's request observer feeds them, and the
 * server console's status view shows them.
 */
@Service(Service.Level.PROJECT)
public final class HaxeServerMetrics {

  /** One server's counters at a point in time. */
  public record Snapshot(long requests, long failures, long lastMillis, long averageMillis) {
    public static final Snapshot EMPTY = new Snapshot(0, 0, 0, 0);
  }

  private static final class Counters {
    long requests;
    long failures;
    long lastMillis;
    long totalMillis;
  }

  private final Map<String, Counters> byServer = new ConcurrentHashMap<>();

  @NotNull
  public static HaxeServerMetrics getInstance(@NotNull Project project) {
    return project.getService(HaxeServerMetrics.class);
  }

  public void record(@NotNull String serverId, long millis, boolean success) {
    Counters counters = byServer.computeIfAbsent(serverId, ignored -> new Counters());
    synchronized (counters) {
      counters.requests++;
      if (!success) counters.failures++;
      counters.lastMillis = millis;
      counters.totalMillis += millis;
    }
  }

  /** Forgets one server's counters, so a stopped or restarted server starts from zero. */
  public void clear(@NotNull String serverId) {
    byServer.remove(serverId);
  }

  @NotNull
  public Snapshot snapshot(@NotNull String serverId) {
    Counters counters = byServer.get(serverId);
    if (counters == null) return Snapshot.EMPTY;
    synchronized (counters) {
      long average = counters.requests == 0 ? 0 : counters.totalMillis / counters.requests;
      return new Snapshot(counters.requests, counters.failures, counters.lastMillis, average);
    }
  }
}
