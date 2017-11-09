/*
 * Copyright 2017-2017 Ilya Malanin
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
package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeUsingStatement;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HaxeUsingModel implements HaxeModel {
  private final HaxeUsingStatement basePsi;

  public HaxeUsingModel(HaxeUsingStatement usingStatement) {
    this.basePsi = usingStatement;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public PsiElement getBasePsi() {
    return basePsi;
  }

  @Nullable
  @Override
  public HaxeExposableModel getExhibitor() {
    return null;
  }

  public FullyQualifiedInfo getQualifiedInfo() {
    HaxeReferenceExpression referenceExpression = getReferenceExpression();
    return referenceExpression == null ? null : new FullyQualifiedInfo(referenceExpression);
  }

  public HaxeReferenceExpression getReferenceExpression() {
    return basePsi.getReferenceExpression();
  }

  @Nullable
  public HaxeMethodModel findExtensionMethod(String name, @NotNull HaxeClass classApplyTo) {
    List<HaxeMethodModel> result = getExtensionMethods(classApplyTo, name);
    return result.isEmpty() ? null : result.get(0);
  }

  @NotNull
  public List<HaxeMethodModel> getExtensionMethods(@NotNull HaxeClass classApplyTo) {
    return getExtensionMethods(classApplyTo, null);
  }

  @NotNull
  private List<HaxeMethodModel> getExtensionMethods(HaxeClass classApplyTo, @Nullable String name) {
    List<HaxeClassModel> classes = getClassModels();
    if (classes == null || classes.isEmpty()) return Collections.emptyList();

    List<HaxeMethodModel> result = null;

    for (HaxeClassModel classModel : classes) {
      List<HaxeMethodModel> methods = null;
      if (name != null) {
        HaxeMethodModel method = classModel.getMethod(name);
        if (method != null) methods = Collections.singletonList(method);
      }
      else {
        methods = classModel.getMethods();
      }

      if (methods == null || methods.isEmpty()) continue;

      for (HaxeMethodModel method : methods) {
        if (method != null && !method.isConstructor() && method.isStatic() && method.isPublic()) {
          List<HaxeParameterModel> parameters = method.getParameters();
          if (!parameters.isEmpty()) {
            //FIXME Switch to specific type with generics
            final HaxeClass firstParameterType = HaxeResolveUtil.getHaxeClassResolveResult(parameters.get(0).getBasePsi()).getHaxeClass();
            final HashSet<HaxeClass> baseClassesSet = HaxeResolveUtil.getBaseClassesSet(classApplyTo);

            final boolean applicable = firstParameterType == classApplyTo || baseClassesSet.contains(firstParameterType);

            if (applicable) {
              if (result == null) result = new ArrayList<>();
              result.add(method);
            }
          }
        }
      }
    }

    return result == null ? Collections.emptyList() : result;
  }

  public List<HaxeClassModel> getClassModels() {
    List<HaxeModel> result = HaxeProjectModel.fromElement(this.basePsi).resolve(getQualifiedInfo(), this.basePsi.getResolveScope());
    if (result == null || result.isEmpty()) return null;

    return result.stream()
      .flatMap(model -> (model instanceof HaxeFileModel)
                        ? ((HaxeFileModel)model).getClassModels().stream()
                        : Stream.of(model))
      .filter(model -> model instanceof HaxeClassModel)
      .map(model -> (HaxeClassModel)model)
      .collect(Collectors.toList());
  }
}
