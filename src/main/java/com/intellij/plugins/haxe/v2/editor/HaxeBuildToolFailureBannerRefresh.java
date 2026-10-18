package com.intellij.plugins.haxe.v2.editor;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildEventListener;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;

/** Re-queries the editor banners when a build-tool failure appears, changes or clears. */
public final class HaxeBuildToolFailureBannerRefresh implements HaxeBuildEventListener {
  private final Project project;

  public HaxeBuildToolFailureBannerRefresh(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public void failureRecorded(@NotNull String key) {
    EditorNotifications.getInstance(project).updateAllNotifications();
  }
}
