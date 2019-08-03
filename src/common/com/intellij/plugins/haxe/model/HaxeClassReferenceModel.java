/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2019 Eric Bishton
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
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeClassReferenceModel {
  public HaxeType type;
  private HaxeClass _clazz;

  public HaxeClassReferenceModel(HaxeType type) {
    this.type = type;
  }

  public HaxeType getPsi() {
    return type;
  }

  @NotNull
  public List<HaxeTypeParameterModel> getTypeParameters() {
    if (null == type) return HaxeTypeParameterModel.EMPTY_LIST;
    return HaxeTypeParameterModel.fromParameterSet(type.getTypeParam());
  }

  public boolean hasParameters() {
    return null != type && null != type.getTypeParam();
  }

  @Nullable
  public HaxeClassModel getHaxeClass() {
    if (_clazz == null) {
      _clazz = HaxeResolveUtil.getHaxeClassResolveResult(type).getHaxeClass();
    }
    return (_clazz != null) ? _clazz.getModel() : null;
  }
}
