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

import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class HaxeTypeUnifier {
  @NotNull
  static public ResultHolder unify(ResultHolder a, ResultHolder b) {
    return unify(a.getType(), b.getType(), a.getType().context).createHolder();
  }

  @NotNull
  static public SpecificTypeReference unify(SpecificTypeReference a, SpecificTypeReference b, @NotNull PsiElement context) {
    if (a == null && b == null) return SpecificTypeReference.getUnknown(context);
    if (a == null) return b;
    if (b == null) return a;

    // Completely equals!
    if (a.toStringWithoutConstant().equals(b.toStringWithoutConstant())) {
      return a.withoutConstantValue();
    }

    if (a instanceof SpecificHaxeClassReference && b instanceof SpecificHaxeClassReference) {
      return unifyTypes((SpecificHaxeClassReference)a, (SpecificHaxeClassReference)b, context);
    }
    if (a instanceof SpecificFunctionReference && b instanceof SpecificFunctionReference) {
      return unifyFunctions((SpecificFunctionReference)a, (SpecificFunctionReference)b, context);
    }

    return SpecificTypeReference.getUnknown(a.getElementContext());
  }

  @NotNull
  static public SpecificTypeReference unifyFunctions(SpecificFunctionReference a, SpecificFunctionReference b, @NotNull PsiElement context) {
    final List<ResultHolder> pa = a.getParameters();
    final List<ResultHolder> pb = b.getParameters();
    //if (pa.size() != pb.size()) throw new HaxeCannotUnifyException();
    if (pa.size() != pb.size()) return SpecificTypeReference.getInvalid(a.getElementContext());
    int size = pa.size();
    final ArrayList<ResultHolder> params = new ArrayList<ResultHolder>();
    for (int n = 0; n < size; n++) {
      final ResultHolder param = unify(pa.get(n), pb.get(n));
      if (param.getType().isInvalid()) return SpecificTypeReference.getInvalid(a.getElementContext());
      params.add(param);
    }
    final ResultHolder retval = unify(a.getReturnType(), b.getReturnType());
    return new SpecificFunctionReference(params, retval, null, context);
  }

  @NotNull
  static public SpecificTypeReference unifyTypes(SpecificHaxeClassReference a, SpecificHaxeClassReference b, @NotNull PsiElement context) {
    if (a.isDynamic()) return a.withoutConstantValue();
    if (b.isDynamic()) return b.withoutConstantValue();
    if (a.getHaxeClassModel() == null) return SpecificTypeReference.getDynamic(context);
    if (b.getHaxeClassModel() == null) return SpecificTypeReference.getDynamic(context);
    final Set<HaxeClassModel> atypes = a.getHaxeClassModel().getCompatibleTypes();
    final Set<HaxeClassModel> btypes = b.getHaxeClassModel().getCompatibleTypes();
    // @TODO: this could be really slow, hotspot for optimizing
    for (HaxeClassModel type : atypes) {
      if (btypes.contains(type)) {
        // @TODO: generics
        return SpecificHaxeClassReference.withoutGenerics(
          new HaxeClassReference(type, context)
        );
      }
    }

    // @TODO: Do a proper unification
    return SpecificTypeReference.getDynamic(a.getElementContext());
  }

  @NotNull
  static public SpecificTypeReference unify(SpecificTypeReference[] types, @NotNull PsiElement context) {
    return unify(Arrays.asList(types), context);
  }

  @NotNull
  static public SpecificTypeReference unify(List<SpecificTypeReference> types, @NotNull PsiElement context) {
    if (types.size() == 0) {
      return SpecificTypeReference.getUnknown(context);
    }
    SpecificTypeReference type = types.get(0);
    for (int n = 1; n < types.size(); n++) {
      type = unify(type, types.get(n), context);
    }
    return type;
  }

  @NotNull
  static public ResultHolder unifyHolders(List<ResultHolder> typeHolders, @NotNull PsiElement context) {
    // @TODO: This should mutate unknown holders?
    return unify(ResultHolder.types(typeHolders), context).createHolder();
  }
}
