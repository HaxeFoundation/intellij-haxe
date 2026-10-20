package com.intellij.plugins.haxe.ide.references;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Pops the completion list when {@code /} is typed inside a string literal —
 * a completed path segment makes its children suggestible. Everything after
 * the popup is normal completion; the FQN counterpart lives in
 * {@link HaxeStringQnameTypedHandler}.
 */
public class HaxeStringPathTypedHandler extends TypedHandlerDelegate {

  @Override
  public @NotNull Result checkAutoPopup(char charTyped, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    if (charTyped != '/' || !(file instanceof HaxeFile)) return Result.CONTINUE;
    if (HaxeStringLiterals.literalAtCaret(file, editor.getCaretModel().getOffset()) == null) return Result.CONTINUE;
    AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
    return Result.STOP;
  }
}
