package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.Nullable;

/**
 * Fired on the project bus whenever a build-configuration store mutates
 * (build files, active build file, target selection, environment). Consumers
 * deriving state from these stores (define context, tool window) subscribe
 * and invalidate; new invalidation sources publish the same topic instead of
 * every consumer learning about every source.
 */
public interface HaxeBuildSettingsListener {
  Topic<HaxeBuildSettingsListener> TOPIC = Topic.create("haxe build settings changed", HaxeBuildSettingsListener.class);

  void buildSettingsChanged();

  /** Announces a store mutation; a store built without a project (state tests) announces nothing. */
  static void publish(@Nullable Project project) {
    if (project != null) {
      project.getMessageBus().syncPublisher(TOPIC).buildSettingsChanged();
    }
  }
}
