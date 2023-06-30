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
package com.intellij.plugins.haxe.ide.info;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.type.HaxeClassReference;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeParameterDescriptionBuilder {

  @NotNull
  static HaxeParameterDescription[] buildFromList(@Nullable List<HaxeParameter> parameters, HaxeResolveResult resolveResult) {
    if (parameters == null || parameters.size() == 0) return new HaxeParameterDescription[0];

    int parameterSize = parameters.size();
    HaxeParameterDescription[] result = new HaxeParameterDescription[parameterSize];

    for (int i = 0; i < parameterSize; i++) {
      HaxeParameter parameter = parameters.get(i);
      HaxeParameterDescription parameterDescription = build(parameter, resolveResult);

      result[i] = parameterDescription;
    }

    return result;
  }

  @NotNull
  private static HaxeParameterDescription build(HaxeParameter parameter, HaxeResolveResult resolveResult) {
    final HaxeParameterModel model = new HaxeParameterModel(parameter);

    String name = model.getName();
    String type;
    String initialValue = null;

    boolean optional = model.hasOptionalPsi();

    HaxeTypeTag typeTag = model.getTypeTagPsi();
    HaxeVarInit varInit = model.getVarInitPsi();

    if (typeTag != null) {
      type = HaxePresentableUtil.buildTypeText(parameter, parameter.getTypeTag(), resolveResult.getSpecialization());
    }
    else {
      type = HaxePresentableUtil.unknownType();
    }

    if (varInit != null) {
      HaxeExpression varInitExpression = varInit.getExpression();
      if (varInitExpression != null) {
        initialValue = varInit.getExpression().getText();
      }
    }

    ResultHolder resultHolder = HaxeTypeResolver.getTypeFromTypeTag(typeTag, parameter);

    String specificTypeText = resultHolder.getType().toStringWithoutConstant();

    if (resolveResult.getSpecialization().containsKey(parameter, specificTypeText)) {
      HaxeResolveResult genericClassResolveResult = resolveResult.getSpecialization().get(parameter, specificTypeText);
      HaxeClass genericHaxeClass = genericClassResolveResult.getHaxeClass();
      PsiElement context = parameter.getContext();
      if (genericHaxeClass != null && context != null) {
        HaxeClassReference genericClassReference = new HaxeClassReference(genericHaxeClass.getModel(), context);

        resultHolder.setType(SpecificHaxeClassReference.withoutGenerics(genericClassReference));
      }
    }

    return new HaxeParameterDescription(name, type, initialValue, optional, resultHolder);
  }
}
