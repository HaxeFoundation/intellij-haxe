package com.intellij.plugins.haxe.execution.console;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/** The link a console filter answers with: a source position, and the span of one console line that opens it. */
final class HaxeConsoleLinks {

  private HaxeConsoleLinks() {
  }

  /** Opens {@code file} at the 0-based {@code documentLine} and {@code documentColumn}. */
  @NotNull
  static HyperlinkInfo target(@NotNull Project project, @NotNull VirtualFile file, int documentLine, int documentColumn) {
    return new OpenFileHyperlinkInfo(project, file, documentLine, documentColumn);
  }

  /**
   * Links {@code [spanStart, spanEnd)} of the line {@code text} to
   * {@code target}. Result offsets are document-absolute; the line starts
   * {@code entireLength} minus its own length back.
   */
  @NotNull
  static Filter.Result lineSpan(@NotNull String text, int entireLength, int spanStart, int spanEnd, @NotNull HyperlinkInfo target) {
    int lineStart = entireLength - text.length();
    return new Filter.Result(lineStart + spanStart, lineStart + spanEnd, target);
  }
}
