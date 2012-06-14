package com.intellij.plugins.haxe.ide.surroundWith;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeIfStatement;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeIfSurrounder extends HaxeManyStatementsSurrounder {
  @NotNull
  @Override
  protected PsiElement doSurroundElements(PsiElement[] elements, PsiElement parent) {
    final HaxeIfStatement ifStatement =
      (HaxeIfStatement)HaxeElementGenerator.createStatementFromText(elements[0].getProject(), "if(a){\n}");
    addStatements(ifStatement.getBlockStatementList().iterator().next(), elements);
    return ifStatement;
  }

  @Override
  protected TextRange getSurroundSelectionRange(PsiElement element) {
    return ((HaxeIfStatement)element).getExpressionList().iterator().next().getTextRange();
  }

  @Override
  public String getTemplateDescription() {
    return HaxeBundle.message("haxe.surrounder.if");
  }
}
