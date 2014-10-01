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

// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;

import com.intellij.psi.PsiModifierList;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeParameterImpl extends HaxeParameterPsiMixinImpl implements HaxeParameter {

  public HaxeParameterImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitParameter(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public HaxeComponentName getComponentName() {
    return findNotNullChildByClass(HaxeComponentName.class);
  }

  @Override
  @Nullable
  public HaxeTypeTag getTypeTag() {
    return findChildByClass(HaxeTypeTag.class);
  }

  @Override
  @Nullable
  public HaxeVarInit getVarInit() {
    return findChildByClass(HaxeVarInit.class);
  }

  @NotNull
  @Override
  public PsiModifierList getModifierList() {
    HaxePsiModifierList haxePsiModifierList = new HaxePsiModifierList(this);

    if (isStatic()) {
      haxePsiModifierList.setModifierProperty(HaxePsiModifier.STATIC, true);
    }

    if (isPublic()) {
      haxePsiModifierList.setModifierProperty(HaxePsiModifier.PUBLIC, true);
    }
    else {
      haxePsiModifierList.setModifierProperty(HaxePsiModifier.PRIVATE, true);
    }

    // XXX: make changes to bnf, and add code to detect any other missing annotations/modifiers
    // that can be applied to an identifier declaration... set appropriate elements as above.
    // E.g. see AbstractHaxeClassPsi

    return haxePsiModifierList;
  }

  @Override
  public boolean hasModifierProperty(@HaxePsiModifier.ModifierConstant @NonNls @NotNull String name) {
    return getModifierList().hasModifierProperty(name);
  }

}
