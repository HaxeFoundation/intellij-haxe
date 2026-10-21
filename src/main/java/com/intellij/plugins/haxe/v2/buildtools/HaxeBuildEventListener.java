package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildSettingsListener;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.EventListener;

/**
 * Something was observed or computed about the build: a build file changed
 * on disk, a library sync or a build-tool evaluation finished, a failure
 * was recorded, the module workspace was written. A user changing a setting
 * is not an event; that arrives through {@link HaxeBuildSettingsListener}.
 * Events are delivered on the publisher's thread, often a background one,
 * so a consumer that touches UI or PSI dispatches first.
 */
public interface HaxeBuildEventListener extends EventListener {
  @Topic.ProjectLevel
  Topic<HaxeBuildEventListener> TOPIC = new Topic<>(HaxeBuildEventListener.class, Topic.BroadcastDirection.NONE);

  /**
   * A build file, or a file one includes, was saved to disk. Autosave fires
   * this, so no consumer starts a process on it: the "Load changes" banner
   * is the user's trigger for that.
   */
  default void buildFileContentChanged(@NotNull String path) {
    anyBuildEvent();
  }

  /** A file with a build-file extension appeared in the project: created, copied, or moved or renamed to this path. */
  default void buildFileAdded(@NotNull String path) {
    anyBuildEvent();
  }

  /** A build file left the path: deleted, or moved or renamed away. */
  default void buildFileDeleted(@NotNull String path) {
    anyBuildEvent();
  }

  /** A library sync run finished, whether or not it changed anything. */
  default void librariesSynced() {
    anyBuildEvent();
  }

  /** A lime, openfl or nme tool run for the build file finished and its result is cached. */
  default void evaluationLanded(@NotNull String buildFilePath) {
    anyBuildEvent();
  }

  /** Module libraries or SDK entries were written to the project model. */
  default void workspaceApplied() {
    anyBuildEvent();
  }

  /**
   * A build-tool or compiler-context failure appeared, changed or cleared;
   * {@code key} is the build file or container it belongs to.
   */
  default void failureRecorded(@NotNull String key) {
    anyBuildEvent();
  }

  /** Sync or Purge dropped every tool evaluation and compiler-derived cache; derived views start over. */
  default void derivedCachesCleared() {
    anyBuildEvent();
  }

  /** Every method above lands here unless overridden: one place for a consumer that reacts to every event. */
  default void anyBuildEvent() {
  }

  /** A listener reacting the same way to every event. */
  @NotNull
  static HaxeBuildEventListener onAny(@NotNull Runnable reaction) {
    return new HaxeBuildEventListener() {
      @Override
      public void anyBuildEvent() {
        reaction.run();
      }
    };
  }

  @NotNull
  static HaxeBuildEventListener publisher(@NotNull Project project) {
    return project.getMessageBus().syncPublisher(TOPIC);
  }
}
