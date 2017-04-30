/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2017 AS3Boyan
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
package com.intellij.plugins.haxe.ide.generation;

import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;

public class GetterSetterMethodBuilder {
  public static void buildGetter(StringBuilder result, HaxeFieldModel field) {
    build(result, field, true);
  }

  public static void buildSetter(StringBuilder result, HaxeFieldModel field) {
    build(result, field, false);
  }

  public static void build(StringBuilder result, HaxeFieldModel namedComponent, boolean isGetter) {
    Boolean isStatic = namedComponent.isStatic();
    String name = namedComponent.getName();
    String type = namedComponent.getResultType().isUnknown() ? "" : namedComponent.getResultType().toStringWithoutConstant();
    if (isStatic) {
      result.append("static ");
    }
    result.append("function ");
    result.append(isGetter ? HaxePresentableUtil.getterName(name) : HaxePresentableUtil.setterName(name));
    result.append("(");
    if (!isGetter) {
      result.append("value");

      if (!type.isEmpty()) {
        result.append(":");
        result.append(type);
      }
    }
    result.append(")");
    if (isGetter && !type.isEmpty()) {
      result.append(":");
      result.append(type);
    }
    result.append("{\n");
    if (isGetter) {
      result.append("return ");
      result.append(name);
      result.append(";");
    }
    else {
      result.append("return ");
      if (!isStatic) {
        result.append("this.");
      }
      result.append(name);
      result.append("= value;");
    }
    result.append("\n}");
  }
}
