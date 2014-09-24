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
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.InheritanceImplUtil;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.PsiSuperMethodImplUtil;
import com.intellij.psi.impl.source.tree.ChildRole;
import com.intellij.psi.impl.source.tree.java.PsiTypeParameterListImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public abstract class AbstractHaxePsiClass extends AbstractHaxeNamedComponent implements HaxeClass {

  public AbstractHaxePsiClass(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public HaxeNamedComponent getTypeComponent() {
    return this;
  }

  @NotNull
  @Override
  public String getQualifiedName() {
    final String name = getName();
    if (getParent() == null) {
      return name == null ? "" : name;
    }
    final String fileName = FileUtil.getNameWithoutExtension(getContainingFile().getName());
    String packageName = HaxeResolveUtil.getPackageName(getContainingFile());
    if (notPublicClass(name, fileName)) {
      packageName = HaxeResolveUtil.joinQName(packageName, fileName);
    }
    return HaxeResolveUtil.joinQName(packageName, name);
  }

  private boolean notPublicClass(String name, String fileName) {
    if (this instanceof HaxeExternClassDeclaration) {
      return false;
    }
    return !fileName.equals(name) && HaxeResolveUtil.findComponentDeclaration(getContainingFile(), fileName) != null;
  }

  @Override
  public boolean isInterface() {
    return HaxeComponentType.typeOf(this) == HaxeComponentType.INTERFACE;
  }

  @NotNull
  @Override
  public List<HaxeType> getHaxeExtendsList() {
    return HaxeResolveUtil.findExtendsList(PsiTreeUtil.getChildOfType(this, HaxeInheritList.class));
  }

  @NotNull
  @Override
  public List<HaxeType> getHaxeImplementsList() {
    return HaxeResolveUtil.getImplementsList(PsiTreeUtil.getChildOfType(this, HaxeInheritList.class));
  }

  @NotNull
  @Override
  public List<HaxeComponentWithDeclarationList> getHaxeMethods() {
    final List<HaxeNamedComponent> alltypes = HaxeResolveUtil.findNamedSubComponents(this);
    final List<HaxeNamedComponent> methods = HaxeResolveUtil.filterNamedComponentsByType(alltypes, HaxeComponentType.METHOD);
    final List<HaxeComponentWithDeclarationList> result = new ArrayList<HaxeComponentWithDeclarationList>();
    for ( HaxeNamedComponent method : methods ) {
      // HaxeComponentWithDeclarationList is the superclass for all method
      // types in the PSI tree.
      result.add((HaxeComponentWithDeclarationList)method);
    }
    return result;
  }

  @NotNull
  @Override
  public List<HaxeNamedComponent> getHaxeFields() {
    final List<HaxeNamedComponent> result = HaxeResolveUtil.findNamedSubComponents(this);
    return HaxeResolveUtil.filterNamedComponentsByType(result, HaxeComponentType.FIELD);
  }

  @NotNull
  @Override
  public List<HaxeVarDeclaration> getVarDeclarations() {
    final List<HaxeVarDeclaration> result = HaxeResolveUtil.getClassVarDeclarations(this);
    return result;
  }

  @Nullable
  @Override
  public HaxeNamedComponent findHaxeFieldByName(@NotNull final String name) {
    return ContainerUtil.find(getHaxeFields(), new Condition<HaxeNamedComponent>() {
      @Override
      public boolean value(HaxeNamedComponent component) {
        return name.equals(component.getName());
      }
    });
  }

  @Override
  public HaxeNamedComponent findHaxeMethodByName(@NotNull final String name) {
    return ContainerUtil.find(getHaxeMethods(), new Condition<HaxeNamedComponent>() {
      @Override
      public boolean value(HaxeNamedComponent component) {
        return name.equals(component.getName());
      }
    });
  }

  @Override
  public boolean isGeneric() {
    return getGenericParam() != null;
  }

  @Override
  public boolean isEnum() {
    return (HaxeComponentType.typeOf(this) == HaxeComponentType.ENUM);
  }

  @Override
  public boolean isAnnotationType() {
    /* both: annotation & typedef in haxe are treated as typedef! */
    return (HaxeComponentType.typeOf(this) == HaxeComponentType.TYPEDEF);
  }

  @Override
  public boolean isDeprecated() {
    /* not applicable to Haxe language */
    return false;
  }

  @Override
  @NotNull
  public PsiClass[] getSupers() {
    // Extends and Implements in one list
    return PsiClassImplUtil.getSupers(this);
  }

  @Override
  public PsiClass getSuperClass() {
    return PsiClassImplUtil.getSuperClass(this);
  }

  @Override
  @NotNull
  public PsiClassType[] getSuperTypes() {
    return PsiClassImplUtil.getSuperTypes(this);
  }

  @Override
  public PsiElement getScope() {
    return getParent();
  }

  @Override
  public PsiClass getContainingClass() {
    PsiElement parent = getParent();
    return (parent instanceof PsiClass ? (PsiClass)parent : null);
  }

  @Override
  public PsiClass[] getInterfaces() {  // Extends and Implements in one list
    return PsiClassImplUtil.getInterfaces(this);
  }

  @Override
  @Nullable
  public PsiReferenceList getExtendsList() {
    List<HaxeType> haxeTypeList = getHaxeExtendsList();
    HaxePsiReferenceList psiReferenceList = new HaxePsiReferenceList(this.getNode());
    for (HaxeType haxeType : haxeTypeList) {
      // TODO: [TiVo]: using 'add' confuses IDE that: it is a refactoring action
      psiReferenceList.add(haxeType.getReferenceExpression());
    }
    return psiReferenceList;
  }

  @Override
  @NotNull
  public PsiClassType[] getExtendsListTypes() {
    return PsiClassImplUtil.getExtendsListTypes(this);
  }

  @Override
  @Nullable
  public PsiReferenceList getImplementsList() {
    List<HaxeType> haxeTypeList = getHaxeImplementsList();
    HaxePsiReferenceList psiReferenceList = new HaxePsiReferenceList(this.getNode());
    for (HaxeType haxeType : haxeTypeList) {
      // TODO: [TiVo]: using 'add' confuses IDE that: it is a refactoring action
      psiReferenceList.add(haxeType.getReferenceExpression());
    }
    return psiReferenceList;
  }

  @Override
  @NotNull
  public PsiClassType[] getImplementsListTypes() {
    return PsiClassImplUtil.getImplementsListTypes(this);
  }

  @Override
  public boolean isInheritor(@NotNull PsiClass baseClass, boolean checkDeep) {
    return InheritanceImplUtil.isInheritor(this, baseClass, checkDeep);
  }

  @Override
  public boolean isInheritorDeep(PsiClass baseClass, @Nullable PsiClass classToByPass) {
    return InheritanceImplUtil.isInheritorDeep(this, baseClass, classToByPass);
  }

  @Override
  @NotNull
  public PsiClassInitializer[] getInitializers() {
    // does this need to be implemented
    return PsiClassInitializer.EMPTY_ARRAY;
  }

  @Override
  @NotNull
  public PsiField[] getFields() {
    List<HaxeNamedComponent> haxeFields = getHaxeFields();
    int index = 0;
    HaxePsiField[] psiFields = new HaxePsiField[haxeFields.size()];
    for (HaxeNamedComponent element : haxeFields) {
      psiFields[index++] = new HaxePsiField(this, element);
    }
    return psiFields;
  }

  @Override
  @NotNull
  public PsiField[] getAllFields() {
    return PsiClassImplUtil.getAllFields(this);
  }

  @Override
  @Nullable
  public PsiField findFieldByName(@NonNls String name, boolean checkBases) {
    return PsiClassImplUtil.findFieldByName(this, name, checkBases);
  }

  @Override
  @NotNull
  public PsiMethod[] getMethods() {
    List<HaxeComponentWithDeclarationList> haxeMethods = getHaxeMethods();
    int index = 0;
    HaxePsiMethod[] psiMethods = new HaxePsiMethod[haxeMethods.size()];
    for (HaxeComponentWithDeclarationList element : haxeMethods) {
      psiMethods[index++] = new HaxePsiMethod(element);
    }
    return psiMethods;
  }

  @Override
  @NotNull
  public PsiMethod[] getAllMethods() {
    return PsiClassImplUtil.getAllMethods(this);
  }

  @Override
  @NotNull
  public PsiMethod[] getConstructors() {
    return PsiClassImplUtil.findMethodsByName(this, "new", false);
  }

  @Override
  @Nullable
  public PsiMethod findMethodBySignature(final PsiMethod psiMethod, final boolean checkBases) {
    return PsiClassImplUtil.findMethodBySignature(this, psiMethod, checkBases);
  }

  @Override
  @NotNull
  public PsiMethod[] findMethodsByName(@NonNls String name, boolean checkBases) {
    return PsiClassImplUtil.findMethodsByName(this, name, checkBases);
  }

  public PsiMethod[] findMethodsBySignature(PsiMethod patternMethod, boolean checkBases) {
    return PsiClassImplUtil.findMethodsBySignature(this, patternMethod, checkBases);
  }

  @Override
  @NotNull
  public List<Pair<PsiMethod, PsiSubstitutor>> getAllMethodsAndTheirSubstitutors() {
    return PsiClassImplUtil.getAllWithSubstitutorsByMap(this, PsiClassImplUtil.MemberType.METHOD);
  }

  @Override
  @NotNull
  public List<Pair<PsiMethod, PsiSubstitutor>> findMethodsAndTheirSubstitutorsByName(@NonNls String name, boolean checkBases) {
    return PsiClassImplUtil.findMethodsAndTheirSubstitutorsByName(this, name, checkBases);
  }

  public boolean hasTypeParameters() {
    return PsiImplUtil.hasTypeParameters(this);
  }

  @Override
  @Nullable
  public PsiTypeParameterList getTypeParameterList() {
    return new PsiTypeParameterListImpl(this.getNode());
  }

  @Override
  @NotNull
  public PsiTypeParameter[] getTypeParameters() {
    return PsiImplUtil.getTypeParameters(this);
  }

  @Override
  public PsiElement getLBrace() {
    return findChildByRoleAsPsiElement(ChildRole.LBRACE);
  }

  @Override
  public PsiElement getRBrace() {
    return findChildByRoleAsPsiElement(ChildRole.RBRACE);
  }

  @Override
  @Nullable
  public PsiModifierList getModifierList() {
    // TODO: [TiVo]: MUST implement
    return null;
  }

  @Override
  public boolean hasModifierProperty(@PsiModifier.ModifierConstant @NonNls @NotNull String name) {
    if (PsiModifier.PUBLIC.equals(name)) {
        return isPublic();
    }
    else if (PsiModifier.PRIVATE.equals(name)) {
      return (! isPublic());
    }
    else if (getModifierList() != null) {
      return getModifierList().hasModifierProperty(name);
    }
    return false;
  }

  @Override
  @Nullable
  public PsiDocComment getDocComment() {
    if (null != HaxeResolveUtil.findDocumentation(this)) {
      return new HaxePsiDocComment(this);
    }
    return null;
  }

  @Override
  @NotNull
  public PsiElement getNavigationElement() {
    return this;
  }

  @Override
  @Nullable
  public PsiIdentifier getNameIdentifier() {
    return ((PsiIdentifier) findChildByRoleAsPsiElement(ChildRole.NAME));
  }

  @Override
  @NotNull
  public Collection<HierarchicalMethodSignature> getVisibleSignatures() {
    return PsiSuperMethodImplUtil.getVisibleSignatures(this);
  }

  @NotNull
  public PsiClass[] getInnerClasses() {
    return PsiClass.EMPTY_ARRAY;
  }

  @NotNull
  public PsiClass[] getAllInnerClasses() {
    return PsiClass.EMPTY_ARRAY;
  }

  @Override
  public PsiClass findInnerClassByName(@NonNls String name, boolean checkBases) {
    return null;
  }
}
