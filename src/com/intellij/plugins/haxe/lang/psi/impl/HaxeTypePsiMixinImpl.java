/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeParam;
import com.intellij.plugins.haxe.lang.psi.HaxeTypePsiMixin;
import com.intellij.psi.JavaResolveResult;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.util.IncorrectOperationException;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by ebishton on 10/9/14.
 */
public class HaxeTypePsiMixinImpl extends HaxePsiCompositeElementImpl implements HaxeTypePsiMixin {

  private static Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.lang.psi.impl.HaxeTypePsiMixin");
  {
    LOG.setLevel(Level.DEBUG);
  }

  public HaxeTypePsiMixinImpl(ASTNode node) {
    super(node);
  }


  @Nullable
  @Override
  public PsiElement getReferenceNameElement() {
    PsiElement child = findChildByType(HaxeTokenTypes.REFERENCE_EXPRESSION); // REFERENCE_NAME in Java
    return child;
  }

  @Nullable
  @Override
  public PsiReferenceParameterList getParameterList() {

    LOG.warn("getParameterList is unimplemented");

    // REFERENCE_PARAMETER_LIST  in Java
    HaxeTypeParam child = (HaxeTypeParam) findChildByType(HaxeTokenTypes.TYPE_PARAM);
    //return child == null ? null : child.getTypeList();
    return null;
  }

  @NotNull
  @Override
  public PsiType[] getTypeParameters() {
    LOG.warn("getTypeParameters is unimplemented");
    return new PsiType[0];
  }

  @Override
  public boolean isQualified() {
    LOG.warn("isQualified is unimplemented");
    return false;
  }

  @Override
  public String getQualifiedName() {
    LOG.warn("getQualifiedName is unimplemented");
    return null;
  }


  // PsiJavaReference overrides

  @Override
  public void processVariants(@NotNull PsiScopeProcessor processor) {
    LOG.warn("processVariants is unimplemented");
  }

  @NotNull
  @Override
  public JavaResolveResult advancedResolve(boolean incompleteCode) {
    LOG.warn("advancedResolve is unimplemented");
    return null;
  }

  @NotNull
  @Override
  public JavaResolveResult[] multiResolve(boolean incompleteCode) {
    LOG.warn("multiResolve is unimplemented");
    return new JavaResolveResult[0];
  }

  // PsiReference overrides


  @Override
  public PsiElement getElement() {
    LOG.warn("getElement is unimplemented");
    return null;
  }

  @Override
  public TextRange getRangeInElement() {
    LOG.warn("getRangeInElement is unimplemented");
    return null;
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    LOG.warn("resolve is unimplemented");
    return null;
  }

  @NotNull
  @Override
  public String getCanonicalText() {
    LOG.warn("getCanonicalText is unimplemented");
    return null;
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    LOG.warn("handleElementRename is unimplemented");
    return null;
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    LOG.warn("bindToElement is unimplemented");
    return null;
  }

  @Override
  public boolean isReferenceTo(PsiElement element) {
    LOG.warn("isReferenceTo is unimplemented");
    return false;
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    LOG.warn("getVariants is unimplemented");
    return new Object[0];
  }

  @Override
  public boolean isSoft() {
    LOG.warn("isSoft is unimplemented");
    return false;
  }

  // PsiQualifiedReference overrides


  @Nullable
  @Override
  public PsiElement getQualifier() {
    LOG.warn("getQualifier is unimplemented");
    return null;
  }

  @Nullable
  @Override
  public String getReferenceName() {
    LOG.warn("getReferenceName is unimplemented");
    return null;
  }

}
