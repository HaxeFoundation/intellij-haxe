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
import com.intellij.plugins.haxe.model.HaxeGenericParamModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.intellij.plugins.haxe.model.type.UnificationRules.UNIFY_NULL;

public class HaxeTypeUnifier {
  @NotNull
  static public ResultHolder unify(ResultHolder a, ResultHolder b) {
    return unify(a, b, UnificationRules.DEFAULT);
  }
  static public ResultHolder unify(ResultHolder a, ResultHolder b,  @NotNull  UnificationRules rules) {
    return unify(a.getType(), b.getType(), a.getType().context, rules).createHolder();
  }

  @NotNull
  static public SpecificTypeReference unify(SpecificTypeReference a, SpecificTypeReference b, @NotNull PsiElement context) {
    return unify(a,b, context, null,  UnificationRules.DEFAULT);
  }
  static public SpecificTypeReference unify(SpecificTypeReference a, SpecificTypeReference b, @NotNull PsiElement context, @NotNull  UnificationRules rules) {
    return unify(a,b, context, null, rules);
  }
  @NotNull
  static public SpecificTypeReference unify(SpecificTypeReference a, SpecificTypeReference b, @NotNull PsiElement context, @Nullable  SpecificTypeReference suggestedType, @NotNull  UnificationRules rules) {
    if (a == null && b == null) return SpecificTypeReference.getUnknown(context);
    if (a == null) return b;
    if (b == null) return a;

    // Completely equals!
    if (a.toStringWithoutConstant().equals(b.toStringWithoutConstant())) {
      return a.withoutConstantValue();
    }
    // if dynamic and the result is not from constant (ex constant = null) use dynamic
    if (a.isDynamic() && a.getConstant() == null) return a;
    if (b.isDynamic() && b.getConstant() == null) return b;


    // Using UnificationRules to make sure we only unify when assigned and not other cases like for instance
    // `var x = 1 + null`, if we unify null here we would get Null<Int> but this expression should not be allowed
    if(rules == UNIFY_NULL) {
      if (constantIsNullValue(a) && !b.isUnknown() && !b.isNullType()) {
        return b.wrapInNullType(a.context);
      }
      if (constantIsNullValue(b) && !a.isUnknown() && !a.isNullType()) {
        return a.wrapInNullType(b.context);
      }
    }
    if ((a.isDynamic() || a.isExpr()) && constantIsNullValue(a) && !b.isUnknown()) return b;
    if ((b.isDynamic() || b.isExpr()) && constantIsNullValue(b) && !a.isUnknown()) return a;

    boolean isNullWrapped = a.isNullType() || b.isNullType();
    SpecificTypeReference referenceA = a.isNullType() ? ((SpecificHaxeClassReference)a).unwrapNullType() : a;
    SpecificTypeReference referenceB = b.isNullType() ? ((SpecificHaxeClassReference)b).unwrapNullType() : b;

    if (referenceA instanceof SpecificHaxeClassReference classReferenceA && referenceB instanceof SpecificHaxeClassReference classReferenceB) {

      if (suggestedType  instanceof SpecificHaxeClassReference suggestedClassReference) {
        SpecificTypeReference reference = unifyTypes(suggestedClassReference, classReferenceA, context, rules);
        reference = unifyTypes((SpecificHaxeClassReference)reference, classReferenceB, context, rules);
        if (!reference.isUnknown()) {
          return reference;
        }
      }
      SpecificTypeReference reference = unifyTypes(classReferenceA, classReferenceB, context, rules);
      return isNullWrapped?  reference.wrapInNullType(a.isNullType() ? a.context :  b.context) : reference;
    }
    if (a instanceof SpecificFunctionReference && b instanceof SpecificFunctionReference) {
      // TODO suggested type support for functions
      return unifyFunctions((SpecificFunctionReference)a, (SpecificFunctionReference)b, context);
    }
    if (a.isEnumValue() && b.isEnumValue()) {
      return unifyEnumValues(a, b);
    }
    else if (a.isEnumType() && b.isEnumValue()) {
      return unifyEnumValues(a, b);
    }
    SpecificTypeReference commonEnumClass = tryToFindCommonEnumClass(a, b, context);
    if (commonEnumClass != null) return commonEnumClass;

    return SpecificTypeReference.getUnknown(context);
  }

  private static boolean constantIsNullValue(SpecificTypeReference b) {
    return b.getConstant() instanceof HaxeNull;
  }

  private static @Nullable SpecificTypeReference tryToFindCommonEnumClass(SpecificTypeReference a,
                                                              SpecificTypeReference b,
                                                              @NotNull PsiElement context) {

    SpecificTypeReference possibleAChange = a;
    SpecificTypeReference possibleBChange = b;
    if (a instanceof  SpecificEnumValueReference valueReference) {
      possibleAChange = valueReference.getEnumClass();
    }
    if (b instanceof  SpecificEnumValueReference valueReference) {
      possibleBChange = valueReference.getEnumClass();
    }
    if (possibleAChange != a || possibleBChange != b) {
      return unify(possibleAChange, possibleBChange, context, UnificationRules.DEFAULT);
    }
    return null;
  }

