package com.intellij.plugins.haxe.lang.psi.manipulators;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeRegularExpressionLiteral;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;

/**
 * Created by fedorkorotkov.
 */
public class HaxeRegularExpressionLiteralManipulator extends AbstractElementManipulator<HaxeRegularExpressionLiteral> {
  @Override
  public HaxeRegularExpressionLiteral handleContentChange(HaxeRegularExpressionLiteral element, TextRange range, String newContent)
    throws IncorrectOperationException {
    String oldText = element.getText();
    PsiFile file = element.getContainingFile();
    newContent = StringUtil.escapeSlashes(newContent);
    String newText = oldText.substring(0, range.getStartOffset()) + newContent + oldText.substring(range.getEndOffset());
    PsiElement fromText = HaxeElementGenerator.createExpressionFromText(file.getProject(), newText);
    if (fromText instanceof HaxeRegularExpressionLiteral) {
      return (HaxeRegularExpressionLiteral)element.replace(fromText);
    }
    return element;
  }
}
