/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
 * Copyright 2019-2020 Eric Bishton
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

import com.intellij.plugins.haxe.lang.psi.HaxeAbstractClassDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeSpecificFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

import static com.intellij.plugins.haxe.model.type.SpecificTypeReference.getStdClass;

public class HaxeTypeCompatible {
  static public boolean canApplyBinaryOperator(SpecificTypeReference left, SpecificTypeReference right, String operator) {
    // @TODO: Stub. Implement.
    return true;
  }

  static public boolean canAssignToFrom(@Nullable SpecificTypeReference to, @Nullable ResultHolder from) {
    return canAssignToFrom(to, from, false);
  }
  static public boolean canAssignToFrom(@Nullable SpecificTypeReference to, @Nullable ResultHolder from, boolean variableInit) {
    if (null == to || null == from) return false;
    return canAssignToFrom(to, from.getType(), variableInit);
  }

  static public boolean canAssignToFrom(@Nullable ResultHolder to, @Nullable ResultHolder from) {
    return canAssignToFrom(to, from, false);
  }
  static public boolean canAssignToFrom(@Nullable ResultHolder to, @Nullable ResultHolder from, boolean variableInit) {
    if (null == to || null == from) return false;
    if (to.isUnknown()) {
      to.setType(from.getType().withoutConstantValue());
    }
    else if (from.isUnknown()) {
      from.setType(to.getType().withoutConstantValue());
    }
    return canAssignToFrom(to.getType(), from.getType(), variableInit);
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
    return canAssignToFrom(to,from,false);
  }

  static public boolean canAssignToFrom(
    @Nullable SpecificTypeReference to,
    @Nullable SpecificTypeReference from,
    boolean variableInit
  ) {
    if (to == null || from == null) return false;
    if (to.isDynamic() || from.isDynamic()) return true;
    if (to.isAny()) return true;

    if (isFunctionTypeOrReference(to) && isFunctionTypeOrReference(from)) {
      SpecificFunctionReference toRef = asFunctionReference(to);
      SpecificFunctionReference fromRef = asFunctionReference(from);
      return canAssignToFromFunction(toRef, fromRef);
    }

    if (to instanceof SpecificHaxeClassReference && from instanceof SpecificHaxeClassReference) {
      return canAssignToFromType((SpecificHaxeClassReference)to, (SpecificHaxeClassReference)from, variableInit);
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
    return to.returnValue != null && (to.returnValue.isVoid() || to.returnValue.canAssign(from.returnValue));
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
    return canAssignToFromType(to,from, false);
  }

  static private boolean canAssignToFromType(
    @NotNull SpecificHaxeClassReference to,
    @NotNull SpecificHaxeClassReference from,
    boolean variableInit
  ) {

    // Null<T> is a special case.  It must act like a T in all ways.  Whereas,
    // any other abstract must act like it hides its internal types.
    to = getUnderlyingClassIfAbstractNull(to);
    from = getUnderlyingClassIfAbstractNull(from);

    if(to.isCoreType() || from.isCoreType() && to.getHaxeClass() != null) {
      String toName = to.getHaxeClass().getName();
      switch (toName) {
        case "EnumValue":
          return handleEnumValue(to,from);
        case "Class":
          return handleClassType(to,from);
        case "Enum":
          return handleEnumType(to,from);
      }
    }

    if (canAssignToFromSpecificType(to, from, variableInit)) return true;

    Set<SpecificHaxeClassReference> compatibleTypes = to.getCompatibleTypes(SpecificHaxeClassReference.Compatibility.ASSIGNABLE_FROM);
    for (SpecificHaxeClassReference compatibleType : compatibleTypes) {
      if (canAssignToFromSpecificType(compatibleType, from, variableInit)) return true;
    }

    compatibleTypes = from.getCompatibleTypes(SpecificHaxeClassReference.Compatibility.ASSIGNABLE_TO);
    for (SpecificHaxeClassReference compatibleType : compatibleTypes) {
      if (canAssignToFromSpecificType(to, compatibleType, variableInit)) return true;
    }

    compatibleTypes = from.getInferTypes();
    for (SpecificHaxeClassReference compatibleType : compatibleTypes) {
      if (canAssignToFromSpecificType(to, compatibleType, variableInit)) return true;
    }

    // Last ditch effort...
    return to.toStringWithoutConstant().equals(from.toStringWithoutConstant());
  }

  private static boolean handleEnumValue(SpecificHaxeClassReference to, SpecificHaxeClassReference from) {
    return from.isContextAnEnumDeclaration();
    //if(!from.isContextAnEnumDeclaration()) return false;
    //return canAssignToFromType(to, from);
  }
  private static boolean handleClassType(SpecificHaxeClassReference to, SpecificHaxeClassReference from) {
    if(to.getHaxeClass().equals(from.getHaxeClass())) return true;
    if(!from.isContextAType() || from.isContextAnEnumType()) return false;
    SpecificHaxeClassReference typeParameter = Objects.requireNonNull(to.getSpecifics()[0].getClassType());
    return canAssignToFromType(typeParameter, from);
  }
  private static boolean handleEnumType(SpecificHaxeClassReference to, SpecificHaxeClassReference from) {
    if(to.getHaxeClass().equals(from.getHaxeClass())) return true;
    if(!from.isContextAnEnumType()) return false;
    SpecificHaxeClassReference typeParameter = Objects.requireNonNull(to.getSpecifics()[0].getClassType());
    return canAssignToFromType(typeParameter, from);
  }


  static private boolean canAssignToFromSpecificType(
    @NotNull SpecificHaxeClassReference to,
    @NotNull SpecificHaxeClassReference from,
    boolean variableInit
  ) {
    if (to.isDynamic() || from.isDynamic()) {
      return true;
    }

    if (from.isUnknown()) {
      return true;
    }

    if (to.getHaxeClassReference().refersToSameClass(from.getHaxeClassReference())) {
      if (to.getSpecifics().length == from.getSpecifics().length) {
        int specificsLength = to.getSpecifics().length;
        for (int n = 0; n < specificsLength; n++) {

          ResultHolder toHolder = to.getSpecifics()[n];
          ResultHolder fromHolder = from.getSpecifics()[n];

          SpecificHaxeClassReference toSpecific = toHolder.getClassType();
          SpecificHaxeClassReference fromSpecific = fromHolder.getClassType();

          if (toSpecific != null && !toSpecific.isCoreType()) {
            toSpecific = getStdClass(toSpecific.isEnumType() ? "Enum" : "Class", to.context, new ResultHolder[]{toHolder});
          }
          if (fromSpecific != null && !fromSpecific.isCoreType()) {
            fromSpecific = getStdClass(fromSpecific.isEnumType() ? "Enum" : "Class", from.context, new ResultHolder[]{fromHolder});
          }

          if (!canAssignToFrom(toSpecific, fromSpecific, variableInit)) {
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
