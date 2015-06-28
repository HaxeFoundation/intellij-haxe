/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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

import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;

import java.util.ArrayList;
import java.util.List;

public class HaxeCustomOperatorModel {
  public final String operator;
  public final List<HaxeMethodModel> methods = new ArrayList<HaxeMethodModel>();

  public HaxeCustomOperatorModel(String operator) {
    this.operator = operator;
  }

  public void addMethod(HaxeOperatorType binary, HaxeMethodModel method) {
    methods.add(method);
  }

  public boolean enabled() {
    return methods.size() > 0;
  }

  public ResultHolder getResultType(SpecificTypeReference left, SpecificTypeReference right) {
    for (HaxeMethodModel method : methods) {
      if (method.isStatic()) {
        final HaxeParametersModel parameters = method.getParameters();
        final HaxeParameterModel param1 = parameters.get(0);
        final HaxeParameterModel param2 = parameters.get(1);
        if (param1 != null && param2 != null) {
          boolean leftCompatible = param1.getType().canAssign(left.createHolder());
          boolean rightCompatible = param2.getType().canAssign(right.createHolder());
          if (leftCompatible && rightCompatible) {
            return method.getReturnType(null);
          }
        }
      }
    }

    return null;
  }
}
