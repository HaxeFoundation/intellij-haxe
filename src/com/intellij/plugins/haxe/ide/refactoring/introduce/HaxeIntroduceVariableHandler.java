package com.intellij.plugins.haxe.ide.refactoring.introduce;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeIntroduceVariableHandler extends HaxeIntroduceHandler {
  public HaxeIntroduceVariableHandler() {
    super(HaxeBundle.message("refactoring.introduce.variable.dialog.title"));
  }

  @Override
  protected PsiElement addDeclaration(@NotNull final PsiElement expression,
                                      @NotNull final PsiElement declaration,
                                      @NotNull HaxeIntroduceOperation operation) {
    return doIntroduceVariable(expression, declaration, operation.getOccurrences(), operation.isReplaceAll());
  }

  public static PsiElement doIntroduceVariable(PsiElement expression,
                                               PsiElement declaration,
                                               List<PsiElement> occurrences,
                                               boolean replaceAll) {
    PsiElement anchor = replaceAll ? findAnchor(occurrences) : findAnchor(expression);
    assert anchor != null;
    final PsiElement parent = anchor.getParent();
    return parent.addBefore(declaration, anchor);
  }
}
