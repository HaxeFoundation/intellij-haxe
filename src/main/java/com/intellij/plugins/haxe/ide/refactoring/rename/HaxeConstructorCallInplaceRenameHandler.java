package com.intellij.plugins.haxe.ide.refactoring.rename;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.ide.refactoring.HaxeRefactoringSupportProvider;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodPsiMixin;
import com.intellij.plugins.haxe.lang.psi.HaxeNewExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.rename.inplace.InplaceRefactoring;
import com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * In-place rename started on a constructor call ({@code new Helper()})
 * renames the class. The reference there resolves to the constructor, whose
 * Haxe name is {@code new}, and the platform's member renamer would start
 * its template with that name, turning every occurrence into {@code new}.
 * {@link HaxeRefactoringSupportProvider} therefore declines in-place member
 * rename for constructors, and this handler renames the constructor's class
 * instead.
 */
public class HaxeConstructorCallInplaceRenameHandler extends MemberInplaceRenameHandler {

  @Override
  protected boolean isAvailable(@Nullable PsiElement element, @NotNull Editor editor, @NotNull PsiFile file) {
    PsiElement atCaret = file.findElementAt(editor.getCaretModel().getOffset());
    if (PsiTreeUtil.getParentOfType(atCaret, HaxeNewExpression.class) == null) return false;
    PsiClass constructedClass = classOfConstructor(element);
    return constructedClass != null && super.isAvailable(constructedClass, editor, file);
  }

  @Override
  public InplaceRefactoring doRename(@NotNull PsiElement elementToRename, @NotNull Editor editor, @Nullable DataContext dataContext) {
    PsiClass constructedClass = classOfConstructor(elementToRename);
    return super.doRename(constructedClass != null ? constructedClass : elementToRename, editor, dataContext);
  }

  @Nullable
  private static PsiClass classOfConstructor(@Nullable PsiElement element) {
    if (element instanceof HaxeMethodPsiMixin method && method.isConstructor()) return method.getContainingClass();
    return null;
  }
}
