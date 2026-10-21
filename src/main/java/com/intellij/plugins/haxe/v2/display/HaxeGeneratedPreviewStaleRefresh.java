package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Re-queries the stale banner of every open generated-code preview when a
 * Haxe source changes on disk. The banner is collected later, after
 * {@link HaxeSavedSourcesStamp} has counted the same change.
 */
public final class HaxeGeneratedPreviewStaleRefresh implements BulkFileListener {
  private final Project project;

  public HaxeGeneratedPreviewStaleRefresh(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public void after(@NotNull List<? extends VFileEvent> events) {
    if (!ContainerUtil.exists(events, HaxeSavedSourcesStamp::isHaxeSourceChange)) return;
    EditorNotifications notifications = EditorNotifications.getInstance(project);
    for (VirtualFile file : FileEditorManager.getInstance(project).getOpenFiles()) {
      if (HaxeGeneratedCodePreview.isPreview(file)) {
        notifications.updateNotifications(file);
      }
    }
  }
}
