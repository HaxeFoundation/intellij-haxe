package com.intellij.plugins.haxe.v2.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeBuildToolFailures;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;

/**
 * Banner on an open build file whose last build-tool evaluation failed: the
 * failure the tool window shows on the file's row. {@link HaxeBuildToolFailureBannerRefresh}
 * re-queries it when a failure changes.
 */
public final class HaxeBuildToolFailureNotificationProvider implements EditorNotificationProvider {

  @Override
  @Nullable
  public Function<? super FileEditor, ? extends JComponent> collectNotificationData(@NotNull Project project,
                                                                                   @NotNull VirtualFile file) {
    String failure = HaxeBuildToolFailures.getInstance(project).lastFailure(file.getPath());
    if (failure == null) return null;
    return fileEditor -> new EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Error).text(failure);
  }
}
