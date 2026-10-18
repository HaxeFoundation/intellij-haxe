package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.function.Function;

/**
 * Banner on a generated-code preview whose dump is older than the Haxe
 * sources on disk, with a link that builds a fresh dump and opens it in the
 * preview's place. {@link HaxeGeneratedPreviewStaleRefresh} re-queries it
 * when a source changes.
 */
public final class HaxeGeneratedPreviewStaleNotificationProvider implements EditorNotificationProvider {

  @Override
  @Nullable
  public Function<? super FileEditor, ? extends JComponent> collectNotificationData(@NotNull Project project,
                                                                                   @NotNull VirtualFile file) {
    if (!HaxeGeneratedCodePreview.isStale(project, file)) return null;
    return fileEditor -> stalePanel(project, file, fileEditor);
  }

  @NotNull
  private static EditorNotificationPanel stalePanel(@NotNull Project project,
                                                    @NotNull VirtualFile file,
                                                    @NotNull FileEditor fileEditor) {
    EditorNotificationPanel panel = new EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Warning)
      .text(HaxeBundle.message("haxe.generated.preview.stale"));
    String regenerate = HaxeBundle.message("haxe.generated.preview.regenerate");
    panel.createActionLabel(regenerate, () -> HaxeGeneratedPreviewTarget.regenerate(project, file, fileEditor));
    return panel;
  }
}
