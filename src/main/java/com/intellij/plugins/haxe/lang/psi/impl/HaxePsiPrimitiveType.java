package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeVisitor;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxePsiPrimitiveType extends PsiType {
  private final String myName;

  public HaxePsiPrimitiveType(@NotNull String name, PsiAnnotation @NotNull [] annotations) {
    super(annotations);
    myName = name;
  }

  @Override
  public @NotNull @NlsSafe String getPresentableText() {
    return getText(false, false);
  }


  @NotNull
  @Override
  public String getCanonicalText(boolean annotated) {
    return getText(true, annotated);
  }

  @Override
  public @NotNull @NlsSafe String getCanonicalText() {
    return null;
  }

  @NotNull
  @Override
  public String getInternalCanonicalText() {
    return getCanonicalText(true);
  }

  private String getText(boolean qualified, boolean annotated) {
    PsiAnnotation[] annotations = annotated ? getAnnotations() : PsiAnnotation.EMPTY_ARRAY;
    if (annotations.length == 0) return myName;

    StringBuilder sb = new StringBuilder();
    PsiNameHelper.appendAnnotations(sb, annotations, qualified);
    sb.append(myName);
    return sb.toString();
  }

  @Override
  public boolean isValid() {
    for (PsiAnnotation annotation : getAnnotations()) {
      if (!annotation.isValid()) return false;
    }
    return true;
  }

  @Override
  public boolean equalsToText(@NotNull String text) {
    return myName.equals(text);
  }

  @Override
  public <A> A accept(@NotNull PsiTypeVisitor<A> visitor) {
    return visitor.visitType(this);
  }


  @Override
  public @Nullable GlobalSearchScope getResolveScope() {
    return null;
  }

  @Override
  public PsiType @NotNull [] getSuperTypes() {
    return EMPTY_ARRAY;
  }
}
