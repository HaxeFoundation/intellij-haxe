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
package com.intellij.plugins.haxe.model.type;

import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpecificFunctionReference extends SpecificTypeReference {
  final public List<ResultHolder> params;
  final public ResultHolder retval;

  @Nullable
  final public HaxeMethodModel method;

  public SpecificFunctionReference(List<ResultHolder> params, ResultHolder retval, @Nullable HaxeMethodModel method, @NotNull PsiElement context) {
    super(context);
    this.params = params;
    this.retval = retval;
    this.method = method;
  }

  @Override
  public SpecificFunctionReference withConstantValue(Object constantValue) {
    return new SpecificFunctionReference(params, retval, method, context);
  }

  public int getNonOptionalArgumentsCount() {
    if (method != null) {
      int count = 0;
      for (HaxeParameterModel param : method.getParameters()) {
        if (!param.isOptional()) {
          count++;
        } else {
          break;
        }
      }
      return count;
    }
    return params.size();
  }

  public List<ResultHolder> getParameters() {
    return params;
  }

  public ResultHolder getReturnType() {
    return retval;
  }

  @Override
  public String toString() {
    String out = "";
    if (params.size() == 0) {
      out += "Void";
    } else {
      for (int n = 0; n < params.size(); n++) {
        if (n > 0) out += " -> ";
        out += params.get(n);
      }
    }
    out += " -> ";
    out += retval;
    //return toStringWithoutConstant();
    return out;
  }

  public String getDebugString() {
    if (this.method != null) {
      return this.method.toString() + " : " + this.toString();
    } else {
      return this.toString();
    }
  }

  @Override
  public String toStringWithoutConstant() {
    return toString();
  }
}
