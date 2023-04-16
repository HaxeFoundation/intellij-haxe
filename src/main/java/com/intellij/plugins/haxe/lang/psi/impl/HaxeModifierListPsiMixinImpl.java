/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2020 Eric Bishton
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
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeModifierListPsiMixin;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.impl.source.PsiModifierListImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

/**
 * @author: Srikanth.Ganapavarapu
 */
public class HaxeModifierListPsiMixinImpl extends PsiModifierListImpl implements HaxeModifierListPsiMixin {

  private final HashMap<String, String> mModifierStatusMap;

  public HaxeModifierListPsiMixinImpl(@NotNull ASTNode node) {
    super(node);
    mModifierStatusMap = new HashMap<String, String>();
  }

  @Override
  public boolean hasModifierProperty(@PsiModifier.ModifierConstant @NotNull @NonNls String name) {
    return mModifierStatusMap.containsKey(name);
  }

  @Override
  public boolean hasExplicitModifier(@PsiModifier.ModifierConstant @NotNull @NonNls String name) {
    return mModifierStatusMap.containsKey(name);
  }

  @Override
  public void setModifierProperty(@PsiModifier.ModifierConstant @NotNull @NonNls String name, boolean value)
    throws IncorrectOperationException {
    mModifierStatusMap.put(name, Boolean.toString(value));
  }

  @Override
  public void checkSetModifierProperty(@PsiModifier.ModifierConstant @NotNull @NonNls String name, boolean value)
    throws IncorrectOperationException {
    // XXX: implement when needed
  }

  @NotNull
  @Override
  public PsiAnnotation[] getAnnotations() {
    // XXX: implement when needed
    return new PsiAnnotation[0];
  }

  @NotNull
  @Override
  public PsiAnnotation[] getApplicableAnnotations() {
    // XXX: implement when needed
    return new PsiAnnotation[0];
  }

  @Nullable
  @Override
  public PsiAnnotation findAnnotation(@NotNull @NonNls String qualifiedName) {
    if (hasModifierProperty(qualifiedName)) {
      String value = mModifierStatusMap.get(qualifiedName);
      // XXX: implement when needed
      // translate String to PsiAnnotation
      // return the newly constructed PsiAnnotation object
    }
    return null;
  }

  @Nullable
  @Override
  public PsiAnnotation addAnnotation(@NotNull @NonNls String qualifiedName) {
    mModifierStatusMap.put(qualifiedName, Boolean.TRUE.toString());
    // XXX: implement when needed
    // translate String to PsiAnnotation
    // return the newly constructed PsiAnnotation object
    return null;
  }

  @Override
  public IElementType getTokenType() {
    return HaxeTokenTypes.CLASS_MODIFIER_LIST;
  }

  @NotNull
  @Override
  public HaxeMetadataList getMetadataList(@Nullable Class<? extends HaxeMeta> metadataType) {
    return HaxeMetadataUtils.getMetadataList(this, metadataType);
  }

  @Override
  public boolean hasMetadata(HaxeMetadataTypeName name, @Nullable Class<? extends HaxeMeta> metadataType) {
    return HaxeMetadataUtils.hasMeta(this, metadataType, name);
  }
}
