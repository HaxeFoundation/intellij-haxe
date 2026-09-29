package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.daemon.impl.analysis.DefaultHighlightingSettingProvider;
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runs generated-code previews at the "Syntax" highlighting level. Annotators
 * still run: the color annotators paint the preview, and the semantic ones
 * skip preview files themselves (see {@code AnnotatorUtil.isInGeneratedPreview}).
 * Inspections and external annotators are skipped entirely, because on a
 * reconstruction they could only report noise.
 */
public class HaxeGeneratedPreviewHighlightingSetting extends DefaultHighlightingSettingProvider {

  @Override
  @Nullable
  public FileHighlightingSetting getDefaultSetting(@NotNull Project project, @NotNull VirtualFile file) {
    return file.getUserData(HaxeGeneratedCodePreview.PREVIEW_KEY) != null ? FileHighlightingSetting.SKIP_INSPECTION : null;
  }
}
