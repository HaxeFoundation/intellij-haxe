/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 TiVo Inc.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
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
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.util.HaxeAbstractEnumUtil;

import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import lombok.CustomLog;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by srikanthg on 10/9/14.
 */
@CustomLog
public abstract class HaxePsiFieldImpl extends AbstractHaxeNamedComponent implements HaxePsiField {


  static {
    log.info("Loaded HaxePsiFieldImpl");
    log.setLevel(LogLevel.DEBUG);
  }

  public HaxePsiFieldImpl(ASTNode node) {
    super(node);
  }

  private HaxeMemberModel _model = null;
  @Override
  public HaxeMemberModel getModel() {
    if (_model == null) {
      if (this instanceof HaxeEnumValueDeclaration enumValueDeclaration) {
        _model = new HaxeEnumValueModel(enumValueDeclaration);
      }else if (HaxeAbstractEnumUtil.isAbstractEnum(getContainingClass()) && HaxeAbstractEnumUtil.couldBeAbstractEnumField(this)) {
          _model = new HaxeEnumValueModel((HaxeFieldDeclaration)this);
      }else{
        _model = new HaxeFieldModel(this);
      }
    }
    return _model;
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
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    final HaxeComponentName componentName = getComponentName();
    if (componentName != null) {
      componentName.setName(name);
    }
    return this;
  }

  @Override
  @Nullable
  public HaxeComponentName getComponentName() {
    final PsiIdentifier identifier = getNameIdentifier();
    return identifier != null ? new HaxeComponentNameImpl(getNode()) : null;
  }

  @Nullable
  @Override
  public PsiIdentifier getNameIdentifier() {
    final HaxeComponentName compName = PsiTreeUtil.getChildOfType(this, HaxeComponentName.class);
    return compName != null ? PsiTreeUtil.getChildOfType(compName, HaxeIdentifier.class) : null;
  }

  public boolean isOptional() {
    return this instanceof  HaxeOptionalFieldDeclaration;
  }

  @Nullable
  @Override
  public PsiDocComment getDocComment() {
    PsiComment psiComment = HaxeResolveUtil.findDocumentation(this);
    return (psiComment != null) ? new HaxePsiDocComment(this, psiComment) : null;
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
    PsiType psiType = null;
    final HaxeTypeTag tag = PsiTreeUtil.getChildOfType(this, HaxeTypeTag.class);
    if (tag != null) {
      final HaxeTypeOrAnonymous toa = tag.getTypeOrAnonymous();
      final HaxeType type = (toa != null) ? toa.getType() : null;
      psiType = (type != null) ? type.getPsiType() : null;
    }
    return psiType != null ? psiType : HaxePsiTypeAdapter.DYNAMIC;
  }

  @Override
  @Nullable
  public HaxeTypeTag getTypeTag() {
    return null;
  }
  @Override
  @Nullable
  public HaxeVarInit getVarInit() {
    return null;
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

  @NotNull
  @Override
  public SearchScope getUseScope() {
    final PsiElement localVar = UsefulPsiTreeUtil.getParentOfType(this, HaxeLocalVarDeclaration.class);
    if (localVar != null) {
      final PsiElement outerBlock = UsefulPsiTreeUtil.getParentOfType(localVar, HaxeBlockStatement.class);
      if (outerBlock != null) {
        return new LocalSearchScope(outerBlock);
      }
    }
    return super.getUseScope();
  }
}
