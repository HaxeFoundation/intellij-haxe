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

import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import org.jetbrains.annotations.Nullable;

public class HaxeParameterDescription {
  private final boolean isOptional;
  private final boolean hasInitialValue;

  private final String description;

  private final ResultHolder resultHolder;

  HaxeParameterDescription(String name,
                           String type,
                           String initialValue,
                           boolean isOptional,
                           ResultHolder resultHolder) {

    this.isOptional = isOptional;
    this.hasInitialValue = initialValue != null;

    this.description = compilePresentableDescription(name, type, initialValue);
    this.resultHolder = resultHolder;
  }

  @Override
  public String toString() {
    return getPresentableText();
  }

  public String getPresentableText() {
    return description;
  }

  public boolean isOptional() {
    return isOptional;
  }

  public boolean hasInitialValue() {
    return hasInitialValue;
  }

  public boolean isPredefined() {
    return isOptional || hasInitialValue;
  }

  private String compilePresentableDescription(String name, @Nullable String type, @Nullable String initialValue) {
    final boolean hasInitialValue = initialValue != null && !initialValue.isEmpty();
    final boolean hasType = type != null && !type.isEmpty();
    final boolean isValuePredefined = isOptional || hasInitialValue;

    final StringBuilder result = new StringBuilder();
    if (isOptional) {
      result.append("?");
    }
    
    result.append(name);
    result.append(":");
    if (hasType) {
      result.append(type);
    }
    else {
      result.append(HaxePresentableUtil.unknownType());
    }

    if (isValuePredefined) {
      result
        .append(" = ")
        .append(isOptional ? "null" : initialValue);
    }

    return result.toString();
  }

  public ResultHolder getResultHolder() {
    return resultHolder;
  }
}
