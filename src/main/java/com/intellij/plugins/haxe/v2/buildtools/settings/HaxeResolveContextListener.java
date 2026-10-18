package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.Nullable;

/**
 * Fired on the project bus when a file's resolve-context choice changes (a
 * pin, "Select automatically", a navigation carrying its context over). Only
 * the views showing the choice listen; build-configuration consumers do not
 * hear it, since a context choice changes no build setting.
 */
public interface HaxeResolveContextListener {
  Topic<HaxeResolveContextListener> TOPIC = Topic.create("haxe resolve context changed", HaxeResolveContextListener.class);

  void resolveContextChanged();

  /** Announces a choice; a store built without a project (state tests) announces nothing. */
  static void publish(@Nullable Project project) {
    if (project != null) {
      project.getMessageBus().syncPublisher(TOPIC).resolveContextChanged();
    }
  }
}
