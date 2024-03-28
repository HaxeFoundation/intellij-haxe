/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
 * Copyright 2019-2020 Eric Bishton
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
package com.intellij.plugins.haxe.model.type;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeClassWrapperForTypeParameter;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeClassReference {
  public final String name;
  @NotNull
  public final PsiElement elementContext;
  public final HaxeClassModel classModel;
  private final boolean isTypeParameter;

  public HaxeClass clazz;

  private static final Key<Pair<Integer, String>> CLASS_NAME_KEY = new Key<>("HAXE_CLASS_NAME");

  public HaxeClassReference(@NotNull HaxeClassModel classModel, @NotNull PsiElement elementContext) {
    this.name = getClassName(classModel);
    this.elementContext = elementContext;
    this.classModel = classModel;
    this.isTypeParameter =  elementContext instanceof HaxeClassWrapperForTypeParameter;
  }

  public HaxeClassReference(String name, @NotNull PsiElement elementContext) {
    this.name = name;
    this.elementContext = elementContext;
    this.classModel = null;
    this.isTypeParameter =  elementContext instanceof HaxeClassWrapperForTypeParameter;
  }
  public HaxeClassReference(String name, @NotNull PsiElement elementContext, boolean isTypeParameter) {
    this.name = name;
    this.elementContext = elementContext;
    this.classModel = null;
    this.isTypeParameter = isTypeParameter;
  }

  private String getClassName(HaxeClassModel clazz) {
    return CachedValuesManager.getProjectPsiDependentCache(clazz.getPsi(), HaxeClassReference::getNameCached).getValue();
  }

  private static CachedValueProvider.Result<String> getNameCached(HaxeClass aClass) {
    String name = getClassNameInternal(aClass.getModel());
    return  CachedValueProvider.Result.create(name, aClass);
  }

  private static String getClassNameInternal(HaxeClassModel clazz) {
    if (clazz.haxeClass instanceof HaxeAnonymousType) {
      HaxeNamedComponent namedComponent = HaxeResolveUtil.findTypeParameterContributor(clazz.getBasePsi());
      if (namedComponent instanceof HaxeTypedefDeclaration typedefDeclaration) {
        // make sure we only replace the text with typedef name if its an exact match ( we dont want to replace anonymous structures in typeParameters etc)
        if (clazz.haxeClass.textMatches(typedefDeclaration.getTypeOrAnonymous())) {
          final HaxeComponentName name = namedComponent.getComponentName();
          if (name != null) {
            return name.getText();
          }
        }
      }
      final String formattedClassText = clazz.haxeClass.getText()
        .replaceAll("\\s{2,}+", " ")
        .replaceAll("\\{\\s", "{");

      return StringUtil.shortenTextWithEllipsis(formattedClassText,128,1);
    }
    return clazz.getName();
  }

  public HaxeClass getHaxeClass() {
    if (this.classModel != null) return classModel.getPsi();
    if (this.clazz != null) return clazz;
    // no real HaxeClass exists for type parameters (only HaxeClassWrapperForTypeParameter  synthetically created)
    if (this.isTypeParameter) return null;
    clazz = HaxeResolveUtil.findClassByQName(name, elementContext);
    if (clazz == null) {
      clazz = HaxeResolveUtil.tryResolveClassByQName(elementContext);
      // Null is a legitimate answer in the case of Dynamic or Unknown.
      // (Plus, this should be logging, not dumping to stderr.)
      //if (clazz == null) {
      //  System.err.println("Not found '" + name + "' : " + elementContext + " : " + elementContext.getText());
      //}
    }
    return clazz;
  }

  public boolean isTypeParameter() {
    return isTypeParameter;
  }

  @Nullable
  public String getName() {
    return this.name;
  }

  @Override
  public boolean equals(Object obj) {
    // @TODO: This should check fqNames
    if (obj instanceof HaxeClassReference) return getName().equals(((HaxeClassReference)obj).getName());
    return false;
  }

  /**
   * Checks whether two references refer to the same underlying haxe class.

   * NOTE: May cause resolution of one or both references to occur.
   *
   * @param other - other HaxeClassReference to compare with.
   * @return true if this reference refers to the same class as other; false otherwise.
   */
  public boolean refersToSameClass(@Nullable HaxeClassReference other) {
    if (null == other) return false;

    HaxeClass myClass = getHaxeClass();

    HaxeClass otherClass = other.getHaxeClass();

    if (null == myClass && null != otherClass ) return false;
    if (null == otherClass && null != myClass) return false;

    if (myClass == null && otherClass == null) {
      // unknown models, best guess generic types, compare text as a best effort
      // this kan happen when for instance a class A implements interface B and the method signature in both are generics
      return other.getName().equals(this.getName());
    }
    // resolve typedefs for correct comparison
    if (myClass.getModel().isTypedef()) {
      HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(myClass.getContext());
      SpecificHaxeClassReference reference = myClass.getModel().getUnderlyingClassReference(resolver);
      if (reference != null) myClass = reference.getHaxeClass();
    }
    if (otherClass.getModel().isTypedef()) {
      HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(otherClass.getContext());
      SpecificHaxeClassReference reference = otherClass.getModel().getUnderlyingClassReference(resolver);
      if (reference != null) myClass = reference.getHaxeClass();
    }

    // Optimization
    ASTNode myNode = myClass.getNode();
    ASTNode otherNode = otherClass.getNode();
    if (myNode == otherNode) return true;

    String myClassName = null != myClass ? myClass.getQualifiedName() : null;
    String otherClassName = null != otherClass ? otherClass.getQualifiedName() : null;

    boolean isSame = null != myClassName && myClassName.equals(otherClassName);
    return isSame;
  }
}
