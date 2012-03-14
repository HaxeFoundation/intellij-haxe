package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDebuggerEditorsProvider extends XDebuggerEditorsProvider {
  @NotNull
  public FileType getFileType() {
    return HaxeFileType.HAXE_FILE_TYPE;
  }

  @NotNull
  public Document createDocument(@NotNull final Project project,
                                 @NotNull final String text, @Nullable final XSourcePosition position, @NotNull EvaluationMode mode) {
    return HaxeDebuggerSupportUtils.createDocument(
      text,
      project,
      position != null ? position.getFile() : null, position != null ? position.getOffset() : -1
    );
  }
}
