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
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Logger;

public class HaxeParameterDescriptionBuilder {

  @NotNull
  static HaxeParameterDescription[] buildFromList(@Nullable List<HaxeParameter> parameters, HaxeGenericSpecialization specialization) {
    if (parameters == null || parameters.size() == 0) return new HaxeParameterDescription[0];

    int currentOffset = 0;

    HaxeParameterDescription[] result = new HaxeParameterDescription[parameters.size()];

    for (int i = 0; i < parameters.size(); i++) {
      HaxeParameter parameter = parameters.get(i);
      HaxeParameterDescription parameterDescription = build(parameter, specialization, currentOffset);

      currentOffset += parameterDescription.toString().length();

      result[i] = parameterDescription;
    }

    return result;
  }

  @NotNull
  private static HaxeParameterDescription build(HaxeParameter parameter, HaxeGenericSpecialization specialization, int textStartOffset) {
    String name = parameter.getName();
    String type = null;
    String initialValue = null;

    //FIXME: Check if type defined as nullable
    boolean definedAsNullable = false;
    boolean optional = parameter.getIsOptional() != null;

    HaxeTypeTag typeTag = parameter.getTypeTag();
    HaxeVarInit varInit = parameter.getVarInit();

    if (typeTag != null) {
      type = HaxePresentableUtil.buildTypeText(parameter, parameter.getTypeTag(), specialization);
      Logger.getGlobal().info(typeTag.toString());
    }

    if (varInit != null) {
      HaxeExpression varInitExpression = varInit.getExpression();
      if (varInitExpression != null) {
        initialValue = varInit.getExpression().getText();
      }
    }

    return new HaxeParameterDescription(name, type, initialValue, optional, definedAsNullable, textStartOffset);
  }
}
