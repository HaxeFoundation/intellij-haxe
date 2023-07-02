/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
 * Copyright 2020 Eric Bishton
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
import com.intellij.plugins.haxe.model.type.SpecificFunctionReference.Argument;
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
    if (a.isEnumValue() && b.isEnumValue()) {
      return unifyEnumValues(a, b);
    }

    return SpecificTypeReference.getUnknown(a.getElementContext());
  }

  @NotNull
  static public SpecificTypeReference unifyFunctions(SpecificFunctionReference a,
                                                     SpecificFunctionReference b,
                                                     @NotNull PsiElement context) {
    final List<Argument> pa = a.getArguments();
    final List<Argument> pb = b.getArguments();
    if (pa.size() != pb.size()) return SpecificTypeReference.getInvalid(a.getElementContext());
    final ArrayList<Argument> arguments = new ArrayList<>();

    int size = pa.size();
    for (int n = 0; n < size; n++) {
      final Argument unifiedArgument = unify(pa.get(n), pb.get(n));
      if (unifiedArgument.isInvalid()) return SpecificTypeReference.getInvalid(a.getElementContext());
      arguments.add(unifiedArgument);
    }
    final ResultHolder returnValue = unify(a.getReturnType(), b.getReturnType());
    return new SpecificFunctionReference(arguments, returnValue, null, context);
  }

  @NotNull
  private static Argument unify(Argument a, Argument b) {
    if (a.isOptional() != b.isOptional()) {
      ResultHolder invalidType = SpecificTypeReference.getInvalid(a.getType().getElementContext()).createHolder();
      return new Argument(a.getIndex(), a.isOptional(), invalidType, a.getName());
    }

    return new Argument(a.getIndex(), a.isOptional(), unify(a.getType(), b.getType()), a.getName());
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
    // hack to get around recursive methods
    // (they will end up as return type void when recursion prevents the current resolving code form reaching a return statment with a real type)
    if(a.isVoid() && !b.isVoid()) {
      return b;
    }else if (!a.isVoid() && b.isVoid()) {
      return a;
    }else {
      // @TODO: Do a proper unification
      //return SpecificTypeReference.getDynamic(a.getElementContext());
      return SpecificTypeReference.getUnknown(a.getElementContext());
    }
  }

  @NotNull
  static public SpecificTypeReference unifyEnumValues(SpecificTypeReference a, SpecificTypeReference b) {
    if (a.isEnumValueClass()) {
      if (b.isEnumValueClass()) {
        return a;
      }
      else if (b instanceof SpecificEnumValueReference) {
        return ((SpecificEnumValueReference)b).clone();
      }
    }
    else if (a instanceof SpecificEnumValueReference) {
      if (b.isEnumValueClass()) {
        return ((SpecificEnumValueReference)a).clone();
      }
      else if (b instanceof SpecificEnumValueReference) {
        ResultHolder atype = ((SpecificEnumValueReference)a).getType();
        ResultHolder btype = ((SpecificEnumValueReference)b).getType();
        SpecificHaxeClassReference aclass = atype.getClassType();
        SpecificHaxeClassReference bclass = btype.getClassType();
        if (null != aclass && null != bclass) {
          return unifyTypes(aclass, bclass, a.getElementContext());
        }
      }
    }
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
