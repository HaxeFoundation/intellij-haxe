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
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by srikanthg on 10/9/14.
 */
public abstract class HaxePsiFieldImpl extends AbstractHaxeNamedComponent implements HaxePsiField {

  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.lang.psi.impl.HaxePsiFieldImpl");
  static {
    LOG.info("Loaded HaxePsiFieldImpl");
    LOG.setLevel(Level.DEBUG);
  }

  public HaxePsiFieldImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  @NonNls
  public String getName() {
    String name = super.getName();
    if (null == name) {
      final PsiIdentifier nameIdentifier = getNameIdentifier();
      if (nameIdentifier != null) {
        name = nameIdentifier.getText();
      }
    }

    return (name != null) ? name : "<unnamed>";
  }

  @Override
  @NotNull
  public HaxeComponentName getComponentName() {
    return new HaxeComponentNameImpl(getNode());
    ////this.getName();
    //return findNotNullChildByClass(HaxeComponentName.class);
  }

  @Nullable
  @Override
  public PsiIdentifier getNameIdentifier() {
    PsiIdentifier foundName = null;
    ASTNode node = getNode();
    if (null != node) {
      ASTNode element = node.findChildByType(HaxeTokenTypes.IDENTIFIER);
      if (null != element) {
        foundName = (PsiIdentifier) element.getPsi();
      }
    }
    return foundName;
  }

  @Nullable
  @Override
  public PsiDocComment getDocComment() {
    // TODO:  Implement 'public PsiDocComment getDocComment()'
    //PsiComment psiComment = HaxeResolveUtil.findDocumentation(this);
    //return ((psiComment != null)? new HaxePsiDocComment(getDelegate(), psiComment) : null);
    return null;
  }

  private boolean isPrivate() {
    // TODO:  Implement 'private boolean isPrivate()'
    //final List<HaxeDeclarationAttribute> declarationAttributeList = getDeclarationAttributeList();
    //for (HaxeDeclarationAttribute declarationAttribute : declarationAttributeList) {
    //  HaxeAccess access = declarationAttribute.getAccess();
    //  if (access!=null && "private".equals(access.getText())) {
    //    return true;
    //  }
    //}
    return false;
  }

  @Override
  public boolean isPublic() {
    return (!isPrivate() && super.isPublic()); // do not change the order of- and the- expressions
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Override
  public void setInitializer(@Nullable PsiExpression initializer) throws IncorrectOperationException {
    // XXX: this may need to be implemented for refactoring functionality
  }

  @Nullable
  @Override
  public PsiClass getContainingClass() {
    return PsiTreeUtil.getParentOfType(this, HaxeClass.class, true);
  }

  @NotNull
  @Override
  public PsiType getType() {
    PsiType                     psiType   = null;
    final HaxeTypeTag           tag       = PsiTreeUtil.getChildOfType(this, HaxeTypeTag.class);
    if (tag != null) {
      final HaxeTypeOrAnonymous toa       = tag.getTypeOrAnonymous();
      final HaxeType            type      = (toa != null) ? toa.getType() : null;
      psiType                             = (type != null) ? type.getPsiType() : null;
    }
    return psiType != null ? psiType : HaxePsiTypeAdapter.DYNAMIC;
  }

  @Nullable
  @Override
  public PsiTypeElement getTypeElement() {
    // Lifted, lock, stock, and barrel from PsiParameterImpl.java
    // which was for the Java language.
    // TODO:  Need to verify against the Haxe language spec.
    //              Are there other situations?
    for (PsiElement child = getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof PsiTypeElement) {
        //noinspection unchecked
        return (PsiTypeElement)child;
      }
    }
    return null;
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
    // intentionally left empty
  }

  @Nullable
  @Override
  public Object computeConstantValue() {
    return null;
  }

  @NotNull
  @Override
  public HaxeModifierList getModifierList() {

    HaxeModifierList list = super.getModifierList();

    if (null == list) {
      list = new HaxeModifierListImpl(this.getNode());
    }

    // -- below modifiers need to be set individually
    //    because, they cannot be enforced through macro-list

    if (isStatic()) {
      list.setModifierProperty(HaxePsiModifier.STATIC, true);
    }

    if (isInline()) {
      list.setModifierProperty(HaxePsiModifier.INLINE, true);
    }

    if (isPublic()) {
      list.setModifierProperty(HaxePsiModifier.PUBLIC, true);
    } else {
      list.setModifierProperty(HaxePsiModifier.PRIVATE, true);
    }

    return list;
  }

  @Override
  public boolean hasModifierProperty(@HaxePsiModifier.ModifierConstant @NonNls @NotNull String name) {
    return this.getModifierList().hasModifierProperty(name);
  }

}
