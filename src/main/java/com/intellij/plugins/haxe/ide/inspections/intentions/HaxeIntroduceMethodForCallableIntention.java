package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Creates the method an unresolved reference names where a callable type without a known signature is expected:
 * there are no arguments to derive parameters from, so the method has none and no declared return type.
 */
public class HaxeIntroduceMethodForCallableIntention extends HaxeIntroduceMethodIntentionBase<HaxeReferenceExpression> {

  public HaxeIntroduceMethodForCallableIntention(@NotNull HaxeReferenceExpression referenceExpression, @NotNull HaxeClass targetClass) {
    super(referenceExpression, referenceExpression.getText(), targetClass);
  }

  @Override
  protected String returnTypeText() {
    return SpecificHaxeClassReference.DYNAMIC;
  }

  @Override
  protected String parameterListText() {
    return "";
  }

  @Override
  protected List<ResultHolder> knownParameterTypes() {
    return List.of();
  }
}
