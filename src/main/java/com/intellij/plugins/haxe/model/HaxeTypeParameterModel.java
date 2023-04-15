/*
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * HaxeTypeParameterModel (typeListPart in haxe.bnf) occurs for type
 * *references* with generic parameters -- meaning in the implements
 * and extends lists.
 *
 * See {@link HaxeGenericParamModel} for generic type declarations.
 */
@SuppressWarnings("WeakerAccess")
public class HaxeTypeParameterModel {
  public static final List<HaxeTypeParameterModel> EMPTY_LIST = new ArrayList<>(0);

  final HaxeTypeParam parentParam;
  final HaxeTypeListPart part;
  final int index;

  @NotNull
  public static List<HaxeTypeParameterModel> fromParameterSet(@Nullable HaxeTypeParam param) {
    if (null == param) return EMPTY_LIST;

    List<HaxeTypeListPart> parts = param.getTypeList().getTypeListPartList();
    if (!parts.isEmpty()) {
      List<HaxeTypeParameterModel> models = new ArrayList<>();
      int i = 0;
      for (HaxeTypeListPart part : parts) {
        models.add(new HaxeTypeParameterModel(param, part, i++));
      }
      return models;
    }
    return EMPTY_LIST;
  }

  public HaxeTypeParameterModel(@NotNull HaxeTypeParam param,
                                @NotNull HaxeTypeListPart part,
                                int index) {
    this.parentParam = param;
    this.part = part;
    this.index = index;
  }

  public boolean isExpression() {
    return null != part.getExpression();
  }

  public boolean isFunctionType() {
    return null != part.getFunctionType();
  }

  public boolean isAnonymousType() {
    HaxeTypeOrAnonymous toa = part.getTypeOrAnonymous();
    return null != toa && null != toa.getAnonymousType();
  }

  public boolean isNamedType() {
    HaxeTypeOrAnonymous toa = part.getTypeOrAnonymous();
    return null != toa && null != toa.getType();
  }

  /** Don't use this if you don't have to. */
  public HaxeExpression getExpression() {
    // TODO: Don't let the PSI escape the model.
    return part.getExpression();
  }

  /** Don't use this if you don't have to. */
  public HaxeFunctionType getFunctionType() {
    // TODO: Don't let the PSI escape the model.
    return part.getFunctionType();
  }

  public HaxeClassReferenceModel getTypeReference() {
    HaxeTypeOrAnonymous toa = part.getTypeOrAnonymous();
    if (null != toa) {
      HaxeType type = toa.getType();
      if (null != type) {
        return new HaxeClassReferenceModel(type);
      }
    }
    return null;
  }

  public HaxeClassModel getAnonymousType() {
    HaxeTypeOrAnonymous toa = part.getTypeOrAnonymous();
    if (null != toa) {
      HaxeAnonymousType type = toa.getAnonymousType();
      if (null != type) {
        return type.getModel();
      }
    }
    return null;
  }
}
