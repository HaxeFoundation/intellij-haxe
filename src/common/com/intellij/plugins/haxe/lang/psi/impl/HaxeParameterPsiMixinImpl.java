/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 TiVo Inc.
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
import com.intellij.plugins.haxe.lang.psi.HaxeModifierList;
import com.intellij.plugins.haxe.lang.psi.HaxeParameter;
import com.intellij.plugins.haxe.lang.psi.HaxeParameterPsiMixin;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * @author: Srikanth.Ganapavarapu
 */
public abstract class HaxeParameterPsiMixinImpl extends AbstractHaxeNamedComponent implements HaxeParameterPsiMixin {

  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.lang.psi.impl.HaxeParameterBase");

  public HaxeParameterPsiMixinImpl(ASTNode node) {
    super(node);
  }

  public HaxeParameterPsiMixinImpl(PsiParameter parameter) {
    super(parameter.getNode());
  }

  public HaxeParameterPsiMixinImpl(HaxeParameter parameter) {
    super(parameter.getNode());
  }

  @Override
  @NotNull
  public PsiElement getDeclarationScope() {
    // Lifted, lock, stock, and barrel from PsiParameterImpl.java
    // which was for the Java language.
    // TODO:  Need to verify against the Haxe language spec.
    //              Are there other situations?
    final PsiElement parent = getParent();
    if (parent == null) return this;

    if (parent instanceof PsiParameterList) {
      return parent.getParent();
    }
    if (parent instanceof PsiForeachStatement) {
      return parent;
    }
    if (parent instanceof PsiCatchSection) {
      return parent;
    }

    PsiElement[] children = parent.getChildren();
    //noinspection ConstantConditions
    if (children != null) {
      ext:
      for (int i = 0; i < children.length; i++) {
        if (children[i].equals(this)) {
          for (int j = i + 1; j < children.length; j++) {
            if (children[j] instanceof PsiCodeBlock) return children[j];
          }
          break ext;
        }
      }
    }

    LOG.error("Code block not found among parameter' (" + this + ") parent' (" + parent + ") children: " + Arrays.asList(children));
    return null;
  }

  @Override
  public boolean isVarArgs() {
    // In Haxe (http://old.haxe.org/doc/cross/reflect), there are no
    // varargs parameters, but the function is made to accept variable
    // arguments.  So, at this level it's always false.
    return false;
  }

  @Nullable
  @Override
  public PsiTypeElement getTypeElement() {
    // Lifted, lock, stock, and barrel from PsiParameterImpl.java
    // which was for the Java language.
    // TODO: Broken.  Needs re-implementation.
    //       Need to verify against the Haxe language spec.
    //              Are there other situations?
    // XXX: This won't work.  The children are further down the tree, not at the child level.
    for (PsiElement child = getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof PsiTypeElement) {
        //noinspection unchecked
        return (PsiTypeElement)child;
      }
    }

    // PsiTypeElement t = (HaxeType) PsiTreeUtil.findChildOfType(this, HaxeType.class);

    return null;
  }

  @NotNull
  @Override
  public PsiType getType() {
    // The Haxe language variable type (int, float, etc.), not the psi token type.
    PsiTypeElement el = getTypeElement();
    PsiType type = null != el ? el.getType() : PsiType.VOID;
    return null != type ? type : PsiType.VOID;
  }

  @Nullable
  @Override
  public PsiExpression getInitializer() {
    // XXX: this may need to be implemented for refactoring functionality
    return null;
  }

  @Override
  public boolean hasInitializer() {
    // XXX: this may need to be implemented for refactoring functionality
    return false;
  }

  @Override
  public void normalizeDeclaration() throws IncorrectOperationException {
    // XXX: this may need to be implemented for refactoring functionality
  }

  @Nullable
  @Override
  public Object computeConstantValue() {
    // XXX: this may need to be implemented for refactoring functionality
    return null;
  }

  @Nullable
  @Override
  public PsiIdentifier getNameIdentifier() {
    // TODO:  Implement 'public PsiIdentifier getNameIdentifier()'
    return null;
  }

  @NotNull
  @Override
  public HaxeModifierList getModifierList() {
    HaxeModifierList haxePsiModifierList = new HaxeModifierListImpl(this.getNode());

    // Triplicated code! HaxeMethodPsiMixinImpl + HaxeParameterPsiMixinImpl + HaxePsiFieldImpl
    if (isStatic()) {
      haxePsiModifierList.setModifierProperty(HaxePsiModifier.STATIC, true);
    }

    if (isInline()) {
      haxePsiModifierList.setModifierProperty(HaxePsiModifier.INLINE, true);
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
