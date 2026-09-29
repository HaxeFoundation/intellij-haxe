package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.plugins.haxe.v2.buildtools.info.HaxeLimeProjectInfoService;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeNmeProjectInfoService;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The v2 sync pipeline, shared by the tool window's sync button and the build file
 * auto-reload: flush editors, drop the lime/nme evaluation caches, resync module
 * libraries, then announce the change on {@link HaxeBuildConfigListener#TOPIC}
 * (which refreshes the tool window tree).
 */
public final class HaxeProjectSync {

  private HaxeProjectSync() {
  }

  /** Runs the pipeline; {@code onFinished} runs on the EDT after everything (including library sync) completed. */
  public static void sync(@NotNull Project project, @Nullable Runnable onFinished) {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (project.isDisposed()) return;
      HaxeUnsavedDocuments.saveAll();
      HaxeLimeProjectInfoService.getInstance(project).clearCache();
      HaxeNmeProjectInfoService.getInstance(project).clearCache();
      HaxeLibrarySync.sync(project, () -> {
        if (!project.isDisposed()) {
          project.getMessageBus().syncPublisher(HaxeBuildConfigListener.TOPIC).buildConfigurationChanged();
        }
        if (onFinished != null) {
          onFinished.run();
        }
      });
    });
  }
}
