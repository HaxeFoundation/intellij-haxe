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
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.Pair;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.ChildRole;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
abstract public class AbstractHaxeNamedComponent extends HaxePsiCompositeElementImpl
  implements HaxeNamedComponent, PsiNamedElement {

  public ResultHolder _cachedType;
  public long _cachedTypeStamp;

  public AbstractHaxeNamedComponent(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  @NonNls
  public String getName() {
    final HaxeComponentName name = getComponentName();
    if (name != null) {
      return name.getText();
    }
    return super.getName();
  }

  @Override
  public String getText() {
    return super.getText();
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
  public Icon getIcon(int flags) {
    final HaxeComponentType type = HaxeComponentType.typeOf(this);
    return type == null ? null : type.getIcon();
  }

  @Override
  public ItemPresentation getPresentation() {
    return new ItemPresentation() {
      @Override
      public String getPresentableText() {
        final StringBuilder result = new StringBuilder();
        HaxeMemberModel member = HaxeMemberModel.fromPsi(AbstractHaxeNamedComponent.this);

        if (member == null) {
          result.append(AbstractHaxeNamedComponent.this.getName());
        }
        else {
          result.append(member.getName());

          if (member instanceof HaxeMethodModel) {
            final String parameterList = HaxePresentableUtil.getPresentableParameterList(member.getNamedComponentPsi());
            result.append("(").append(parameterList).append(")");
          }

          result.append(":");
          result.append(HaxeTypeResolver.getFieldOrMethodReturnType(AbstractHaxeNamedComponent.this).toString());
        }

        return result.toString();
      }

      @Override
      public String getLocationString() {
        HaxeClass haxeClass = AbstractHaxeNamedComponent.this instanceof HaxeClass
                              ? (HaxeClass)AbstractHaxeNamedComponent.this
                              : PsiTreeUtil.getParentOfType(AbstractHaxeNamedComponent.this, HaxeClass.class);
        if (haxeClass instanceof HaxeAnonymousType) {
          final HaxeTypedefDeclaration typedefDeclaration = PsiTreeUtil.getParentOfType(haxeClass, HaxeTypedefDeclaration.class);
          if (typedefDeclaration != null) {
            haxeClass = typedefDeclaration;
          }
        }
        if (haxeClass == null) {
          return "";
        }
        final Pair<String, String> qName = HaxeResolveUtil.splitQName(haxeClass.getQualifiedName());
        if (haxeClass == AbstractHaxeNamedComponent.this) {
          return qName.getFirst();
        }
        return haxeClass.getQualifiedName();
      }

      @Override
      public Icon getIcon(boolean open) {
        return AbstractHaxeNamedComponent.this.getIcon(0);
      }
    };
  }

  @Override
  public HaxeNamedComponent getTypeComponent() {
    final HaxeTypeTag typeTag = PsiTreeUtil.getChildOfType(getParent(), HaxeTypeTag.class);
    final HaxeType type = typeTag != null ? typeTag.getTypeOrAnonymous().getType() : null;
    final PsiReference reference = type != null ? type.getReference() : null;
    if (reference != null) {
      final PsiElement result = reference.resolve();
      if (result instanceof HaxeNamedComponent) {
        return (HaxeNamedComponent)result;
      }
    }
    return null;
  }

  @Override
  public boolean isPublic() {
    if (PsiTreeUtil.getParentOfType(this, HaxeExternClassDeclaration.class) != null) {
      return true;
    }
    if (PsiTreeUtil.getParentOfType(this, HaxeInterfaceDeclaration.class, HaxeEnumDeclaration.class) != null) {
      return true;
    }
    if (PsiTreeUtil.getParentOfType(this, HaxeAnonymousType.class) != null) {
      return true;
    }
    final PsiElement parent = getParent();
    return hasPublicAccessor(this) || (parent instanceof HaxePsiCompositeElement && hasPublicAccessor((HaxePsiCompositeElement)parent));
  }

  private static boolean hasPublicAccessor(HaxePsiCompositeElement element) {
    // do not change the order of these if-statements
    if (UsefulPsiTreeUtil.getChildOfType(element, HaxeTokenTypes.KPRIVATE) != null) {
      return false; // private
    }
    if (UsefulPsiTreeUtil.getChildOfType(element, HaxeTokenTypes.KPUBLIC) != null) {
      return true; // public
    }

    final HaxeDeclarationAttribute[] declarationAttributeList = PsiTreeUtil.getChildrenOfType(element, HaxeDeclarationAttribute.class);
    if (declarationAttributeList != null) {
      final Set<IElementType> declarationTypes = HaxeResolveUtil.getDeclarationTypes((declarationAttributeList));
      // do not change the order of these if-statements
      if (declarationTypes.contains(HaxeTokenTypes.KPRIVATE)) {
        return false; // private
      }
      if (declarationTypes.contains(HaxeTokenTypes.KPUBLIC)) {
        return true; // public
      }
    }

    return false; // <default>: private
  }

  @Override
  public boolean isStatic() {
    AbstractHaxeNamedComponent element = this;

    if (this instanceof HaxeVarDeclarationPart) {
      element = (AbstractHaxeNamedComponent)element.getParent();
    }

    final HaxeDeclarationAttribute[] declarationAttributeList = PsiTreeUtil.getChildrenOfType(element, HaxeDeclarationAttribute.class);
    return HaxeResolveUtil.getDeclarationTypes(declarationAttributeList).contains(HaxeTokenTypes.KSTATIC);
  }

  @Override
  public boolean isOverride() {
    final HaxeDeclarationAttribute[] declarationAttributeList = PsiTreeUtil.getChildrenOfType(this, HaxeDeclarationAttribute.class);
    return HaxeResolveUtil.getDeclarationTypes(declarationAttributeList).contains(HaxeTokenTypes.KOVERRIDE);
  }

  @Override
  public boolean isInline() {
    final HaxeDeclarationAttribute[] declarationAttributeList = PsiTreeUtil.getChildrenOfType(this, HaxeDeclarationAttribute.class);
    return HaxeResolveUtil.getDeclarationTypes(declarationAttributeList).contains(HaxeTokenTypes.KINLINE);
  }

  @Nullable
  public final PsiElement findChildByRoleAsPsiElement(int role) {
    ASTNode element = findChildByRole(role);
    if (element == null) return null;
    return SourceTreeToPsiMap.treeElementToPsi(element);
  }

  @Nullable
  public ASTNode findChildByRole(int role) {
    // assert ChildRole.isUnique(role);
    for (ASTNode child = getFirstChild().getNode(); child != null; child = child.getTreeNext()) {
      if (getChildRole(child) == role) return child;
    }
    return null;
  }

  public int getChildRole(ASTNode child) {
    if (child.getElementType() == HaxeTokenTypes.PLCURLY) {
      return ChildRole.LBRACE;
    }
    else if (child.getElementType() == HaxeTokenTypes.PRCURLY) {
      return ChildRole.RBRACE;
    }

    return 0;
  }

  protected final int getChildRole(ASTNode child, int roleCandidate) {
    if (findChildByRole(roleCandidate) == child) {
      return roleCandidate;
    }
    return 0; //ChildRole.NONE;
  }
}
