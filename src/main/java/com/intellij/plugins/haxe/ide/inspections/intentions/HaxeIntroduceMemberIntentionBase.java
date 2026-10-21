package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Creates a member (a field or an enum value) for an unresolved reference.
 * The base finds the target class or module and its existing members; the
 * subclass picks where among them the new one goes.
 */
abstract class HaxeIntroduceMemberIntentionBase extends HaxeUnresolvedSymbolIntentionBase<HaxeReferenceExpression> {

  /** Where the declaration goes: before or after {@code element}. */
  protected record InsertInfo(PsiElement element, boolean isAfter) {
  }

  protected final @NotNull SmartPsiElementPointer<HaxeClass> myPsiTargetPointer;
  protected final String expressionText;
  private final String scopeName;

  protected HaxeIntroduceMemberIntentionBase(@NotNull HaxeReferenceExpression expression, @NotNull HaxeClass targetClass) {
    super(expression);
    PsiElement[] children = expression.getChildren();
    expressionText = children[children.length - 1].getText();
    myPsiTargetPointer = createPointer(targetClass);
    String className = targetClass.getName();
    scopeName = className != null ? className : expression.getContainingFile().getName();
  }

  /** The class or module the member lands in, as the intention text names it. */
  protected String scopeName() {
    return scopeName;
  }

  @Override
  protected String getPreviewName() {
    HaxeClass aClass = myPsiTargetPointer.getElement();
    return aClass == null ? null : aClass.getQualifiedName();
  }

  /** Where among the existing members (in order, never empty) the new one goes. */
  protected abstract InsertInfo insertPointAmong(@NotNull PsiElement expression, @NotNull List<? extends PsiElement> members);

  /** The insert point: among the target class's fields, else the module's, else the file's start. */
  protected @NotNull InsertInfo findInsertInfo(@NotNull HaxeReferenceExpression expression, boolean readOnly) {
    HaxeModule myModule = PsiTreeUtil.getStubOrPsiParentOfType(expression, HaxeModule.class);
    HaxeClass myClass = myPsiTargetPointer.getElement();
    if (myClass != null) {
      if (readOnly) myClass = copyFileAndReturnClonedPsiElement(myClass);
      return findInsertClass(expression, myClass);
    }
    else if (myModule != null) {
      List<? extends PsiElement> list = myModule.getModuleFieldDeclarationList();
      if (!list.isEmpty()) {
        return insertPointAmong(expression, list);
      }
      PsiElement sibling = myModule.getPrevSibling();
      if (sibling != null) return new InsertInfo(sibling, false);
    }
    return new InsertInfo(expression.getContainingFile().getFirstChild(), true);
  }

  private @NotNull InsertInfo findInsertClass(@NotNull PsiElement expression, @NotNull HaxeClass myClass) {
    List<? extends PsiElement> list = Arrays.stream(myClass.getFields()).toList();
    if (!list.isEmpty()) {
      return insertPointAmong(expression, list);
    }
    PsiElement brace = myClass.getLBrace();
    return brace != null ? new InsertInfo(brace, true) : new InsertInfo(myClass, false);
  }
}
