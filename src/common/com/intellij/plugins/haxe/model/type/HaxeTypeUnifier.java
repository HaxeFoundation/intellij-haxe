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

public class HaxeTypeUnifier {
  static public SpecificTypeReference unify(SpecificTypeReference a, SpecificTypeReference b) {
    // @TODO: Do a proper unification
    return a.withConstantValue(null);
    //return a;
  }

  static public SpecificTypeReference unify(SpecificTypeReference[] types) {
    if (types.length == 0) return null;
    SpecificTypeReference type = types[0];
    for (int n = 1; n < types.length; n++) {
      type = unify(type, types[n]);
    }
    return type;
  }
}
