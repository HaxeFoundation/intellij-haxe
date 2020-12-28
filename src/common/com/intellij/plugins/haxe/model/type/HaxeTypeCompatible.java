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

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeSpecificFunction;
import com.intellij.psi.PsiElement;
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
    return canAssignToFrom(to.getType(), from.getType(), true);
  }

  static private boolean isFunctionTypeOrReference(SpecificTypeReference ref) {
    return ref instanceof SpecificFunctionReference || ref.isFunction() || isTypeDefFunction(ref);
  }
  static private boolean isTypeDefFunction(SpecificTypeReference ref ) {
    if (ref instanceof  SpecificHaxeClassReference) {
      SpecificHaxeClassReference classReference = (SpecificHaxeClassReference) ref;
      return classReference.isTypeDefOfFunction();
    }
    return false;
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
    if (isTypeDefFunction(ref)) {
        SpecificHaxeClassReference classReference = (SpecificHaxeClassReference) ref;
        if(classReference.isTypeDefOfFunction())  return classReference.resolveTypeDefFunction();
    }
    return null;  // XXX: Should throw exception instead??
  }

  static public boolean canAssignToFrom(
    @Nullable SpecificTypeReference to,
    @Nullable SpecificTypeReference from) {
    return canAssignToFrom(to,from, true);
  }

  static public boolean canAssignToFrom(
    @Nullable SpecificTypeReference to,
    @Nullable SpecificTypeReference from,
    Boolean transitive
  ) {
    if (to == null || from == null) return false;
    if (to.isDynamic() || from.isDynamic()) return true;

    if (isFunctionTypeOrReference(to) && isFunctionTypeOrReference(from)) {
      SpecificFunctionReference toRef = asFunctionReference(to);
      SpecificFunctionReference fromRef = asFunctionReference(from);
      return canAssignToFromFunction(toRef, fromRef);
    }

    if (to instanceof SpecificHaxeClassReference && from instanceof SpecificHaxeClassReference) {
      return canAssignToFromType((SpecificHaxeClassReference)to, (SpecificHaxeClassReference)from, transitive) ;
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
        if (!to.arguments.get(n).canAssignToFrom(from.arguments.get(n))) return false;
      }
    }
    // Void return on the to just means that the value isn't used/cared about. See
    // the Haxe manual, section 3.5.4 at https://haxe.org/manual/type-system-unification-function-return.html
    return to.returnValue != null && (to.returnValue.isVoid() || to.returnValue.canAssign(from.returnValue));
  }



  static public SpecificHaxeClassReference getUnderlyingClassIfAbstractNull(SpecificHaxeClassReference ref) {
    if (ref.isAbstract() && "Null".equals(ref.getClassName())) {
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
    return canAssignToFromType(to,from, true);
  }

  static private boolean canAssignToFromType(
    @NotNull SpecificHaxeClassReference to,
    @NotNull SpecificHaxeClassReference from,
    Boolean transitive
  ) {

    // Null<T> is a special case.  It must act like a T in all ways.  Whereas,
    // any other abstract must act like it hides its internal types.
    to = getUnderlyingClassIfAbstractNull(to);
    from = getUnderlyingClassIfAbstractNull(from);

    // getting underlying type  makes it easier to determine behaviors like
    // Implicit cast for Abstracts etc.

    if(to.isTypeDefOfClass())to = to.resolveTypeDefClass();
    if(from.isTypeDefOfClass())from = from.resolveTypeDefClass();

    // check if type is one of the core types that needs custom logic
    if(to.isCoreType() || from.isCoreType() && to.getHaxeClass() != null) {
      String toName = to.getHaxeClass().getName();
      switch (toName) {
        case SpecificTypeReference.ENUM_VALUE:
          return handleEnumValue(to,from);
        case SpecificTypeReference.CLASS:
          return handleClassType(to,from);
        case SpecificTypeReference.ENUM:
          return handleEnumType(to,from);
      }
    }

    if (canAssignToFromSpecificType(to, from)) return true;

    Set<SpecificHaxeClassReference> compatibleTypes = to.getCompatibleTypes(SpecificHaxeClassReference.Compatibility.ASSIGNABLE_FROM);
    if (to.isAbstract() && transitive) compatibleTypes.addAll(to.getHaxeClassModel().getImplicitCastTypesList(to));
    for (SpecificHaxeClassReference compatibleType : compatibleTypes) {
      if (canAssignToFromSpecificType(compatibleType, from)) return true;
    }

    compatibleTypes = from.getCompatibleTypes(SpecificHaxeClassReference.Compatibility.ASSIGNABLE_TO);
    if (from.isAbstract() && transitive) compatibleTypes.addAll(from.getHaxeClassModel().getCastableToTypesList(from));
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

  private static boolean handleEnumValue(SpecificHaxeClassReference to, SpecificHaxeClassReference from) {
    if(to.getHaxeClassReference().refersToSameClass(from.getHaxeClassReference())) return true;
    if(from.isEnumClass()) return false;
    return (from.isEnumType() && !from.isContextAType())|| from.isContextAnEnumDeclaration();
  }
  private static boolean handleClassType(SpecificHaxeClassReference to, SpecificHaxeClassReference from) {
    ResultHolder[] specificsTo = to.getSpecifics();
    ResultHolder[] specificsFrom = from.getSpecifics();

    if(to.getHaxeClass().equals(from.getHaxeClass())) {
      //Class type can only have 1 Spesific (Class<T>) so we only check the first one
      HaxeClassReference toReference = specificsTo.length > 0 ? specificsTo[0].getClassType().getHaxeClassReference() : null;
      HaxeClassReference fromReference = specificsFrom.length > 0 ? specificsFrom[0].getClassType().getHaxeClassReference() : null;
      if(toReference != null && fromReference != null)
      if (toReference.refersToSameClass(fromReference)) return true;
    }

    if(!from.isContextAType() || from.isContextAnEnumType()) return false;
    if(specificsTo.length == 0) return false;
    SpecificHaxeClassReference typeParameter = Objects.requireNonNull(specificsTo[0].getClassType());
    // The compiler does not accept types to be assigned to Class<Any> without being casted
    // This is probably due to the abstract implicit cast methods only accepting and returning instances.
    if(typeParameter.isAny() && !from.isAny()) return false;
    return canAssignToFromType(typeParameter, from);
  }
  private static boolean handleEnumType(SpecificHaxeClassReference to, SpecificHaxeClassReference from) {
    if(to.getHaxeClass().equals(from.getHaxeClass())) return true;
    if(!from.isContextAnEnumType()) return false;
    if(from.isEnumValueClass()) return false;
    SpecificHaxeClassReference typeParameter = Objects.requireNonNull(to.getSpecifics()[0].getClassType());
    return canAssignToFromType(typeParameter, from);
  }

  static public boolean canAssignToFromSpecificType(
    @NotNull SpecificHaxeClassReference to,
    @NotNull SpecificHaxeClassReference from
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
          if(toHolder.equals(fromHolder)) continue;

          if (typeCanBeWrapped(toHolder) && typeCanBeWrapped(fromHolder)) {
            SpecificHaxeClassReference toSpecific = toHolder.getClassType();
            SpecificHaxeClassReference fromSpecific = fromHolder.getClassType();

            if(toSpecific != null && !toSpecific.isCoreType()) {
              SpecificHaxeClassReference specific = wrapType(toHolder, to.context, toSpecific.isEnumType());
              // recursive protection
              if(referencesAreDifferent(to, specific)) toSpecific = specific;
            }
            if(fromSpecific != null && !fromSpecific.isCoreType()) {
              SpecificHaxeClassReference specific = wrapType(fromHolder, from.context, fromSpecific.isEnumType());
              // recursive protection
              if(referencesAreDifferent(from, specific)) fromSpecific = specific;
            }

            if (!canAssignToFrom(toSpecific, fromSpecific)) {
                //HACK make sure we can assign collection literals / init expressions to types with with EnumValue specific
                  if(toSpecific != null  && fromSpecific!= null && (from.isLiteralMap() || from.isLiteralArray())) {
                  if (toSpecific.isEnumValueClass() && fromSpecific.isEnumClass()) return true;
                  if (fromSpecific.isEnumValueClass() && toSpecific.isEnumClass()) return true;
                }
              return false;
            }
          } else {
            if (!canAssignToFrom(toHolder, fromHolder)) return false;
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

  //used to help prevent stack overflows in canAssignToFromSpecificType
  private static boolean referencesAreDifferent(@NotNull SpecificHaxeClassReference first, @NotNull SpecificHaxeClassReference second) {
    return !first.toPresentationString().equalsIgnoreCase(second.toPresentationString());
  }

  @NotNull
  public static SpecificHaxeClassReference wrapType(@NotNull ResultHolder type, @NotNull PsiElement context, boolean useEnum) {
    return getStdClass(useEnum ? "Enum" : "Class", context, new ResultHolder[]{type});
  }

  // We only want to wrap "real" types in Class<T>, ex Class<String>
  // Other combinations makes little to no sense ( Class<Null> or Class<int->int> etc. )
  private static boolean typeCanBeWrapped(ResultHolder holder) {
    return holder.isClassType()
           && !holder.isFunctionType()
           && !holder.isUnknown()
           && !holder.isVoid()
           && !holder.isDynamic();
  }
}
