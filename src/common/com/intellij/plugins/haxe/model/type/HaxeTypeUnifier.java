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

import com.intellij.plugins.haxe.model.type.SpecificTypeReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HaxeTypeUnifier {
  static public SpecificTypeReference unify(SpecificTypeReference a, SpecificTypeReference b) {
    if (a == null && b == null) return SpecificTypeReference.getUnknown(null);
    if (a == null) return b;
    if (b == null) return a;

    // Completely equals!
    if (a.toStringWithoutConstant().equals(b.toStringWithoutConstant())) {
      return a.withoutConstantValue();
    }

    if (a instanceof SpecificHaxeClassReference && b instanceof SpecificHaxeClassReference) {
      return unifyTypes((SpecificHaxeClassReference)a, (SpecificHaxeClassReference)b);
    }
    if (a instanceof SpecificFunctionReference && b instanceof SpecificFunctionReference) {
      return unifyFunctions((SpecificFunctionReference)a, (SpecificFunctionReference)b);
    }

    return SpecificTypeReference.getUnknown(a.getElementContext());
  }

  static public SpecificTypeReference unifyFunctions(SpecificFunctionReference a, SpecificFunctionReference b)
    {
    final List<SpecificTypeReference> pa = a.getParameters();
    final List<SpecificTypeReference> pb = b.getParameters();
    //if (pa.size() != pb.size()) throw new HaxeCannotUnifyException();
    if (pa.size() != pb.size()) return SpecificTypeReference.getInvalid(a.getElementContext());
    int size = pa.size();
    final ArrayList<SpecificTypeReference> params = new ArrayList<SpecificTypeReference>();
    for (int n = 0; n < size; n++) {
      final SpecificTypeReference param = unify(pa.get(n), pb.get(n));
      if (param.isInvalid()) return SpecificTypeReference.getInvalid(a.getElementContext());
      params.add(param);
    }
    final SpecificTypeReference retval = unify(a.getReturnType(), b.getReturnType());
    return new SpecificFunctionReference(params, retval, null);
  }

  static public SpecificTypeReference unifyTypes(SpecificHaxeClassReference a, SpecificHaxeClassReference b) {
    // @TODO: Do a proper unification
    return a.withoutConstantValue();
  }

  static public SpecificTypeReference unify(SpecificTypeReference[] types) {
    return unify(Arrays.asList(types));
  }

  static public SpecificTypeReference unify(List<SpecificTypeReference> types) {
    if (types.size() == 0) {
      return SpecificTypeReference.getUnknown(null);
    }
    SpecificTypeReference type = types.get(0);
    for (int n = 1; n < types.size(); n++) {
      type = unify(type, types.get(n));
    }
    return type;
  }
}
