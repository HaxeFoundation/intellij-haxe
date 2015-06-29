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

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeOrAnonymous;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeTypeCompatible {
  static public boolean canApplyBinaryOperator(SpecificTypeReference left, SpecificTypeReference right, String operator) {
    // @TODO: Stub. Implement.
    return true;
  }

  static public boolean canAssignToFrom(SpecificTypeReference to, ResultHolder from) {
    return canAssignToFrom(to, from.getType());
  }

  static public boolean canAssignToFrom(ResultHolder to, ResultHolder from) {
    if (to.isUnknown()) {
      to.setType(from.getType().withoutConstantValue());
    }
    else if (from.isUnknown()) {
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
    if (to.params.size() != from.params.size()) return false;
    for (int n = 0; n < to.params.size(); n++) {
      if (!to.params.get(n).canAssign(from.params.get(n))) return false;
    }
    return to.retval.canAssign(from.retval);
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

    // @TODO: A first tdd-dummy approach
    if (to.clazz.equals(from.clazz)) {
      if (to.specifics.length == from.specifics.length) {
        int specificsLength = to.specifics.length;
        for (int n = 0; n < specificsLength; n++) {
          if (!canAssignToFrom(to.specifics[n], from.specifics[n])) {
            return false;
          }
        }
        return true;
      }
    }

    if (to.toStringWithoutConstant().equals(from.toStringWithoutConstant())) {
      return true;
    }

    // Check from abstracts
    HaxeClass thisClassPsi = to.clazz.getHaxeClass();
    if (thisClassPsi != null) {
      HaxeClassModel thisClass = thisClassPsi.getModel();
      for (HaxeType type : thisClass.getAbstractFromList()) {
        if (HaxeTypeResolver.getTypeFromType(type).toStringWithoutConstant().equals(from.toStringWithoutConstant())) {
          return true;
        }
      }
    }

    // Check to abstracts
    HaxeClass thatClassPsi = from.clazz.getHaxeClass();
    if (thatClassPsi != null) {
      HaxeClassModel thatClass = thatClassPsi.getModel();

      if (thatClass.isAbstract()) {

        // Check if this is required!
        HaxeTypeOrAnonymous underlyingAbstractType = thatClass.getAbstractUnderlyingType();
        if (underlyingAbstractType != null) {
          ResultHolder thatUnderlying = HaxeTypeResolver.getTypeFromTypeOrAnonymous(underlyingAbstractType);
          if (to.toStringWithoutConstant().equals(thatUnderlying.toStringWithoutConstant())) {
            return true;
          }
        }

        for (HaxeType type : thatClass.getAbstractToList()) {
          if (to.canAssign(HaxeTypeResolver.getTypeFromType(type))) {
            return true;
          }
        }
      }
    }

    return to.toStringWithoutConstant().equals(from.toStringWithoutConstant());
  }
}
