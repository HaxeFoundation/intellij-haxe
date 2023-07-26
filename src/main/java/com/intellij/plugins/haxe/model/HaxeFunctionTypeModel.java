/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2020 Eric Bishton
 * Copyright 2017-2018 Ilya Malanin
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

import com.intellij.plugins.haxe.lang.psi.HaxeFunctionReturnType;
import com.intellij.plugins.haxe.lang.psi.HaxeFunctionType;
import com.intellij.plugins.haxe.lang.psi.HaxeGenericSpecialization;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeFunctionTypeModel implements HaxeModel {
  private HaxeFunctionType functionType;

  public HaxeFunctionTypeModel(HaxeFunctionType functionType) {
    this.functionType = functionType;
  }

  @Override
  public String getName() {
    return HaxePresentableUtil.buildTypeText(this.functionType, HaxeGenericSpecialization.EMPTY);
  }

  @Override
  public PsiElement getBasePsi() {
    return functionType;
  }

  public HaxeFunctionType getFunctionType() {
    return functionType;
  }

  public List<HaxeFunctionTypeParameterModel> getParameters() {
    if (functionType == null) return List.of();
    return functionType.getFunctionArgumentList().stream()
      .map(HaxeFunctionTypeParameterModel::new)
      .toList();
  }

  public ResultHolder getReturnType() {
    HaxeFunctionReturnType type = functionType.getFunctionReturnType();
    if (type.getFunctionType() != null) {
      return HaxeTypeResolver.getTypeFromFunctionType(type.getFunctionType());
    }
    else if (type.getTypeOrAnonymous() != null) {
      return HaxeTypeResolver.getTypeFromTypeOrAnonymous(type.getTypeOrAnonymous());
    }
    else {
      return SpecificTypeReference.getUnknown(type).createHolder();
    }
  }

  @Override
  public String toString() {
    return "HaxeFunctionTypeModel(" + HaxePresentableUtil.buildTypeText(this.functionType, HaxeGenericSpecialization.EMPTY) + ")";
  }

  @Override
  public @Nullable HaxeExposableModel getExhibitor() {
    return null;
  }

  @Override
  public @Nullable FullyQualifiedInfo getQualifiedInfo() {
    return null;
  }
}

