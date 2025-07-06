package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;

public class HaxeCompletionUtil {
  public static void flushChanges(Project project, Document document) {
    PsiDocumentManager instance = PsiDocumentManager.getInstance(project);
    instance.doPostponedOperationsAndUnblockDocument(document);
    instance.commitDocument(document);
  }

  public static void reformatAndAdjustIndent(InsertionContext context, TextRange range) {
    Editor editor = context.getEditor();
    Project project = editor.getProject();
    PsiFile file = context.getFile();

    CodeStyleManager styleManager = CodeStyleManager.getInstance(project);
    styleManager.reformatRange(file, range.getStartOffset(), range.getEndOffset());
    styleManager.adjustLineIndent(file, editor.getCaretModel().getOffset());
  }
  public static void reformatAndAdjustIndent(PsiFile file, Editor editor, TextRange range) {
    Project project = file.getProject();

    CodeStyleManager styleManager = CodeStyleManager.getInstance(project);
    styleManager.reformatRange(file, range.getStartOffset(), range.getEndOffset());
    styleManager.adjustLineIndent(file, editor.getCaretModel().getOffset());
  }
}
