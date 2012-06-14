package com.intellij.plugins.haxe.ide.surroundWith;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeWhileStatement;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeWhileSurrounder extends HaxeManyStatementsSurrounder {
  @NotNull
  @Override
  protected PsiElement doSurroundElements(PsiElement[] elements, PsiElement parent) {
    final HaxeWhileStatement whileStatement =
      (HaxeWhileStatement)HaxeElementGenerator.createStatementFromText(elements[0].getProject(), "while(a) {\n}");
    addStatements(whileStatement.getBlockStatement(), elements);
    return whileStatement;
  }

  @Override
  protected TextRange getSurroundSelectionRange(PsiElement element) {
    return ((HaxeWhileStatement)element).getExpressionList().iterator().next().getTextRange();
  }

  @Override
  public String getTemplateDescription() {
    return HaxeBundle.message("haxe.surrounder.while");
  }
}
