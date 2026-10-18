package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The v2 sync pipeline, shared by the tool window's sync button and the build file
 * auto-reload: flush editors, drop everything derived
 * ({@link HaxeBuildCaches#clearDerivedCaches}), then resync module libraries.
 * The tool window and the define context follow the sync's end
 * ({@link HaxeBuildEventListener#librariesSynced}).
 */
public final class HaxeProjectSync {

  private HaxeProjectSync() {
  }

  /** Runs the pipeline; {@code onFinished} runs on the EDT after everything (including library sync) completed. */
  public static void sync(@NotNull Project project, @Nullable Runnable onFinished) {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (project.isDisposed()) return;
      HaxeUnsavedDocuments.saveAll();
      HaxeBuildCaches.clearDerivedCaches(project, "haxe: project synced");
      HaxeLibrarySync.sync(project, onFinished);
    });
  }
}
