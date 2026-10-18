package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildEventListener;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EventListener;

/**
 * A persisted build choice changed: a store wrote it, or a Haxe settings
 * page did ({@link #compilerSettingsChanged}). There is one method per kind
 * of choice. A consumer overrides the kinds it derives from, or
 * {@link #anySettingChanged} to react to all of them. Things observed or
 * computed about the build are not choices; they arrive through
 * {@link HaxeBuildEventListener}.
 */
public interface HaxeBuildSettingsListener extends EventListener {
  @Topic.ProjectLevel
  Topic<HaxeBuildSettingsListener> TOPIC = new Topic<>(HaxeBuildSettingsListener.class, Topic.BroadcastDirection.NONE);

  /** Publishes nowhere: what a store built without a project (state tests) publishes to. */
  HaxeBuildSettingsListener NONE = new HaxeBuildSettingsListener() {
  };

  /** The part of a container's environment that changed; {@code ALL} when the whole environment was dropped. */
  enum EnvironmentAspect { SDK, COMPILE_COMMAND, COMPILATION_SERVER, ALL }

  /** A store loaded its persisted state (project open): everything derived from it starts over. */
  default void stateLoaded() {
    anySettingChanged();
  }

  default void activeBuildFileChanged() {
    anySettingChanged();
  }

  /** A build file was registered, removed, hidden or moved. */
  default void buildFilesChanged() {
    anySettingChanged();
  }

  default void targetSelected(@NotNull String buildFilePath) {
    anySettingChanged();
  }

  default void sectionSelected(@NotNull String buildFilePath) {
    anySettingChanged();
  }

  default void environmentChanged(@NotNull String containerId, @NotNull EnvironmentAspect aspect) {
    anySettingChanged();
  }

  /** A build file's IDE overrides changed: its define entries or its custom target. */
  default void ideOverridesChanged(@NotNull String buildFilePath) {
    anySettingChanged();
  }

  default void testsMarkChanged(@NotNull String containerId) {
    anySettingChanged();
  }

  default void workDirectoryChanged(@NotNull String buildFilePath) {
    anySettingChanged();
  }

  /** The custom actions of a build file changed; only the views that show them derive from it. */
  default void customActionsChanged(@NotNull String buildFilePath) {
    anySettingChanged();
  }

  default void customToolsChanged(@NotNull String containerId) {
    anySettingChanged();
  }

  /** The Haxe settings or a language level changed; {@code haxe_ver} and the Language level rows follow them. */
  default void compilerSettingsChanged() {
    anySettingChanged();
  }

  /** Every method above lands here unless overridden: one place for a consumer that derives from every choice. */
  default void anySettingChanged() {
  }

  /** A listener reacting the same way to every choice. */
  @NotNull
  static HaxeBuildSettingsListener onAny(@NotNull Runnable reaction) {
    return new HaxeBuildSettingsListener() {
      @Override
      public void anySettingChanged() {
        reaction.run();
      }
    };
  }

  /** The project's publisher; {@link #NONE} without a project. */
  @NotNull
  static HaxeBuildSettingsListener publisher(@Nullable Project project) {
    return project == null ? NONE : project.getMessageBus().syncPublisher(TOPIC);
  }
}
