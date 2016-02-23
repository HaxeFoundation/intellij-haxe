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

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * This class, ideally, must not derive from HaxeModifierListOwner because every element cannot be prefixed with modifiers/annotations.
 * E.g. try/catch blocks, Interface body, Class body etc. do not have annotations attached to them.
 *
 * Unfortunately, it is observed that individual words read from the file are being validated whether they are methods, fields or have
 * annotations attached to them. This is causing .findMethodByName("void"), .findFieldByName("var"), .hasModifierByName("catch") etc
 * calls to be made and resulting in runtime errors / class-cast exceptions.
 *
 * To work around that, this 'is-a' relationship is introduced :(
 */

public class HaxePsiCompositeElementImpl extends ASTWrapperPsiElement implements HaxePsiCompositeElement, HaxeModifierListOwner {
  public HaxePsiCompositeElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public IElementType getTokenType() {
    return getNode().getElementType();
  }

  public String getDebugName() {
    String name = getName();
    if (null != name) {
      return "'" + getName() + "'";
    }
    return "";
  }

  public String toString() {
    String out = getTokenType().toString();
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      // Unit tests don't want the extra data.
      out += " " + getDebugName();
    }
    return out;
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState state,
                                     PsiElement lastParent,
                                     @NotNull PsiElement place) {
    for (PsiElement element : getDeclarationElementToProcess(lastParent)) {
      if (!processor.execute(element, state)) {
        return false;
      }
    }
    return super.processDeclarations(processor, state, lastParent, place);
  }

  private List<PsiElement> getDeclarationElementToProcess(PsiElement lastParent) {
    final boolean isBlock = this instanceof HaxeBlockStatement || this instanceof HaxeSwitchCaseBlock;
    final PsiElement stopper = isBlock ? lastParent : null;
    final List<PsiElement> result = new ArrayList<PsiElement>();
    addVarDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeVarDeclaration.class));
    addLocalVarDeclarations(result, UsefulPsiTreeUtil.getChildrenOfType(this, HaxeLocalVarDeclaration.class, stopper));

    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeFunctionDeclarationWithAttributes.class));
    addDeclarations(result, UsefulPsiTreeUtil.getChildrenOfType(this, HaxeLocalFunctionDeclaration.class, stopper));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeClassDeclaration.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeExternClassDeclaration.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeEnumDeclaration.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeInterfaceDeclaration.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeTypedefDeclaration.class));

    final HaxeParameterList parameterList = PsiTreeUtil.getChildOfType(this, HaxeParameterList.class);
    if (parameterList != null) {
      result.addAll(parameterList.getParameterList());
    }
    final HaxeGenericParam tygenericParameParam = PsiTreeUtil.getChildOfType(this, HaxeGenericParam.class);
    if (tygenericParameParam != null) {
      result.addAll(tygenericParameParam.getGenericListPartList());
    }

    if (this instanceof HaxeForStatement && ((HaxeForStatement)this).getIterable() != lastParent) {
      result.add(this);
    }

    if (this instanceof HaxeCatchStatement) {
      final HaxeParameter catchParameter = PsiTreeUtil.getChildOfType(this, HaxeParameter.class);
      if(catchParameter != null) {
        result.add(catchParameter);
      }
    }
    return result;
  }

  private static void addLocalVarDeclarations(@NotNull List<PsiElement> result,
                                              @Nullable HaxeLocalVarDeclaration[] items) {
    if (items == null) {
      return;
    }
    for (HaxeLocalVarDeclaration varDeclaration : items) {
      result.addAll(varDeclaration.getLocalVarDeclarationPartList());
    }
  }

  private static void addVarDeclarations(@NotNull List<PsiElement> result, @Nullable HaxeVarDeclaration[] items) {
    if (items == null) {
      return;
    }
    for (HaxeVarDeclaration varDeclaration : items) {
      result.add(varDeclaration.getVarDeclarationPart());
    }
  }

  private static void addDeclarations(@NotNull List<PsiElement> result, @Nullable PsiElement[] items) {
    if (items != null) {
      result.addAll(Arrays.asList(items));
    }
  }


  // HaxeModifierListOwner implementations

  @Override
  public boolean hasModifierProperty(@PsiModifier.ModifierConstant @NonNls @NotNull String name) {
    HaxeModifierList list = getModifierList();
    return null == list ? false : list.hasModifierProperty(name);
  }

  @Nullable
  @Override
  public HaxeModifierList getModifierList() {
    HaxeModifierList list = (HaxeModifierList) this.findChildByType(HaxeTokenTypes.MACRO_CLASS_LIST);
    return list;
  }

}
