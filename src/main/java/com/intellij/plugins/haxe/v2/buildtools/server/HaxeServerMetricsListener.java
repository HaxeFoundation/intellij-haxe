package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

/**
 * Project message bus topic for request-statistics changes: a request was
 * sent or answered, or a server's counters were reset. Delivered on the
 * thread that made the change, as often as requests go out; a UI subscriber
 * coalesces its refresh.
 */
@FunctionalInterface
public interface HaxeServerMetricsListener {

  @Topic.ProjectLevel
  Topic<HaxeServerMetricsListener> TOPIC =
    new Topic<>("Haxe server request metrics", HaxeServerMetricsListener.class, Topic.BroadcastDirection.NONE);

  void metricsChanged();

  /** A request to the server failed: its method, how long it took and why. Delivered on the requesting thread. */
  default void requestFailed(@NotNull String serverId, @NotNull String method, long millis, @NotNull String reason) {
  }
}
