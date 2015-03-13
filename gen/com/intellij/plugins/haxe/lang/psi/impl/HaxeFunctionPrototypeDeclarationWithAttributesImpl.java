/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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

// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeFunctionPrototypeDeclarationWithAttributesImpl extends HaxeMethodImpl implements HaxeFunctionPrototypeDeclarationWithAttributes {

  public HaxeFunctionPrototypeDeclarationWithAttributesImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitFunctionPrototypeDeclarationWithAttributes(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<HaxeAutoBuildMacro> getAutoBuildMacroList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeAutoBuildMacro.class);
  }

  @Override
  @NotNull
  public List<HaxeBuildMacro> getBuildMacroList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeBuildMacro.class);
  }

  @Override
  @NotNull
  public HaxeComponentName getComponentName() {
    return findNotNullChildByClass(HaxeComponentName.class);
  }

  @Override
  @NotNull
  public List<HaxeCustomMeta> getCustomMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeCustomMeta.class);
  }

  @Override
  @NotNull
  public List<HaxeDebugMeta> getDebugMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeDebugMeta.class);
  }

  @Override
  @NotNull
  public List<HaxeDeclarationAttribute> getDeclarationAttributeList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeDeclarationAttribute.class);
  }

  @Override
  @NotNull
  public List<HaxeFinalMeta> getFinalMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeFinalMeta.class);
  }

  @Override
  @Nullable
  public HaxeGenericParam getGenericParam() {
    return findChildByClass(HaxeGenericParam.class);
  }

  @Override
  @NotNull
  public List<HaxeGetterMeta> getGetterMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeGetterMeta.class);
  }

  @Override
  @NotNull
  public List<HaxeKeepMeta> getKeepMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeKeepMeta.class);
  }

  @Override
  @NotNull
  public List<HaxeMetaMeta> getMetaMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeMetaMeta.class);
  }

  @Override
  @NotNull
  public List<HaxeNoDebugMeta> getNoDebugMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeNoDebugMeta.class);
  }

  @Override
  @NotNull
  public List<HaxeNsMeta> getNsMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeNsMeta.class);
  }

  @Override
  @NotNull
  public List<HaxeOverloadMeta> getOverloadMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeOverloadMeta.class);
  }

  @Override
  @Nullable
  public HaxeParameterList getParameterList() {
    return findChildByClass(HaxeParameterList.class);
  }

  @Override
  @NotNull
  public List<HaxeProtectedMeta> getProtectedMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeProtectedMeta.class);
  }

  @Override
  @NotNull
  public List<HaxeRequireMeta> getRequireMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeRequireMeta.class);
  }

  @Override
  @NotNull
  public List<HaxeSetterMeta> getSetterMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeSetterMeta.class);
  }

  @Override
  @Nullable
  public HaxeTypeTag getTypeTag() {
    return findChildByClass(HaxeTypeTag.class);
  }

}
