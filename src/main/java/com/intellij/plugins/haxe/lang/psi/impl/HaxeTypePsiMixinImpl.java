/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.plugins.haxe.lang.psi.*;

import com.intellij.psi.*;
import lombok.CustomLog;
import org.apache.log4j.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Created by ebishton on 10/9/14.
 */
@CustomLog
public class HaxeTypePsiMixinImpl extends HaxePsiCompositeElementImpl implements HaxeTypePsiMixin {

  {
    log.setLevel(LogLevel.DEBUG);
  }

  public HaxeTypePsiMixinImpl(ASTNode node) {
    super(node);
  }


  @Nullable
  @Override
  public PsiType getPsiType() {
    return (this instanceof HaxeType) ? new HaxePsiTypeAdapter((HaxeType)this) : null;
  }

  //@Override
  //public boolean hasTypeParameters() {
  //  return getTypeParameters().length != 0;
  //}
  //
  //@Nullable
  //@Override
  //public PsiTypeParameterList getTypeParameterList() {
  //  return (HaxeTypeParam) findChildByType(HaxeTokenTypes.TYPE_PARAM);
  //}
  //
  //@NotNull
  //@Override
  //public PsiTypeParameter[] getTypeParameters() {
  //  return PsiImplUtil.getTypeParameters(this);
  //}
  //
  //@Nullable
  //@Override
  //public PsiClass getContainingClass() {
  //  PsiElement parent = getParent();
  //  while (parent != null) {
  //    if (parent instanceof HaxeFile) {
  //      // If we get to the file node, we've gone too far.
  //      return null;
  //    }
  //    if (parent instanceof HaxeClass) {
  //      return (HaxeClass)parent;
  //    }
  //    parent = parent.getParent();
  //  }
  //  return null;
  //}
}
