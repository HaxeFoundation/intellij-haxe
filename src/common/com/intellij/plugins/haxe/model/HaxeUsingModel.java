/*
 * Copyright 2017-2017 Ilya Malanin
 * Copyright 2020 Eric Bishton
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
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HaxeUsingModel extends HaxeImportableModel {

  public HaxeUsingModel(@NotNull HaxeUsingStatement usingStatement) {
    super(usingStatement);
  }

  protected HaxeUsingModel(@NotNull PsiElement basePsi) {
    super(basePsi);
  }

  @Override
  public String getName() {
    return null;
  }

  @Nullable
  @Override
  public HaxeExposableModel getExhibitor() {
    return null;
  }

  @Nullable
  @Override
  public HaxeReferenceExpression getReferenceExpression() {
    PsiElement basePsi = getBasePsi();
    if (basePsi instanceof HaxeUsingStatement) {
      return ((HaxeUsingStatement)basePsi).getReferenceExpression();
    }
    return null;
  }

  @Nullable
  @Override
  public PsiElement exposeByName(String name) {
    if (name == null || name.isEmpty()) return null;

    if (getReferenceExpression() != null) {
      HaxeModel member = getExposedMember(name);
      if (member != null) {
        return member.getBasePsi();
      }
    }

    return null;
  }

  @Nullable
  public HaxeMethodModel findExtensionMethod(String name, SpecificHaxeClassReference classApplyTo) {
    List<HaxeMethodModel> result = getExtensionMethods(classApplyTo, name);
    return result.isEmpty() ? null : result.get(0);
  }

  @NotNull
  public List<HaxeMethodModel> getExtensionMethods(@NotNull HaxeClass classApplyTo) {
    HaxeClassModel model = HaxeClassModel.fromElement(classApplyTo);
    SpecificHaxeClassReference classReference = SpecificHaxeClassReference.withoutGenerics(model.getReference());
    return getExtensionMethods(classReference, null);
  }

  @NotNull
  private List<HaxeMethodModel> getExtensionMethods(@NotNull SpecificHaxeClassReference classApplyTo, @Nullable String name) {
    List<HaxeClassModel> classes = getClassModels();
    if (classes == null || classes.isEmpty()) return Collections.emptyList();

    List<HaxeMethodModel> result = null;

    HaxeGenericResolver resolver = classApplyTo.getGenericResolver();
    ResultHolder classResult = classApplyTo.createHolder();

    for (HaxeClassModel classModel : classes) {
      List<HaxeMethodModel> methods = null;
      if (name != null) {
        HaxeMethodModel method = classModel.getMethod(name, resolver);
        if (method != null) methods = Collections.singletonList(method);
      }
      else {
        methods = classModel.getMethods(resolver);
      }

      if (methods == null || methods.isEmpty()) continue;

      for (HaxeMethodModel method : methods) {
        if (method != null && !method.isConstructor() && method.isStatic() && method.isPublic()) {
          List<HaxeParameterModel> parameters = method.getParameters();
          if (!parameters.isEmpty()) {
            HaxeParameterModel paramModel = parameters.get(0);
            ResultHolder paramResult = paramModel.getType(resolver);
            final boolean applicable = paramResult.canAssign(classResult);

            ////FIXME Switch to specific type with generics
            //final HaxeClass firstParameterType = HaxeResolveUtil.getHaxeClassResolveResult(parameters.get(0).getBasePsi()).getHaxeClass();
            //final HashSet<HaxeClass> baseClassesSet = HaxeResolveUtil.getBaseClassesSet(classApplyTo);
            //
            //final boolean applicable = firstParameterType == classApplyTo || baseClassesSet.contains(firstParameterType);

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
