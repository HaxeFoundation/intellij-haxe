package com.intellij.plugins.haxe.ide.surroundWith;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeDoWhileStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDoWhileSurrounder extends HaxeManyStatementsSurrounder {
  @NotNull
  @Override
  protected PsiElement doSurroundElements(PsiElement[] elements, PsiElement parent) {
    final HaxeDoWhileStatement whileStatement =
      (HaxeDoWhileStatement)HaxeElementGenerator.createStatementFromText(elements[0].getProject(), "do {\n} while(a);");
    addStatements(whileStatement.getBlockStatement(), elements);
    return whileStatement;
  }

  @Override
  protected TextRange getSurroundSelectionRange(PsiElement element) {
    final List<HaxeExpression> expressionList = ((HaxeDoWhileStatement)element).getExpressionList();
    return expressionList.get(expressionList.size() - 1).getTextRange();
  }

  @Override
  public String getTemplateDescription() {
    return HaxeBundle.message("haxe.surrounder.do.while");
  }
}
