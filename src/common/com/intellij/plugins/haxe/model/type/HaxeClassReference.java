/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
 * Copyright 2019 Eric Bishton
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

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class HaxeClassReference {
  public final String name;
  @NotNull
  public final PsiElement elementContext;
  public final HaxeClassModel clazz;

  private static final Key<Pair<Integer, String>> CLASS_NAME_KEY = new Key<>("HAXE_CLASS_NAME");

  public HaxeClassReference(@NotNull HaxeClassModel clazz, @NotNull PsiElement elementContext) {
    this.name = getClassName(clazz);
    this.elementContext = elementContext;
    this.clazz = clazz;
  }

  public HaxeClassReference(String name, @NotNull PsiElement elementContext) {
    this.name = name;
    this.elementContext = elementContext;
    this.clazz = null;
  }

  private String getClassName(HaxeClassModel clazz) {
    Pair<Integer, String> className = clazz.haxeClass.getUserData(CLASS_NAME_KEY);
    if (className == null || className.first != clazz.haxeClass.hashCode()) {
      className = new Pair<>(clazz.haxeClass.hashCode(), getClassNameInternal(clazz));
      clazz.haxeClass.putUserData(CLASS_NAME_KEY, className);
    }

    return className.second;
  }

  private String getClassNameInternal(HaxeClassModel clazz) {
    if (clazz.haxeClass instanceof HaxeAnonymousType) {
      HaxeNamedComponent namedComponent = HaxeResolveUtil.findTypeParameterContributor(clazz.getBasePsi());
      if (namedComponent instanceof HaxeTypedefDeclaration) {
        final HaxeComponentName name = namedComponent.getComponentName();
        if (name != null) {
          return name.getText();
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
    if (this.clazz != null) return this.clazz.getPsi();
    HaxeClass clazz = HaxeResolveUtil.findClassByQName(name, elementContext);
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

  public String getName() {
    return this.name;
  }

  @Override
  public boolean equals(Object obj) {
    // @TODO: This should check fqNames
    if (obj instanceof HaxeClassReference) return getName().equals(((HaxeClassReference)obj).getName());
    return false;
  }
}
