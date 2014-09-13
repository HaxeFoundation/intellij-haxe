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
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.PsiSuperMethodImplUtil;
import com.intellij.psi.impl.source.ClassInnerStuffCache;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import com.intellij.psi.impl.source.PsiReferenceListImpl;
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
    myInnersCache = new ClassInnerStuffCache(this);
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
  public List<HaxeNamedComponent> getHaxeMethods() {
    final List<HaxeNamedComponent> result = HaxeResolveUtil.findNamedSubComponents(this);
    return HaxeResolveUtil.filterNamedComponentsByType(result, HaxeComponentType.METHOD);
  }

  @NotNull
  @Override
  public List<HaxeNamedComponent> getHaxeFields() {
    final List<HaxeNamedComponent> result = HaxeResolveUtil.findNamedSubComponents(this);
    return HaxeResolveUtil.filterNamedComponentsByType(result, HaxeComponentType.FIELD);
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
  public HaxeNamedComponent findMethodByName(@NotNull final String name) {
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


  //
  //------------------------------- BEGIN -------------------------------

  // Methods added to comply with "implements PsiClass"

  /*
    TODO HAXE: CRITICAL
      SOME of these methods need to be correctly implemented in respective
      derived types e.g. hasModifierProperty

      {IDEA-CODE-BASE}/./java-psi-impl/src/com/intellij/psi/impl/source/PsiAnonymousClassImpl.java
      etc., files in that directory path has decent examples that will help.
  */

  protected ClassInnerStuffCache myInnersCache;

  @Override
  public boolean isAnnotationType() {
    boolean retVal = false;
    /* TODO HAXE: implement */
    return retVal;
  }

  @Override
  public boolean isEnum() {
    return (HaxeComponentType.typeOf(this) == HaxeComponentType.ENUM);
  }

  @Override
  @NotNull
  public PsiClassType[] getExtendsListTypes() {
    return PsiClassImplUtil.getExtendsListTypes(this);
  }

  @Override
  @NotNull
  public PsiClassType[] getImplementsListTypes() {
    return PsiClassImplUtil.getImplementsListTypes(this);
  }

  @Override
  public PsiClass[] getInterfaces() {  // Extends and Implements in one list
    return PsiClassImplUtil.getInterfaces(this);
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
  @NotNull
  public PsiClass[] getSupers() {  // Extends and Implements in one list
    return PsiClassImplUtil.getSupers(this);
  }

  @Override
  public boolean isInheritor(@NotNull PsiClass baseClass, boolean checkDeep) {
    boolean retVal = false;
    /* TODO HAXE: implement */
    return retVal;
  }

  @Override
  public boolean isInheritorDeep(PsiClass baseClass, @Nullable PsiClass classToByPass) {
    boolean retVal = false;
    /* TODO HAXE: implement */
    return retVal;
  }

  @Override
  @NotNull
  public Collection<HierarchicalMethodSignature> getVisibleSignatures() {
    return PsiSuperMethodImplUtil.getVisibleSignatures(this);
  }

  @Override
  @NotNull
  public PsiMethod[] getAllMethods() {
    return PsiClassImplUtil.getAllMethods(this);
  }

  @Override
  @NotNull
  public PsiMethod[] findMethodsByName(@NonNls String name, boolean checkBases) {
    PsiMethod[] retVal = {};
    /* TODO HAXE: implement */
    return retVal;
  }

  public PsiMethod[] findMethodsBySignature(PsiMethod patternMethod, boolean checkBases) {
    PsiMethod[] retVal = {};
    /* TODO HAXE: implement */
    return retVal;
  }

  @NotNull
  public PsiClass[] getInnerClasses() {
    PsiClass[] retVal = {};
    return retVal;
  }

  @NotNull
  public PsiClass[] getAllInnerClasses() {
    PsiClass[] retVal = {};
    return retVal;
  }

  @Override
  public PsiClass findInnerClassByName(@NonNls String name /* unused */, boolean checkBases /* unused */) {
    return null;
  }

  @Override
  @NotNull
  public PsiMethod[] getConstructors() {
    List<PsiMethod> result = null;
    for (PsiMethod method : getMethods()) {
      if (method.getName().equals("new")) {
        if (result == null) result = ContainerUtil.newSmartList();
        result.add(method);
      }
    }
    return result == null ? PsiMethod.EMPTY_ARRAY : result.toArray(new PsiMethod[result.size()]);
  }

  @Override
  @NotNull
  public PsiField[] getAllFields() {
    return PsiClassImplUtil.getAllFields(this);
  }

  @Override
  @NotNull
  public PsiClassInitializer[] getInitializers() {
    PsiClassInitializer[] retVal = {};
    /* TODO HAXE: implement */
    return retVal;
  }

  @Override
  @NotNull
  public List<Pair<PsiMethod, PsiSubstitutor>> getAllMethodsAndTheirSubstitutors() {
    return getAllMethodsAndTheirSubstitutors();
  }

  @Override
  @NotNull
  public List<Pair<PsiMethod, PsiSubstitutor>> findMethodsAndTheirSubstitutorsByName(@NonNls String name, boolean checkBases) {
    List<Pair<PsiMethod, PsiSubstitutor>> retVal = new ArrayList<Pair<PsiMethod, PsiSubstitutor>>();
    /* TODO HAXE: implement */
    return retVal;
  }

  @Override
  public PsiElement getLBrace() {
    PsiElement retVal = null;
    /* TODO HAXE: implement */
    return retVal;
  }

  @Override
  public PsiElement getRBrace() {
    PsiElement retVal = null;
    /* TODO HAXE: implement */
    return retVal;
  }

  @Override
  @Nullable
  public PsiIdentifier getNameIdentifier() {
    PsiIdentifier retVal = null;
    /* TODO HAXE: implement */
    return retVal;
  }

  @Override
  public PsiElement getScope() {
    PsiElement retVal = null;
    /* TODO HAXE: implement */
    return retVal;
  }

  @Override
  public PsiClass getContainingClass() {
    PsiElement parent = getParent();
    return parent instanceof PsiClass ? (PsiClass)parent : null;
  }

  @Override
  @Nullable
  public PsiReferenceList getExtendsList() {
    List<HaxeType> hxList = getHaxeExtendsList();
    final int size = hxList.size();
    PsiReference[] psiRefArray = new PsiReference[size];
    for (int idx=0; idx<size; idx++) {
      psiRefArray[idx] = hxList.get(idx).getReferenceExpression();
    }
    // TODO HAXE: fix translation
    // return new PsiReferenceListImpl(psiRefArray);
    return null;
  }

  @Override
  @Nullable
  public PsiReferenceList getImplementsList() {
    List<HaxeType> hxList = getHaxeImplementsList();
    final int size = hxList.size();
    PsiReference[] psiRefArray = new PsiReference[size];
    for (int idx=0; idx<size; idx++) {
      psiRefArray[idx] = hxList.get(idx).getReferenceExpression();
    }
    // TODO HAXE: fix translation
    // return new PsiReferenceListImpl(psiRefArray);
    return null;
  }

  @Override
  @NotNull
  public PsiField[] getFields() {
    return myInnersCache.getFields();
  }

  @Override
  @NotNull
  public PsiMethod[] getMethods() {
    return myInnersCache.getMethods();
  }

  @Override
  @Nullable
  public PsiField findFieldByName(@NonNls String s, boolean b /* unused */) {
    HaxeNamedComponent inVal = findHaxeFieldByName(s);
    PsiField[] retVal = {};
    /* TODO HAXE: translate HaxeNamedComponent into PsiField[0] */
    return retVal[0];
  }

  @Override
  @Nullable
  public PsiMethod findMethodBySignature(final PsiMethod psiMethod, final boolean b) {
    return PsiClassImplUtil.findMethodBySignature(this, psiMethod, b);
  }

  @Override
  @Nullable
  public PsiModifierList getModifierList() {
    //return getRequiredStubOrPsiChild(JavaStubElementTypes.MODIFIER_LIST);
    PsiModifierList retVal = null;
    /* TODO HAXE: implement */
    return retVal;
  }

  @Override
  public boolean hasModifierProperty(@PsiModifier.ModifierConstant @NonNls @NotNull String name) {
    /* TODO HAXE: implement */
    final PsiModifierList modlist = getModifierList();
    return modlist != null && modlist.hasModifierProperty(name);
  }

  @Override
  @Nullable
  public PsiDocComment getDocComment() {
    PsiDocComment retVal = null;
    /* TODO HAXE: implement */
    return retVal;
  }

  @Override
  public boolean isDeprecated() {
    /* TODO HAXE: implement */
    return false;
  }

  public boolean hasTypeParameters() {
    /* TODO HAXE: implement */
    return false;
  }

  @Override
  @Nullable
  public PsiTypeParameterList getTypeParameterList() {
    PsiTypeParameterList[] retVal = {};
    /* TODO HAXE: implement */
    return retVal[0];
  }

  @Override
  @NotNull
  public PsiTypeParameter[] getTypeParameters() {
    PsiTypeParameter[] retVal = {};
    /* TODO HAXE: implement */
    return retVal;
  }

  @Override
  @NotNull
  public PsiElement getNavigationElement() {
    return this;
  }

  @Override
  @NotNull
  public List<PsiField> getOwnFields() {
    List<PsiField> retVal = new ArrayList<PsiField>();
    /* TODO HAXE: implement */
    return retVal;
  }

  @Override
  @NotNull
  public List<PsiMethod> getOwnMethods() {
    List<PsiMethod> retVal = new ArrayList<PsiMethod>();
    /* TODO HAXE: implement */
    return retVal;
  }

  @Override
  @NotNull
  public List<PsiClass> getOwnInnerClasses() {
    return new ArrayList<PsiClass>();
  }

  //-------------------------------- END --------------------------------
  //

}
