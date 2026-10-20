package com.intellij.plugins.haxe.ide.references;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.plugins.haxe.util.HaxeQnameResolveUtil;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * Pops the completion list when a {@code .} typed inside a string literal is
 * the SECOND-or-later dot of a qualified name whose prefix the project
 * knows — mirroring the confidence's rule so the scheduled popup is not
 * immediately vetoed. A first dot is everyday prose and stays silent; the
 * path counterpart lives in {@link HaxeStringPathTypedHandler}.
 */
public class HaxeStringQnameTypedHandler extends TypedHandlerDelegate {

  // a qualified name with at least one dot, ending at the caret with a
  // word segment ("com.package") - typing '.' after it makes dot number two
  private static final Pattern QNAME_ONE_DOT = Pattern.compile("[A-Za-z_]\\w*(?:\\.\\w+)+");

  @Override
  public @NotNull Result checkAutoPopup(char charTyped, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    if (charTyped != '.' || !(file instanceof HaxeFile)) return Result.CONTINUE;
    int offset = editor.getCaretModel().getOffset();
    HaxeStringLiteralExpression literal = HaxeStringLiterals.literalAtCaret(file, offset);
    if (literal == null) return Result.CONTINUE;

    String beforeCaret = HaxeStringLiterals.contentBeforeCaret(literal, file, offset);
    boolean secondDotOfKnownPrefix = beforeCaret != null
                                     && QNAME_ONE_DOT.matcher(beforeCaret).matches()
                                     && HaxeQnameResolveUtil.isKnownQnamePrefix(beforeCaret, project);
    if (!secondDotOfKnownPrefix) return Result.CONTINUE;
    AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
    return Result.STOP;
  }
}
