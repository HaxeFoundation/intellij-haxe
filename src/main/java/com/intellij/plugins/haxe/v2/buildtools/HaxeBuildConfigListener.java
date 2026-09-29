package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.plugins.haxe.v2.buildtools.server.HaxeContextFailures;
import com.intellij.util.messages.Topic;

/**
 * Project-bus notification that the Haxe build configuration was (re)synced -
 * build files reparsed, libraries resynced. UI showing derived state (the tool
 * window tree) refreshes on it.
 * <p>
 * Delivered on the PUBLISHER's thread - the sync/workspace/settings publishers
 * fire on the EDT, {@link HaxeContextFailures#record} fires from the background
 * threads recording compiler-request outcomes. Subscribers must be safe to
 * call from any thread: dispatch to {@code invokeLater} or a non-blocking read
 * action before touching UI or PSI (both current subscribers do).
 */
@FunctionalInterface
public interface HaxeBuildConfigListener {

  @Topic.ProjectLevel
  Topic<HaxeBuildConfigListener> TOPIC =
    new Topic<>("Haxe build configuration", HaxeBuildConfigListener.class, Topic.BroadcastDirection.NONE);

  void buildConfigurationChanged();
}
