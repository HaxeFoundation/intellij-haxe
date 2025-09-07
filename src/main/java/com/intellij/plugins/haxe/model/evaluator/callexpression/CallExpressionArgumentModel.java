package com.intellij.plugins.haxe.model.evaluator.callexpression;

import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.psi.PsiElement;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CallExpressionArgumentModel {
  PsiElement psiElement;
  SpecificTypeReference type;
  boolean canCache;


  public static CallExpressionArgumentModel create(PsiElement psiElement, SpecificTypeReference type, boolean canCache) {
    return new CallExpressionArgumentModel(psiElement, type, canCache);
  }


}
