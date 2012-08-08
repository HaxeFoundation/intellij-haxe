package com.intellij.plugins.haxe.ide.refactoring;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.plugins.haxe.ide.refactoring.introduce.HaxeIntroduceVariableHandler;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedElement;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringActionHandler;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeRefactoringSupportProvider extends RefactoringSupportProvider {
  @Override
  public boolean isInplaceRenameAvailable(PsiElement element, PsiElement context) {
    return element instanceof HaxeNamedElement;
  }

  @Override
  public RefactoringActionHandler getIntroduceVariableHandler() {
    return new HaxeIntroduceVariableHandler();
  }
}
