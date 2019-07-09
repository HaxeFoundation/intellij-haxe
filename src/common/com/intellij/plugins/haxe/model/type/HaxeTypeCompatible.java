/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
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
package com.intellij.plugins.haxe.model.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class HaxeTypeCompatible {
  static public boolean canApplyBinaryOperator(SpecificTypeReference left, SpecificTypeReference right, String operator) {
    // @TODO: Stub. Implement.
    return true;
  }

  static public boolean canAssignToFrom(@Nullable SpecificTypeReference to, @Nullable ResultHolder from) {
    if (null == to || null == from) return false;
    return canAssignToFrom(to, from.getType());
  }

  static public boolean canAssignToFrom(@Nullable ResultHolder to, @Nullable ResultHolder from) {
    if (null == to || null == from) return false;
    if (to.isUnknown()) {
      to.setType(from.getType().withoutConstantValue());
    } else if (from.isUnknown()) {
      from.setType(to.getType().withoutConstantValue());
    }
    return canAssignToFrom(to.getType(), from.getType());
  }

  static public boolean canAssignToFrom(
    @Nullable SpecificTypeReference to,
    @Nullable SpecificTypeReference from
  ) {
    if (to == null || from == null) return false;
    if (to.isDynamic() || from.isDynamic()) return true;

    if (to instanceof SpecificFunctionReference && from instanceof SpecificFunctionReference) {
      return canAssignToFromFunction((SpecificFunctionReference)to, (SpecificFunctionReference)from);
    }

    if (to instanceof SpecificHaxeClassReference && from instanceof SpecificHaxeClassReference) {
      return canAssignToFromType((SpecificHaxeClassReference)to, (SpecificHaxeClassReference)from);
    }

    return false;
  }

  static private boolean canAssignToFromFunction(
    @NotNull SpecificFunctionReference to,
    @NotNull SpecificFunctionReference from
  ) {
    if (to.arguments.size() != from.arguments.size()) return false;
    for (int n = 0; n < to.arguments.size(); n++) {
      if (!to.arguments.get(n).canAssign(from.arguments.get(n))) return false;
    }
    // Void return on the to just means that the value isn't used/cared about. See
    // the Haxe manual, section 3.5.4 at https://haxe.org/manual/type-system-unification-function-return.html
    return to.returnValue.isVoid() || to.returnValue.canAssign(from.returnValue);
  }

  static private boolean canAssignToFromType(
    @NotNull SpecificHaxeClassReference to,
    @NotNull SpecificHaxeClassReference from
  ) {
    if (to.isDynamic() || from.isDynamic()) {
      return true;
    }

    if (from.isUnknown()) {
      return true;
    }

    if (to.getHaxeClassReference().equals(from.getHaxeClassReference())) {
      if (to.getSpecifics().length == from.getSpecifics().length) {
        int specificsLength = to.getSpecifics().length;
        for (int n = 0; n < specificsLength; n++) {
          if (!canAssignToFrom(to.getSpecifics()[n], from.getSpecifics()[n])) {
            return false;
          }
        }
        return true;
      }
      // issue #388: allow `public var m:Map<String, String> = new Map();`
      else if(from.getSpecifics().length == 0) {
        return true;
      }
    }

    Set<SpecificHaxeClassReference> compatibleTypes = to.getCompatibleTypes();
    for (SpecificHaxeClassReference compatibleType : compatibleTypes) {
      if (compatibleType.toStringWithoutConstant().equals(from.toStringWithoutConstant())) return true;
    }

    compatibleTypes = from.getInferTypes();
    for (SpecificHaxeClassReference compatibleType : compatibleTypes) {
      if (compatibleType.toStringWithoutConstant().equals(to.toStringWithoutConstant())) return true;
    }

    return to.toStringWithoutConstant().equals(from.toStringWithoutConstant());
  }
}
