package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Creates the method an unresolved call names, its parameters derived from the call's arguments. */
public class HaxeIntroduceMethodIntention extends HaxeIntroduceMethodIntentionBase<HaxeCallExpression> {

  public HaxeIntroduceMethodIntention(@NotNull HaxeCallExpression callExpression, @NotNull HaxeClass targetClass) {
    super(callExpression, calledName(callExpression), targetClass);
  }

  private static String calledName(@NotNull HaxeCallExpression callExpression) {
    PsiElement[] children = callExpression.getExpression().getChildren();
    return children[children.length - 1].getText();
  }

  @Override
  protected String returnTypeText() {
    return callReturnTypeText(myPsiElementPointer.getElement());
  }

  @Override
  protected String parameterListText() {
    return parameterListFromArguments(callArguments(myPsiElementPointer.getElement()), false);
  }

  @Override
  protected List<ResultHolder> knownParameterTypes() {
    HaxeCallExpression call = myPsiElementPointer.getElement();
    return call == null ? List.of() : argumentTypes(callArguments(call));
  }
}
