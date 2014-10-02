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
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.PsiSuperMethodImplUtil;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.MethodSignature;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


/**
 * @author: Srikanth.Ganapavarapu
 */
public abstract class HaxeMethodPsiMixinImpl extends AbstractHaxeNamedComponent implements HaxeMethodPsiMixin {

  private HaxeNamedComponent mHaxeNamedComponent;

  public HaxeMethodPsiMixinImpl(ASTNode node) {
    super(node);
  }


  @NotNull
  @Override
  public String getName() {
    String name = null;
    name = super.getName();
    if (null == name) {
      if (this.isConstructor()) {
        name = "new";
      } else {
        name = "unknown";
      }
    }
    return name;
  }


  @Nullable
  @Override
  public List<HaxeDeclarationAttribute> getDeclarationAttributeList() {
    // Not all function types have one of these...  If they do, the
    // subclass (via the generator) will override this method.
    return null;
  }


  @Nullable
  public HaxeReturnStatement getReturnStatement() {
    // Not all function types have one of these...  If they do, the
    // subclass (via the generator) will override this method.
    return findChildByClass(HaxeReturnStatement.class);
  }


  @Nullable
  public HaxeTypeTag getTypeTag() {
    // Not all function types have one of these...  If they do, the
    // subclass (via the generator) will override this method.
    return findChildByClass(HaxeTypeTag.class);
  }


  @Nullable
  @Override
  public PsiType getReturnType() {
    if (isConstructor()) {
      return null;
    }
    // A HaxeFunctionType is a PSI Element.
    HaxeFunctionType type = getTypeTag().getFunctionType(); // type could be null
    /* TODO: [TiVo]: translate above returned objects into PsiType */
    return null;
  }

  @Nullable
  @Override
  public PsiTypeElement getReturnTypeElement() {
    /* TODO: [TiVo]: translate below returned objects into PsiTypeElement */
    // return getTypeTag();
    return null;
  }


  @Nullable
  public HaxeThrowStatement getThrowStatement() {
    // Not all function types have one of these...  If they do, the
    // subclass (via the generator) will override this method.
    return null;
  }

  @NotNull
  @Override
  public PsiReferenceList getThrowsList() {
    HaxeThrowStatement ts = getThrowStatement();
    return new HaxePsiReferenceList(ts == null ? new HaxeDummyASTNode("ThrowsList")
                                                : ts.getNode());
  }

  @Nullable
  public HaxeBlockStatement getBlockStatement() {
    // Not all function types have one of these...  If they do, the
    // subclass (via the generator) will override this method.
    return null;
  }

  @Nullable
  @Override
  public PsiCodeBlock getBody() {
    HaxeBlockStatement bs = getBlockStatement();
    if (bs == null) {
      return null;
    }

    return (PsiCodeBlock)bs.getNode().findChildByType(HaxeTokenTypes.BLOCK_STATEMENT);
  }

  @Override
  public boolean isConstructor() {
    if (super.getName()==null &&
        getComponentName()==null &&
        getText().contains("function new(") &&
        !isStatic() &&
        !isOverride()) {
      return true;
    }
    return false;
  }

  @Nullable
  @Override
  public PsiDocComment getDocComment() {
    PsiComment psiComment = HaxeResolveUtil.findDocumentation(this);
    return ((psiComment != null)? new HaxePsiDocComment(this, psiComment) : null);
  }

  @Override
  public boolean isVarArgs() {
    // In Haxe, the method is set to VarArgs at runtime, via a function call.
    // We would need the ability to know if a particular run sequence has
    // called such a function.  I don't think we can pull that off without
    // the compiler's help.
    return false;
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Override
  public boolean hasTypeParameters() {
    return PsiImplUtil.hasTypeParameters(this);
  }

  @NotNull
  @Override
  public PsiTypeParameter[] getTypeParameters() {
    return PsiImplUtil.getTypeParameters(this);
  }

  @Nullable
  @Override
  public PsiClass getContainingClass() {
    return PsiTreeUtil.getParentOfType(this, HaxeClass.class, true);
  }

  @NotNull
  @Override
  public MethodSignature getSignature(@NotNull PsiSubstitutor substitutor) {
    /* TODO: [TiVo]: Implement */
    return null;
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

  @NotNull
  @Override
  public PsiMethod[] findSuperMethods() {
    return PsiSuperMethodImplUtil.findSuperMethods(this);
  }

  @NotNull
  @Override
  public PsiMethod[] findSuperMethods(boolean checkAccess) {
    return PsiSuperMethodImplUtil.findSuperMethods(this, checkAccess);
  }

  @NotNull
  @Override
  public PsiMethod[] findSuperMethods(PsiClass parentClass) {
    return PsiSuperMethodImplUtil.findSuperMethods(this, parentClass);
  }

  @NotNull
  @Override
  public List<MethodSignatureBackedByPsiMethod> findSuperMethodSignaturesIncludingStatic(boolean checkAccess) {
    return PsiSuperMethodImplUtil.findSuperMethodSignaturesIncludingStatic(this, checkAccess);
  }

  @Deprecated
  @Nullable
  @Override
  public PsiMethod findDeepestSuperMethod() {
    return PsiSuperMethodImplUtil.findDeepestSuperMethod(this);
  }

  @NotNull
  @Override
  public PsiMethod[] findDeepestSuperMethods() {
    return PsiSuperMethodImplUtil.findDeepestSuperMethods(this);
  }

  @Nullable
  @Override
  public PsiTypeParameterList getTypeParameterList() {
    /* TODO: [TiVo]: Implement */

    if (false) {

      //ASTNode node = getNode();
      //ASTNode paramList = node.findChildByType(HaxeTokenTypes.PARAMETER_LIST);
      //for (ASTNode astChild = paramList.getFirstChildNode(); astChild != null; astChild = astChild.getTreeNext()) {
      //  if (astChild.getElementType() == HaxeTokenTypes.PARAMETER) {
      //    Need to get the type from each parameter and drop it into our list.
      //
      //  }
      //  PsiElement psiChild = astChild.getPsi();
      //  if (psiChild)
      //}
      //PARAMETER
      //
      //return getRequiredStubOrPsiChild(JavaStubElementTypes.TYPE_PARAMETER_LIST);
    }

    return null;
  }

  @NotNull
  @Override
  public HaxeModifierList getModifierList() {

    if (false) {
      // Now that the modifier list is a proper part of the tree, we need only find it.
      // TODO: [TiVo] Use this stanza once PsiParameterList becomes part of the mixin.
      return PsiTreeUtil.findChildOfType(this, HaxeMacroClassList.class);
    }

    HaxeModifierList haxePsiModifierList = new HaxeModifierListImpl(this.getNode());

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

  @NotNull
  @Override
  public HierarchicalMethodSignature getHierarchicalMethodSignature() {
    return PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this);
  }

  @NotNull
  @Override
  public PsiParameterList getParameterList() {
    final HaxeParameterList list = PsiTreeUtil.getChildOfType(mHaxeNamedComponent, HaxeParameterList.class);
    return ((list != null) ? list : new HaxeParameterListImpl(new HaxeDummyASTNode("Dummy parameter list")));
  }
}
