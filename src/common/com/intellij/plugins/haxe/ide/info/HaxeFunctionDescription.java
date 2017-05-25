/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.Nullable;

public class HaxeFunctionDescription {
  private static final TextRange ZERO_TEXT_RANGE = new TextRange(0, 0);
  private static final String PARAMETERS_DELIMITER = ", ";

  private final HaxeParameterDescription[] parameters;

  private final String description;
  private final TextRange[] parametersTextRange;

  HaxeFunctionDescription(@Nullable HaxeParameterDescription[] parameters) {
    this.parameters = parameters;
    this.parametersTextRange = parameters == null ? null : new TextRange[parameters.length];

    this.description = compilePresentableDescription();
  }
  
  public HaxeParameterDescription[] getParameters() {
    return parameters;
  }

  private String compilePresentableDescription() {
    final StringBuilder result = new StringBuilder();

    int currentOffset = 0;

    if (parameters.length == 0) {
      return HaxeBundle.message("haxe.parameter.info.helper.no.parameters");
    }

    for (int i = 0; i < parameters.length; i++) {
      HaxeParameterDescription parameter = parameters[i];
      String description = parameter.getPresentableText();
      int descriptionLength = description.length();

      parametersTextRange[i] = new TextRange(currentOffset, currentOffset + descriptionLength);

      if (currentOffset > 0) {
        result.append(PARAMETERS_DELIMITER);
      }
      result.append(description);

      currentOffset += descriptionLength + PARAMETERS_DELIMITER.length();
    }

    return result.toString();
  }

  TextRange getParameterRange(int index) {
    if (index == -1 || index >= parameters.length) {
      return ZERO_TEXT_RANGE;
    }

    return parametersTextRange[index];
  }

  @Override
  public String toString() {
    return getPresentableText();
  }

  public String getPresentableText() {
    return description;
  }
}
