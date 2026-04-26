/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
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
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.helper.HaxeProcessDeclarationsHelper;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataListOwner;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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

public class HaxePsiCompositeElementImpl extends ASTWrapperPsiElement implements HaxePsiCompositeElement, HaxeModifierListOwner,
                                                                                 HaxeMetadataListOwner {

  public HaxePsiCompositeElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public IElementType getTokenType() {
    return getNode().getElementType();
  }

  public String getDebugName() {
    String name = null;
    String text = null;

      text = getText();
      name = getName();

    StringBuilder sb = new StringBuilder();
    if (null != name) {
      sb.append('\'');
      sb.append(name);
      sb.append('\'');
    }
    if (null != text) {
      if (null != name) {
        sb.append(' ');
      }
      sb.append('"');
      sb.append(text);
      sb.append('"');
    }
    return sb.toString();
  }

  public String toDebugString() {
    return getTokenType().toString() + getDebugName();
  }

  public String toString() {
    String out = getTokenType().toString();
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      out += " " + getDebugName();
    }
    return out;
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState state,
                                     PsiElement lastParent,
                                     @NotNull PsiElement place) {

    return HaxeProcessDeclarationsHelper.processDeclarations(this, processor, state,lastParent,place);
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
    return null;  // This list is built in sub-classes.
  }

  // HaxeMetadataListOwner implementations

  @Nullable
  @Override
  public HaxeMetadataList getMetadataList(@Nullable Class<? extends HaxeMeta> metadataType) {
    return HaxeMetadataUtils.getMetadataList(this, metadataType);
  }

  @Override
  public boolean hasMetadata(HaxeMetadataTypeName name, @Nullable Class<? extends HaxeMeta> metadataType) {
    return HaxeMetadataUtils.hasMeta(this, metadataType, name);
  }

  @Override
  public PsiElement @NotNull [] getChildren() {
    PsiElement psiChild = getFirstChild();
    if (psiChild == null) return PsiElement.EMPTY_ARRAY;

    List<PsiElement> result = null;
    while (psiChild != null) {
      // we want to include comments when listing children as its usefull in a lot of places
      // we could get children with comments by using custom code looping getNextSibling manually, but its more convenient
      // to just override this method and solve the need everywhere, and if we dont want Comments we can always filter the results later.
      // (including Comments here will for instance allow us to use Comments in Pattern matching for autocompletion)
      if (psiChild.getNode() instanceof CompositeElement || psiChild.getNode() instanceof PsiComment) {
        if (result == null) {
          result = new ArrayList<>();
        }
        result.add(psiChild);
      }
      psiChild = psiChild.getNextSibling();
    }
    return result == null ? PsiElement.EMPTY_ARRAY : PsiUtilCore.toPsiElementArray(result);
  }
}
