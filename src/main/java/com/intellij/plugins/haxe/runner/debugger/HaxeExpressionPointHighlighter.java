package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Highlights the EXACT sub-expression the eval interpreter is about to run
 * (expression-stepping mode): the IDE's execution point is line-based, so the
 * expression span (line/column .. endLine/endColumn, 1-based from the VM) is
 * rendered as extra editor markup on top of it. One highlight exists at a
 * time; it is replaced on every stop and cleared on resume / session end.
 *
 * All markup mutation happens on the EDT; the show/clear entry points may be
 * called from any thread.
 */
public final class HaxeExpressionPointHighlighter {
  private final Project project;
  private final List<RangeHighlighter> active = new ArrayList<>();

  public HaxeExpressionPointHighlighter(Project project) {
    this.project = project;
  }

  /** Replaces the highlight with the given span; no-ops on bad coordinates. */
  public void show(@Nullable String sourcePath, int line, int column, @Nullable Integer endLine, @Nullable Integer endColumn) {
    ApplicationManager.getApplication().invokeLater(() -> {
      clearOnEdt();
      if (sourcePath == null || endColumn == null || line < 1 || column < 1) {
        return;
      }
      int spanEndLine = endLine != null ? endLine : line;
      for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
        if (editor.getProject() != null && editor.getProject() != project) {
          continue;
        }
        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (file == null || !pathsMatch(file.getPath(), sourcePath)) {
          continue;
        }
        Document document = editor.getDocument();
        if (line > document.getLineCount() || spanEndLine > document.getLineCount()) {
          continue;
        }
        int start = offsetOf(document, line, column);
        int end = offsetOf(document, spanEndLine, endColumn);
        if (end <= start) {
          continue;
        }
        active.add(editor.getMarkupModel().addRangeHighlighter(
          EditorColors.SEARCH_RESULT_ATTRIBUTES, start, end,
          HighlighterLayer.SELECTION - 1, HighlighterTargetArea.EXACT_RANGE));
      }
    });
  }

  /** Removes the highlight (resume, next stop, session end). */
  public void clear() {
    ApplicationManager.getApplication().invokeLater(this::clearOnEdt);
  }

  private void clearOnEdt() {
    for (RangeHighlighter highlighter : active) {
      if (highlighter.isValid()) {
        highlighter.dispose();
      }
    }
    active.clear();
  }

  // 1-based (line, column) -> document offset, clamped into the line
  private static int offsetOf(Document document, int line, int column) {
    int lineStart = document.getLineStartOffset(line - 1);
    int lineEnd = document.getLineEndOffset(line - 1);
    return Math.min(lineStart + Math.max(column - 1, 0), lineEnd);
  }

  private static boolean pathsMatch(String editorPath, String framePath) {
    return editorPath.replace('\\', '/').equalsIgnoreCase(framePath.replace('\\', '/'));
  }
}