  @NotNull
  static public SpecificTypeReference unifyFunctions(SpecificFunctionReference a,
                                                     SpecificFunctionReference b,
                                                     @NotNull PsiElement context) {
    final List<HaxeArgument> pa = a.getArguments();
    final List<HaxeArgument> pb = b.getArguments();
    if (pa.size() != pb.size()) return SpecificTypeReference.getInvalid(a.getElementContext());
    final ArrayList<HaxeArgument> arguments = new ArrayList<>();

    int size = pa.size();
    for (int n = 0; n < size; n++) {
      //final Argument unifiedArgument = unify(pa.get(n), pb.get(n), UnificationRules.IGNORE_VOID);
      final HaxeArgument unifiedArgument = unify(pa.get(n), pb.get(n), UnificationRules.DEFAULT);
      if (unifiedArgument.isInvalid()) return SpecificTypeReference.getInvalid(a.getElementContext());
      arguments.add(unifiedArgument);
    }
    final ResultHolder returnValue = unify(a.getReturnType(), b.getReturnType(), UnificationRules.PREFER_VOID);
    //final ResultHolder returnValue = unify(a.getReturnType(), b.getReturnType());
    return new SpecificFunctionReference(arguments, returnValue, (HaxeMethodModel)null, context);
  }

  @NotNull
  private static HaxeArgument unify(HaxeArgument a, HaxeArgument b, @NotNull UnificationRules rules) {
    if (a.isOptional() != b.isOptional()) {
      ResultHolder invalidType = SpecificTypeReference.getInvalid(a.getType().getElementContext()).createHolder();
      return new HaxeArgument(a.getElement(), a.getIndex(), a.isOptional(), false, invalidType, a.getName());
    }

    return new HaxeArgument(a.getElement(),a.getIndex(), a.isOptional(), false, unify(a.getType(), b.getType(), rules), a.getName());
  }

