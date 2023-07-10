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

import com.intellij.plugins.haxe.lang.psi.HaxeFunctionArgument;
import com.intellij.plugins.haxe.lang.psi.HaxeRestArgumentType;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.model.type.HaxeTypeResolver.getTypeFromFunctionType;
import static com.intellij.plugins.haxe.model.type.HaxeTypeResolver.getTypeFromTypeOrAnonymous;

public class HaxeFunctionTypeParameterModel implements HaxeModel {
  private HaxeFunctionArgument argument;

  public HaxeFunctionTypeParameterModel(HaxeFunctionArgument functionArgument) {
    this.argument = functionArgument;
  }

  @Override
  public PsiElement getBasePsi() {
    return argument;
  }

  public boolean isOptional() {
    return argument.getOptionalMark() != null;
  }

  public boolean isFunctionType() {
    return argument.getFunctionType() != null;
  }

  public ResultHolder getArgumentType() {
    if (argument.getFunctionType() != null) {
      return getTypeFromFunctionType(argument.getFunctionType());
    } else if (argument.getTypeOrAnonymous() != null) {
      return getTypeFromTypeOrAnonymous(argument.getTypeOrAnonymous());
    }
    HaxeRestArgumentType restArgumentType = argument.getRestArgumentType();
    if (restArgumentType!= null) {
      if (restArgumentType.getFunctionType() != null) {
        return getTypeFromFunctionType(restArgumentType.getFunctionType());
      } else if (restArgumentType.getTypeOrAnonymous() != null) {
        return getTypeFromTypeOrAnonymous(restArgumentType.getTypeOrAnonymous());
      }
    }
    return SpecificTypeReference.getUnknown(argument).createHolder();
  }


  public @Nullable String getParameterName() {
    return argument.getComponentName() != null ? argument.getComponentName().getName() : null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public @Nullable HaxeExposableModel getExhibitor() {
    return null;
  }

  @Override
  public @Nullable FullyQualifiedInfo getQualifiedInfo() {
    return null;
  }


  @Override
  public String toString() {
    return argument.toString();
  }

  public boolean isRestArgument() {
    return argument.getRestArgumentType() != null;
  }
}

