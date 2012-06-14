package com.intellij.plugins.haxe.ide.surroundWith;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeCatchStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeParameter;
import com.intellij.plugins.haxe.lang.psi.HaxeTryStatement;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeTryCatchSurrounder extends HaxeManyStatementsSurrounder {
  @NotNull
  @Override
  protected PsiElement doSurroundElements(PsiElement[] elements, PsiElement parent) {
    final HaxeTryStatement tryStatement =
      (HaxeTryStatement)HaxeElementGenerator.createStatementFromText(elements[0].getProject(), "try {\n} catch(a) {\n}");
    addStatements(tryStatement.getBlockStatement(), elements);
    return tryStatement;
  }

  @Override
  protected TextRange getSurroundSelectionRange(PsiElement element) {
    final HaxeCatchStatement catchStatement = ((HaxeTryStatement)element).getCatchStatementList().iterator().next();
    final HaxeParameter parameter = catchStatement.getParameter();
    return parameter == null ? catchStatement.getTextRange() : parameter.getTextRange();
  }

  @Override
  public String getTemplateDescription() {
    return HaxeBundle.message("haxe.surrounder.try.catch");
  }
}
