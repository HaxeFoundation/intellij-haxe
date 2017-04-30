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

import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import org.jetbrains.annotations.Nullable;

public class HaxeParameterDescription {
  private final boolean isOptional;
  private final boolean isDefinedAsNullable;

  private final String text;

  HaxeParameterDescription(String name, String type, String initialValue, boolean isOptional, boolean isDefinedAsNullable, int textOffset) {
    this.isOptional = isOptional;
    this.isDefinedAsNullable = isDefinedAsNullable;

    this.text = compileDescription(name, type, initialValue);
  }

  @Override
  public String toString() {
    return text;
  }

  public boolean getIsOptional() {
    return isOptional;
  }

  public boolean getIsDefinedAsNullable() {
    return isDefinedAsNullable;
  }

  private String compileDescription(String name, @Nullable String type, @Nullable String initialValue) {
    final boolean hasInitialValue = initialValue != null && !initialValue.isEmpty();
    final boolean hasType = type != null && !type.isEmpty();
    final boolean isValuePredefined = isOptional || hasInitialValue;

    final StringBuilder result = new StringBuilder();
    if (isValuePredefined) {
      result.append("[");
    }
    result.append(name);
    if (hasType) {
      result.append(":");
      if (isOptional && !isDefinedAsNullable) {
        result.append(HaxePresentableUtil.asNullable(type));
      }
      else {
        result.append(type);
      }
    }
    else {
      result.append(":").append(HaxePresentableUtil.unknownType());
    }
    if (hasInitialValue) {
      result.append(" = ").append(initialValue);
    }
    else if (isOptional) {
      result.append(" = null");
    }
    if (isValuePredefined) {
      result.append("]");
    }

    return result.toString();
  }
}
