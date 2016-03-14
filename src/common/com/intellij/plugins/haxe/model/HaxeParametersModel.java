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

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.resolver.HaxeResolver2Parameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class HaxeParametersModel implements Iterable<HaxeParameterModel> {
  @NotNull private final List<HaxeParameterModel> parameters;
  @NotNull private final HaxeFunctionModel function;

  private HaxeParametersModel(@NotNull HaxeFunctionModel function) {
    this.function = function;
    this.parameters = new ArrayList<HaxeParameterModel>();
  }

  static HaxeParametersModel fromHaxeParameterList(@NotNull HaxeFunctionModel function, HaxeParameterList list, boolean isExtensionMethod) {
    HaxeParametersModel params = new HaxeParametersModel(function);
    for (HaxeParameter parameter : list.getParameterList()) {
      params.parameters.add(new HaxeParameterModel(parameter, params));
    }
    if (isExtensionMethod) {
      params.parameters.remove(0);
    }
    return params;
  }

  public HaxeResolver2Parameters getResolver() {
    return new HaxeResolver2Parameters(this);
  }

  @Nullable
  public HaxeParameterModel get(int index) {
    int size = parameters.size();
    return (index >= 0 || index < size) ? parameters.get(index) : null;
  }

  public int length() { return parameters.size(); }

  @NotNull
  public HaxeFunctionModel getFunction() {
    return function;
  }

  public List<HaxeParameterModel> getList() {
    return Collections.unmodifiableList(parameters);
  }

  @Override
  public Iterator<HaxeParameterModel> iterator() {
    return parameters.iterator();
  }
}
