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

import com.intellij.plugins.haxe.lang.psi.*;
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
    }
    else if (from.isUnknown()) {
      from.setType(to.getType().withoutConstantValue());
    }
    return canAssignToFrom(to.getType(), from.getType());
  }

  static private boolean isFunctionTypeOrReference(SpecificTypeReference ref) {
    return ref instanceof SpecificFunctionReference || ref.isFunction();
  }

  static private SpecificFunctionReference asFunctionReference(SpecificTypeReference ref) {
    if (ref instanceof SpecificFunctionReference)
      return (SpecificFunctionReference)ref;

    if (ref.isFunction()) {
      HaxeClass classReference = ((SpecificHaxeClassReference)ref).getHaxeClass();
      if (classReference instanceof HaxeSpecificFunction) {
        HaxeSpecificFunction func = (HaxeSpecificFunction)classReference;
        return SpecificFunctionReference.create(func);
      }
      // The Function class unifies with (can be assigned to by) any function.
      return new SpecificFunctionReference.StdFunctionReference(ref.getElementContext());
    }
    return null;  // XXX: Should throw exception instead??
  }

  static public boolean canAssignToFrom(
    @Nullable SpecificTypeReference to,
    @Nullable SpecificTypeReference from
  ) {
    if (to == null || from == null) return false;
    if (to.isDynamic() || from.isDynamic()) return true;

    if (isFunctionTypeOrReference(to) && isFunctionTypeOrReference(from)) {
      SpecificFunctionReference toRef = asFunctionReference(to);
      SpecificFunctionReference fromRef = asFunctionReference(from);
      return canAssignToFromFunction(toRef, fromRef);
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

    // The Function class is always assignable to other functions.
    if (to instanceof SpecificFunctionReference.StdFunctionReference
        ||from instanceof SpecificFunctionReference.StdFunctionReference) {
      return true;
    }

    int toArgSize = to.arguments.size();
    int fromArgSize = from.arguments.size();
    if (toArgSize == 1 && fromArgSize == 0){  // Single arg of Void is the same as no args.
      if (!to.arguments.get(0).isVoid()) {
        return false;
      }
    } else if (toArgSize == 0 && fromArgSize == 1) {
      if (!from.arguments.get(0).isVoid()) {
        return false;
      }
    } else {
      if (toArgSize != fromArgSize) {
        return false;
      }
      for (int n = 0; n < toArgSize; n++) {
        if (!to.arguments.get(n).canAssign(from.arguments.get(n))) return false;
      }
    }
    // Void return on the to just means that the value isn't used/cared about. See
    // the Haxe manual, section 3.5.4 at https://haxe.org/manual/type-system-unification-function-return.html
    return to.returnValue.isVoid() || to.returnValue.canAssign(from.returnValue);
  }



  static private SpecificHaxeClassReference getUnderlyingClassIfAbstractNull(SpecificHaxeClassReference ref) {
    if (ref.getHaxeClass() instanceof HaxeAbstractClassDeclaration && "Null".equals(ref.getClassName())) {
      SpecificHaxeClassReference underlying = ref.getHaxeClassModel().getUnderlyingClassReference(ref.getGenericResolver());
      if (null != underlying) {
        ref = underlying;
      }
    }
    return ref;
  }

  static private boolean canAssignToFromType(
    @NotNull SpecificHaxeClassReference to,
    @NotNull SpecificHaxeClassReference from
  ) {

    // Null<T> is a special case.  It must act like a T in all ways.  Whereas,
    // any other abstract must act like it hides its internal types.
    to = getUnderlyingClassIfAbstractNull(to);
    from = getUnderlyingClassIfAbstractNull(from);

    if (canAssignToFromSpecificType(to, from)) return true;

    Set<SpecificHaxeClassReference> compatibleTypes = to.getCompatibleTypes(SpecificHaxeClassReference.Compatibility.ASSIGNABLE_FROM);
    for (SpecificHaxeClassReference compatibleType : compatibleTypes) {
      if (canAssignToFromSpecificType(compatibleType, from)) return true;
    }

    compatibleTypes = from.getCompatibleTypes(SpecificHaxeClassReference.Compatibility.ASSIGNABLE_TO);
    for (SpecificHaxeClassReference compatibleType : compatibleTypes) {
      if (canAssignToFromSpecificType(to, compatibleType)) return true;
    }

    compatibleTypes = from.getInferTypes();
    for (SpecificHaxeClassReference compatibleType : compatibleTypes) {
      if (canAssignToFromSpecificType(to, compatibleType)) return true;
    }

    // Last ditch effort...
    return to.toStringWithoutConstant().equals(from.toStringWithoutConstant());
  }

  static private boolean canAssignToFromSpecificType(
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
      else if (from.getSpecifics().length == 0) {
        return true;
      }
    }
    return false;
  }
}