  @NotNull
  static public SpecificTypeReference unifyTypes(SpecificHaxeClassReference a, SpecificHaxeClassReference b, @NotNull PsiElement context, @NotNull UnificationRules rules) {
    if (a.isDynamic()) return a.withoutConstantValue();
    if (b.isDynamic()) return b.withoutConstantValue();

    HaxeClassModel modelA = a.getHaxeClassModel();
    if (modelA == null) return SpecificTypeReference.getDynamic(context);
    HaxeClassModel modelB = b.getHaxeClassModel();
    if (modelB == null) return SpecificTypeReference.getDynamic(context);

    // if not same type but typedef, resolve typdefs and try to unify real type
    if (!a.isSameType(b) && (a.isTypeDef() || b.isTypeDef())) {
      SpecificTypeReference referenceA = a;
      SpecificTypeReference referenceB = b;
      if (a.isTypeDef()) {
        referenceA = a.fullyResolveTypeDefReference();
      }
      if (b.isTypeDef()) {
        referenceB = b.fullyResolveTypeDefReference();
      }
      if (referenceA != null && referenceB != null) {
        return unify(referenceA, referenceB, context, rules);
      }
    }

    @NotNull ResultHolder[] specificsA = a.getSpecifics();
    @NotNull ResultHolder[] specificsB = b.getSpecifics();
    if (a.isSameType(b)) {
      if (specificsA.length == 0 && specificsB.length == 0) {
        return a;
      }
    }

    SpecificHaxeClassReference unifiedAnonymousType = unifyIfContainsAnonymousType(a, b);
    if (unifiedAnonymousType != null) {
      return unifiedAnonymousType;
    }
    // type parameter constraints check
    if (modelA instanceof HaxeGenericParamModel genericParamModel) {
      if (genericParamModel.hasConstraint()) {
        ResultHolder constraint = genericParamModel.getConstraint(null);
        if (constraint!= null && constraint.canAssign(b.createHolder())) return b;
      }
    }

    final Set<HaxeClassModel> atypes = modelA.getCompatibleTypes();
    final Set<HaxeClassModel> btypes = modelB.getCompatibleTypes();
    // @TODO: this could be really slow, hotspot for optimizing

    for (HaxeClassModel type : atypes) {
      if (btypes.contains(type)) {
        // @TODO: generics
        if (specificsA.length == 0 && specificsB.length == 0) {
          return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(type, context));
        }else{
          return SpecificHaxeClassReference.withGenerics(new HaxeClassReference(type, context), unifySpecifics(specificsA, specificsB, context));
        }
      }
    }
    if (rules == UnificationRules.IGNORE_VOID) {
      if (a.isVoid()) return b;
      if (b.isVoid()) return a;
    }
    else if (rules == UnificationRules.PREFER_VOID) {
      if (a.isVoid()) return a;
      if (b.isVoid()) return b;
    }
    else if (rules == UnificationRules.DEFAULT) {
      if (a.isVoid() && !b.isVoid()) {
        return b;
      }
      else if (!a.isVoid() && b.isVoid()) {
        return a;
      }
    }
    // @TODO: Do a proper unification
    return SpecificTypeReference.getUnknown(a.getElementContext());
  }

  @Nullable
  private static SpecificHaxeClassReference unifyIfContainsAnonymousType(SpecificHaxeClassReference a, SpecificHaxeClassReference b) {
    if (a.isAnonymousType()) {
      if (a.canAssign(b)) {
        return a;
      }
    }
    if (b.isAnonymousType()) {
      if (b.canAssign(a)) {
        return b;
      }
    }
    return null;
  }

  private static ResultHolder[] unifySpecifics(@NotNull  ResultHolder[] specificsForA, @NotNull  ResultHolder[] specificsForB,  @NotNull PsiElement context) {
    // TODO might have to resolve typedefs to get correct amount here
    int size = Math.min(specificsForA.length, specificsForB.length);
    ResultHolder[] unified = new ResultHolder[size];
    for (int i = 0; i < size; i++) {
      ResultHolder holderA = specificsForA[i];
      ResultHolder holderB = specificsForB[i];
      if (holderA.getClassType() != null && holderB.getClassType() != null) {
        // the sum of multiple results with incomplete may make a complete specific list
        // so we replace unknowns with knowns when possible (typical for generic enums)
        if (holderA.getClassType().isUnknown() && !holderB.getClassType().isUnknown()) {
          unified[i] = holderB;
        } else if (!holderA.getClassType().isUnknown() && holderB.getClassType().isUnknown()) {
          unified[i] = holderA;
        } else {
          SpecificTypeReference type = unifyTypes(holderA.getClassType(), holderB.getClassType(), context, UnificationRules.DEFAULT);
          // if we cant unify specifics then Dynamic should be used
          if (type.isUnknown()) type = SpecificTypeReference.getDynamic(context);
          unified[i] = new ResultHolder(type);
        }
      } else if (holderA.getFunctionType() != null && holderB.getFunctionType() != null) {
        // TODO compare  functions and see if they are the same signature
        unified[i] = new ResultHolder(SpecificTypeReference.getDynamic(context));
      } else {
        // use dynamic if we can match in any way
        unified[i] = new ResultHolder(SpecificTypeReference.getDynamic(context));
    }
  }

    return unified;
  }

  @NotNull
  static public SpecificTypeReference unifyEnumValues(SpecificTypeReference a, SpecificTypeReference b) {
    if (a.isEnumValueClass()) {
      if (b.isEnumValueClass()) {
        return a;
      }
      else if (b instanceof SpecificEnumValueReference enumValueReference) {
        return enumValueReference.clone();
      }
    }
    else if (a instanceof SpecificHaxeClassReference && b instanceof SpecificEnumValueReference bReference) {
      if(a.isSameType(bReference.getEnumClass())) {
        return a;
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
          return unifyTypes(aclass, bclass, a.getElementContext(), UnificationRules.DEFAULT);
        }
      }
    }
    return SpecificTypeReference.getDynamic(a.getElementContext());
  }

  @NotNull
  static public SpecificTypeReference unify(SpecificTypeReference[] types, @NotNull PsiElement context) {
    return unify(Arrays.asList(types), context,  UnificationRules.DEFAULT);
  }
  @NotNull
  static public SpecificTypeReference unify(SpecificTypeReference[] types, @NotNull PsiElement context, @NotNull  UnificationRules rules) {
    return unify(Arrays.asList(types), context, rules);
  }

  @NotNull
  static public SpecificTypeReference unify(List<SpecificTypeReference> types, @NotNull PsiElement context, @NotNull  UnificationRules rules) {
    return unify(types, context, null, UnificationRules.DEFAULT);
  }
  @NotNull
  static public SpecificTypeReference unify(List<SpecificTypeReference> types, @NotNull PsiElement context, @Nullable  SpecificTypeReference suggestedType, @NotNull  UnificationRules rules) {
    if (types.isEmpty()) {
      return SpecificTypeReference.getUnknown(context);
    }
    SpecificTypeReference type = types.get(0);
    for (int n = 1; n < types.size(); n++) {
      type = unify(type, types.get(n), context, suggestedType, rules);
    }
    return type;
  }

  @NotNull
  static public ResultHolder unifyHolders(List<ResultHolder> typeHolders, @NotNull PsiElement context, @NotNull  UnificationRules rules) {
    // @TODO: This should mutate unknown holders?
    return unify(ResultHolder.types(typeHolders), context, rules).createHolder();
  }
}
