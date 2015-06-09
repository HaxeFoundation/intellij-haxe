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
import com.intellij.plugins.haxe.model.HaxeClassModel;

public class HaxeTypeCompatible {
  static public boolean canApplyBinaryOperator(SpecificTypeReference a, SpecificTypeReference b, String operator) {
    //return false;
    return true;
  }

  static public boolean isAssignable(SpecificTypeReference a, SpecificTypeReference b) {
    if (a instanceof SpecificFunctionReference && b instanceof SpecificFunctionReference) {
      return isAssignableFunction((SpecificFunctionReference)a, (SpecificFunctionReference)b);
    }

    if (a instanceof SpecificHaxeClassReference && b instanceof SpecificHaxeClassReference) {
      return isAssignableType((SpecificHaxeClassReference)a, (SpecificHaxeClassReference)b);
    }

    return false;
  }

  static private boolean isAssignableFunction(SpecificFunctionReference a, SpecificFunctionReference b) {
    if (a.params.size() != b.params.size()) return false;
    for (int n = 0; n < a.params.size(); n++) {
      if (!a.params.get(n).canAssign(b.params.get(n))) return false;
    }
    return a.retval.canAssign(b.retval);
  }

  static private boolean isAssignableType(SpecificHaxeClassReference a, SpecificHaxeClassReference b) {
    if (a.toStringWithoutConstant().equals("Dynamic") || b.toStringWithoutConstant().equals("Dynamic")) {
      return true;
    }
    if (a.toStringWithoutConstant().equals(b.toStringWithoutConstant())) {
      return true;
    }

    // Check from abstracts
    HaxeClass thisClassPsi = (a != null && a.clazz != null) ? a.clazz.getHaxeClass() : null;
    if (thisClassPsi != null) {
      HaxeClassModel thisClass = thisClassPsi.getModel();
      for (HaxeType type : thisClass.getAbstractFromList()) {
        if (HaxeTypeResolver.getTypeFromType(type).toStringWithoutConstant().equals(b.toStringWithoutConstant())) {
          return true;
        }
      }
    }

    // Check to abstracts
    HaxeClass thatClassPsi = b.clazz.getHaxeClass();
    if (thatClassPsi != null) {
      HaxeClassModel thatClass = thatClassPsi.getModel();

      if (thatClass.isAbstract()) {

        // Check if this is required!
        HaxeType underlyingAbstractType = thatClass.getAbstractUnderlyingType();
        if (underlyingAbstractType != null) {
          SpecificTypeReference thatUnderlying = HaxeTypeResolver.getTypeFromType(underlyingAbstractType);
          if (a.toStringWithoutConstant().equals(thatUnderlying.toStringWithoutConstant())) {
            return true;
          }
        }

        for (HaxeType type : thatClass.getAbstractToList()) {
          if (a.canAssign(HaxeTypeResolver.getTypeFromType(type))) {
            return true;
          }
        }
      }
    }

    return a.toStringWithoutConstant().equals(b.toStringWithoutConstant());
  }
}
