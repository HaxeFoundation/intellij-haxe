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

import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.MULTI_TYPE;
import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.TRANSITIVE;
import static com.intellij.plugins.haxe.model.type.SpecificTypeReference.getStdClass;

@CustomLog
public class HaxeTypeCompatible {

  // disabling for now, too many mismatches (seems to struggle with resolving correct enum types)
  static boolean checkAnonymousMemberTypes = true;

  static public boolean canAssignToFrom(@Nullable SpecificTypeReference to, @Nullable ResultHolder from) {
    if (null == to || null == from) return false;
    return canAssignToFrom(to, from.getType());
  }

  static public boolean canAssignToFrom(@Nullable ResultHolder to, @Nullable ResultHolder from) {
    return  canAssignToFrom(to, from, null);
  }
  static public boolean canAssignToFrom(@Nullable ResultHolder to, @Nullable ResultHolder from, HaxeAssignContext  context) {
    if (null == to || null == from) return false;
    if (to.isUnknown()) {
      return true;
    }
    else if (from.isUnknown()) {
      return true;
    }
    if (context == null) {
      context = new HaxeAssignContext(to.getOrigin(),  from.getOrigin());
    }
    return canAssignToFrom(to.getType(), from.getType(), true,  context);
  }


  static private boolean isFunctionTypeOrReference(SpecificTypeReference ref) {
    return ref instanceof SpecificFunctionReference
           || ref.isFunction()
           || isTypeDefFunction(ref)
           || isAbstractAssignableToFunction(ref)
           || isEnumValueConstructor(ref);
  }



  private static boolean isAbstractAssignableToFunction(SpecificTypeReference ref) {
    if (ref instanceof  SpecificHaxeClassReference classReference) {
      if (classReference.isAbstractType()) {
        Set<SpecificHaxeClassReference> types = classReference.getCompatibleTypes(SpecificHaxeClassReference.Compatibility.ASSIGNABLE_FROM);
        for (SpecificHaxeClassReference type : types) {
          if (type.isFunction()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  static private boolean isTypeDefFunction(SpecificTypeReference ref ) {
    if (ref instanceof  SpecificHaxeClassReference) {
      SpecificHaxeClassReference classReference = (SpecificHaxeClassReference) ref;
      return classReference.isTypeDefOfFunction();
    }
    return false;
  }

  static private SpecificFunctionReference asFunctionReference(SpecificTypeReference ref) {
    if (ref instanceof SpecificFunctionReference functionReference) return functionReference;

    if (ref.isFunction()) {
      HaxeClass classReference = ((SpecificHaxeClassReference)ref).getHaxeClass();
      if (classReference instanceof HaxeSpecificFunction func) {
        return SpecificFunctionReference.create(func);
      }
      // The Function class unifies with (can be assigned to by) any function.
      return new SpecificFunctionReference.StdFunctionReference(ref.getElementContext());
    }
    if (isTypeDefFunction(ref)) {
        SpecificHaxeClassReference classReference = (SpecificHaxeClassReference) ref;
        if(classReference.isTypeDefOfFunction())  return classReference.resolveTypeDefFunction();
    }
    if(isEnumValueConstructor(ref)) {
      return createEnumConstructorFunction(ref);
    }
    if (ref.isNullType() && ref instanceof  SpecificHaxeClassReference classReference) {
      @NotNull ResultHolder[] specifics = classReference.getSpecifics();
      if (specifics.length > 0 &&  specifics[0].isFunctionType()) return specifics[0].getFunctionType();
    }

    return null;  // XXX: Should throw exception instead??
  }

  private static SpecificFunctionReference createEnumConstructorFunction(SpecificTypeReference ref) {
    if (ref instanceof SpecificEnumValueReference enumValueReference) {
      return enumValueReference.getConstructor();
    }
    return null;
  }

  private static boolean isEnumValueConstructor(SpecificTypeReference ref) {
    if (ref instanceof SpecificEnumValueReference enumValueReference) {
      return enumValueReference.getModel() instanceof HaxeEnumValueConstructorModel;
    }
    return false;
  }

  static public boolean canAssignToFrom(
    @Nullable SpecificTypeReference to,
    @Nullable SpecificTypeReference from
  ) {
    return canAssignToFrom(to,from, true, null);
  }

  static public boolean canAssignToFrom(
    @Nullable SpecificTypeReference to,
    @Nullable SpecificTypeReference from,
    Boolean includeImplicitCast,
    @Nullable HaxeAssignContext context
  ) {
    if (to == null || from == null) return false;

    PsiElement fromOrigin = context == null ? null : context.fromOrigin;

    from = replaceMacroExprIfFromMacroMethod(from, fromOrigin);

    if (to instanceof  SpecificHaxeClassReference classReference) {
      if (classReference.isTypeDef() || classReference.isNullType()) {
        to = classReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
      }
    }
    if (from instanceof  SpecificHaxeClassReference classReference ) {
      if (classReference.isTypeDef() || classReference.isNullType()) {
        from = classReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
      }
    }
    // We allow any expression to be assign to and from Expr and treating it kinda like Dynamic as we currently dont have any real way of type compare.
    if (to.isExpr() || from.isExpr())return true;
    if (to.isAny()) return true;
    if (to.isDynamic() || from.isDynamic()) return true;
    if (to.isEnumValueClass() && isEnum(from)) return true;

    // if abstract of function is being compared to a function we map the abstract to its underlying function
    if (isFunctionTypeOrReference(from) && to.isAbstractType()) {
      SpecificFunctionReference fromFunctionType = asFunctionReference(from);
      if (hasAbstractFunctionTypeCast(to, true)) {
        List<SpecificFunctionReference> functionTypes = getAbstractFunctionTypes((SpecificHaxeClassReference)to, true);
        for (SpecificFunctionReference functionType : functionTypes) {
          if (canAssignToFromFunction(functionType, fromFunctionType)) return true;
        }
      }
      if (to instanceof  SpecificHaxeClassReference classReference) {
        List<SpecificTypeReference> list = getImplicitCast(classReference, true);
        for (SpecificTypeReference reference : list) {
          if (reference instanceof SpecificFunctionReference functionType) {
            if (canAssignToFromFunction(functionType, fromFunctionType)) return true;
          }
        }
      }

    }
    if (isFunctionTypeOrReference(to) && from.isAbstractType()) {
      SpecificFunctionReference toFunctionType = asFunctionReference(to); //may return null  (ex. classType of Function, or Null<Function>)
      if (hasAbstractFunctionTypeCast(from, false)) {
        List<SpecificFunctionReference> functionTypes = getAbstractFunctionTypes((SpecificHaxeClassReference)from, false);
        for (SpecificFunctionReference functionType : functionTypes) {
          if (toFunctionType == null) return true; // any function should be assignable to type Function
          if (canAssignToFromFunction(toFunctionType, functionType)) return true;
        }
      }
      if (from instanceof  SpecificHaxeClassReference classReference) {
        List<SpecificTypeReference> list = getImplicitCast(classReference, false);
        for (SpecificTypeReference reference : list) {
          if (reference instanceof  SpecificFunctionReference functionType) {
            if (toFunctionType == null) return true; // any function should be assignable to type Function
            if (canAssignToFromFunction(toFunctionType, functionType)) return true;
          }
        }
      }
    }

    if (isFunctionTypeOrReference(to) && isFunctionTypeOrReference(from)) {
      // if assignable to Function(Dynamic) class (@:callable) any function pointer should be allowed
      // TODO mlo: figure out the best way to handle classReferences with @:callable
      if (isAbstractAssignableToFunction(to)) {
        return true;
      }else {
        SpecificFunctionReference toRef = asFunctionReference(to);
        SpecificFunctionReference fromRef = asFunctionReference(from);
        return canAssignToFromFunction(toRef, fromRef);
      }
    }
    // check if we try to assign enum value to a reference with (it's) enum class as type
    if (to instanceof SpecificHaxeClassReference classReference) {
      if (classReference.isEnumType() || classReference.isEnumValueClass() || isUnderlyingTypeEnum(classReference)) {
        if (from instanceof SpecificEnumValueReference valueReference) {
          if (classReference.isEnumValueClass()) return true; // all enum values can be assigned to EnumValueClass type;
          return canAssignToFromType(classReference, valueReference.getEnumClass());
        }
      } else {
        //NOTE: special case for abstract enums
        // check if  from value is same type as  abstract enums underlying type and allow assign if done inside abstract enum definition
        HaxeClass haxeClass = classReference.getHaxeClass();
        if (haxeClass != null && haxeClass.isEnum() && haxeClass.isAbstractType()) {
          if (from instanceof SpecificHaxeClassReference reference) {
            PsiElement commonParent = PsiTreeUtil.findCommonParent(reference.context, haxeClass);
            if (commonParent == haxeClass) {
              SpecificTypeReference underlyingType = haxeClass.getModel().getUnderlyingType();
              boolean canAssignToUnderlying = canAssignToFrom(underlyingType, from);
              // we only return true when allowed,  we continue checks below if not as the assign might be Enum to Enum (and not underlying to enum)
              if (canAssignToUnderlying) return true;
            }
          }
        }
      }
    }

    if (to instanceof SpecificHaxeClassReference toClassReference && from instanceof SpecificHaxeClassReference fromClassReference) {
      return canAssignToFromType(toClassReference, fromClassReference, includeImplicitCast, context) ;
    }

    if (to instanceof SpecificEnumValueReference toReference && from instanceof SpecificEnumValueReference fromReference) {
      return toReference.isSameType(fromReference);
    }
    // if value is type parameter and we have reached this point without resolving its type we just accept it as it would be the same as unknown
    if (to.isTypeParameter()) return true;


    return false;
  }

  private static boolean isUnderlyingTypeEnum(SpecificHaxeClassReference classReference) {
    HaxeClassModel model = classReference.getHaxeClassModel();
    if (model == null) return false;
    SpecificHaxeClassReference reference = model.getUnderlyingClassReference(classReference.getGenericResolver());
    if (reference == null) return false;
    return reference.isEnumType();
  }

  private static boolean isEnum(@Nullable SpecificTypeReference from) {
    if (from instanceof  SpecificHaxeClassReference classReference) {
      HaxeClassModel classModel = classReference.getHaxeClassModel();
      return classModel != null && classModel.isEnum();
    }
    return false;
  }

  private static SpecificTypeReference replaceMacroExprIfFromMacroMethod(@NotNull SpecificTypeReference from, @Nullable PsiElement fromOrigin) {
    if (from instanceof SpecificHaxeClassReference classReference) {
      if (fromOrigin instanceof HaxeMethodDeclaration methodDeclaration) {
          HaxeMethodModel model = methodDeclaration.getModel();
          if (model != null && model.isMacro() && HaxeMacroUtil.isMacroType(classReference)) {
            from = HaxeMacroUtil.resolveMacroType(classReference);
          }
        }
      }
    return from;
  }

  private static List<SpecificTypeReference> getImplicitCast(@NotNull SpecificHaxeClassReference classReference, boolean from) {

      HaxeAbstractTypeDeclaration abstractType = (HaxeAbstractTypeDeclaration)classReference.getHaxeClass();
      if (abstractType != null && abstractType.getModel() != null) {
        HaxeClassModel model = abstractType.getModel();
        return from
               ? model.getImplicitCastFromTypesList(classReference)
               : model.getImplicitCastToTypesList(classReference);
      }
    return List.of();
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
        HaxeArgument fromArg = from.arguments.get(n);
        HaxeArgument toArg = to.arguments.get(n);

        if (!toArg.getType().isUnknown() && !toArg.getType().isMissingClassModel()) {
          if (!toArg.canAssignToFrom(fromArg))
            return false;
        }
      }
    }
    // Void return on the to just means that the value isn't used/cared about. See
    // the Haxe manual, section 3.5.4 at https://haxe.org/manual/type-system-unification-function-return.html
    return to.returnValue == null || (to.returnValue.isVoid() || to.returnValue.canAssign(from.returnValue));
  }

  private static boolean hasAbstractFunctionTypeCast(SpecificTypeReference reference, boolean castFrom) {
    if (reference instanceof SpecificHaxeClassReference haxeClassReference) {
      HaxeClass haxeClass = haxeClassReference.getHaxeClass();

      if (haxeClass instanceof HaxeAbstractTypeDeclaration abstractTypeDeclaration) {
        if (castFrom && !abstractTypeDeclaration.getAbstractFromTypeList().isEmpty()) return true;
        if (!castFrom && !abstractTypeDeclaration.getAbstractToTypeList().isEmpty()) return true;

      }
    }
    return false;
  }

  @NotNull
  private static List<SpecificFunctionReference> getAbstractFunctionTypes(SpecificHaxeClassReference classReference, boolean getCastFrom) {
    if (!(classReference.getHaxeClass() instanceof HaxeAbstractTypeDeclaration abstractClass))  return Collections.emptyList();
    HaxeClassModel model = abstractClass.getModel();
    List<SpecificFunctionReference> list = new ArrayList<>();
    if(getCastFrom) {
      if (!abstractClass.getAbstractFromTypeList().isEmpty()) {
        for (HaxeAbstractFromType type : abstractClass.getAbstractFromTypeList()) {
          if (type.getFunctionType() != null) {
            HaxeSpecificFunction specificFunction =
              new HaxeSpecificFunction(type.getFunctionType(), classReference.getGenericResolver().getSpecialization(null));
            list.add(SpecificFunctionReference.create(specificFunction));
          }
          else {
            // check if typdef or abstracts can resolve to function type
            if (type.getTypeOrAnonymous() != null) {
              ResultHolder resultHolder = HaxeTypeResolver.getTypeFromTypeOrAnonymous(type.getTypeOrAnonymous());
              ResultHolder holder = HaxeTypeResolver.resolveParameterizedType(resultHolder, classReference.getGenericResolver());
              if (holder.isFunctionType()) {
                list.add(holder.getFunctionType());
              }
            }
          }
        }
      }
    }else {
      if (!abstractClass.getAbstractToTypeList().isEmpty()){
        for(HaxeAbstractToType type : abstractClass.getAbstractToTypeList()) {
          if (type.getFunctionType() != null) {
            HaxeSpecificFunction specificFunction =
              new HaxeSpecificFunction(type.getFunctionType(), classReference.getGenericResolver().getSpecialization(null));
            list.add(SpecificFunctionReference.create(specificFunction));
          }
          else if (type.getTypeOrAnonymous() != null) {
            // check if typeDef that needs to be resolved
            HaxeTypeOrAnonymous anonymous = type.getTypeOrAnonymous();
            HaxeType haxeType = anonymous.getType();
            if (haxeType != null) {
              HaxeGenericResolver resolver = classReference.getGenericResolver();
              PsiElement element = type.getOriginalElement();
              HaxeResolveResult result = HaxeResolveUtil.tryResolveType(haxeType, element, resolver.getSpecialization(element));
              if (result.isFunctionType()) {
                HaxeSpecificFunction specificFunction =
                  new HaxeSpecificFunction(result.getFunctionType(), classReference.getGenericResolver().getSpecialization(null));
                list.add(SpecificFunctionReference.create(specificFunction));
              }
              else if (result.isHaxeClass()) {
                HaxeClass aClass = result.getHaxeClass();
                if (aClass instanceof HaxeTypedefDeclaration typedef) {
                  // TODO should we traverse typedefs or do some kind of resolve type ?
                  // current code only checks first level
                  if (typedef.getFunctionType() != null) {
                    HaxeSpecificFunction specificFunction =
                      new HaxeSpecificFunction(typedef.getFunctionType(), classReference.getGenericResolver().getSpecialization(null));
                    list.add(SpecificFunctionReference.create(specificFunction));
                  }
                }
              }
            }
          }
        }
      }
    }
    return list;
  }



  static public SpecificHaxeClassReference getUnderlyingClassIfAbstractNull(SpecificHaxeClassReference ref) {
    if (ref.isAbstractType() && ref.isNullType()) {
      SpecificHaxeClassReference underlying = ref.getHaxeClassModel().getUnderlyingClassReference(ref.getGenericResolver());
      if (null != underlying) {
        ref = underlying;
      }
    }
    return ref;
  }
  static public SpecificFunctionReference getUnderlyingFunctionIfAbstractNull(SpecificHaxeClassReference ref) {
    if (ref.isAbstractType() && ref.isNullType()) {
      SpecificFunctionReference underlying = ref.getHaxeClassModel().getUnderlyingFunctionReference(ref.getGenericResolver());
      if (null != underlying) {
        return underlying;
      }
    }
    return null;
  }

  static private boolean canAssignToFromType(
    @NotNull SpecificHaxeClassReference to,
    @NotNull SpecificHaxeClassReference from
  ) {
    return canAssignToFromType(to,from, true, null);
  }

  private static final RecursionGuard<HaxeClass> transitiveRecursionGuard = RecursionManager.createGuard("transitiveRecursionGuard");

  static private boolean canAssignToFromType(
    @NotNull SpecificHaxeClassReference to,
    @NotNull SpecificHaxeClassReference from,
    Boolean includeImplicitCast,
    @Nullable HaxeAssignContext context
  ) {

    PsiElement fromOrigin = context == null ? null : context.fromOrigin;

    // Null<T> is a special case.  It must act like a T in all ways.  Whereas,
    // any other abstract must act like it hides its internal types.
    to = getUnderlyingClassIfAbstractNull(to);
    from = getUnderlyingClassIfAbstractNull(from);

    // getting underlying type  makes it easier to determine behaviors like
    // Implicit cast for Abstracts etc.

    if(to.isTypeDefOfClass())to = to.resolveTypeDefClass();
    if(from.isTypeDefOfClass())from = from.resolveTypeDefClass();

    if (to == null || from == null) return false;
    // check if type is one of the core types that needs custom logic
    if(to.isCoreType() || from.isCoreType()) {
      HaxeClass haxeClass = to.getHaxeClass();
      if (haxeClass != null && haxeClass.getName() != null) {
        String toName = haxeClass.getName();
        switch (toName) {
          case SpecificTypeReference.ENUM_VALUE -> {
            return handleEnumValue(to, from, fromOrigin);
          }
          case SpecificTypeReference.CLASS -> {
            return handleClassType(to, from);
          }
          case SpecificTypeReference.ENUM -> {
            return handleEnumType(to, from);
          }
        }
      }
    }
    if(to.getHaxeClassModel() != null && !to.isTypeParameter()) { // typeParameters can be Anonymous but is not part of the anonymous "interface" check here
      HaxeClassModel classModel = to.getHaxeClassModel();
      if (classModel.isAnonymous() || classModel.isStructInit() || classModel.isObjectLiteral()){
        if (!from.isTypeParameter()) { // cant get members from a typeParameter
          // compare members (Anonymous stucts can be "used" as interface)
          if(containsAllMembers(to, from, context)) return true;
            if (classModel.isStructInit()) {
              return checkStructInitConstructor(to, from, context);
            }
        }
      }
    }

    boolean constraintCheck = context != null && context.isConstraintCheck();
    if (canAssignToFromSpecificType(to, from, constraintCheck)) return true;

    Set<SpecificHaxeClassReference> compatibleTypes = to.getCompatibleTypes(SpecificHaxeClassReference.Compatibility.ASSIGNABLE_FROM);
    if (to.isAbstractType() && includeImplicitCast) compatibleTypes.addAll(to.getHaxeClassModel().getImplicitCastFromTypesListClassOnly(to));

    boolean transitiveFrom = isTransitive(from);
    HaxeClassModel fromClassModel = from.getHaxeClassModel();
    List<SpecificHaxeClassReference> fromTransitiveTypes = fromClassModel == null ? List.of() : fromClassModel.getImplicitCastToTypesListClassOnly(from);

    for (SpecificHaxeClassReference compatibleType : compatibleTypes) {
      SpecificTypeReference compatibleTypeResolved = compatibleType.fullyResolveTypeDefAndUnwrapNullTypeReference();
      if (compatibleTypeResolved instanceof SpecificHaxeClassReference classReference) {
        if (canAssignToFromSpecificType(classReference, from)) return true;

        if (transitiveFrom && fromClassModel != null) {

          for (SpecificHaxeClassReference transitiveType : fromTransitiveTypes) {
            HaxeClass haxeClass = transitiveType.getHaxeClass();
            if (haxeClass != null) {
              Boolean canAssign =
                transitiveRecursionGuard.computePreventingRecursion(haxeClass, true, () -> canAssignToFromSpecificType(classReference, transitiveType));
              if (canAssign == Boolean.TRUE) return true;
            }
          }
        }
      }
    }

    compatibleTypes = from.getCompatibleTypes(SpecificHaxeClassReference.Compatibility.ASSIGNABLE_TO);
    if (from.isAbstractType()) {

      boolean isMultiType = from.getHaxeClass().hasCompileTimeMeta(MULTI_TYPE);
      if (isMultiType || includeImplicitCast) {
        if (fromClassModel != null) {
          List<SpecificHaxeClassReference> implicitCastList = fromClassModel.getImplicitCastToTypesListClassOnly(from);
          if (isMultiType) {
            if (canAssignMultiType(to, from, context, implicitCastList)) return true;
          }
          if (includeImplicitCast) {
            compatibleTypes.addAll(implicitCastList);
          }
        }
      }
    }
    for (SpecificHaxeClassReference compatibleType : compatibleTypes) {
      SpecificTypeReference compatibleTypeResolved = compatibleType.fullyResolveTypeDefAndUnwrapNullTypeReference();
      if (compatibleTypeResolved instanceof SpecificHaxeClassReference classReference) {
        if (canAssignToFromSpecificType(to, classReference)) return true;
      }
    }

    compatibleTypes = from.getInferTypes();
    for (SpecificHaxeClassReference compatibleType : compatibleTypes) {
      if (canAssignToFromSpecificType(to, compatibleType)) return true;
    }
    // if we don't know the type and don't have any constraints for Type parameters we just accept it for now
    // to avoid  wrong error annotations
    if (to.isTypeParameter()) {
      return true;
    }else if (from.isTypeParameter()) {
      return true;
    }

    // Last ditch effort...
    return to.toStringWithoutConstant().equals(from.toStringWithoutConstant());
  }

  private static boolean isTransitive(@NotNull SpecificHaxeClassReference from) {
    HaxeClass aClass = from.getHaxeClass();
    return aClass != null && aClass.hasCompileTimeMeta(TRANSITIVE);
  }

  private static boolean canAssignMultiType(@NotNull SpecificHaxeClassReference to,
                                   @NotNull SpecificHaxeClassReference from,
                                   @Nullable HaxeAssignContext context,
                                   List<SpecificHaxeClassReference> implicitCastList) {
    HaxeGenericResolver genericResolver = from.getGenericResolver();
    SpecificTypeReference underlyingType = from.getHaxeClassModel().getUnderlyingType();

    if (underlyingType instanceof SpecificHaxeClassReference underlyingClassReference) {
      HaxeClass underlyingClass = underlyingClassReference.getHaxeClass();
      HaxeGenericResolver underlyingResolver = genericResolver.translateFromTo(from.getHaxeClass(), underlyingClass);
      ResultHolder underlyingResult = underlyingResolver.resolve(underlyingType);

      if(underlyingResult != null && underlyingResult.getClassType() != null) {
        for (SpecificHaxeClassReference reference : implicitCastList) {
          HaxeClassReference classReference = reference.getHaxeClassReference();
          HaxeClass castClass = classReference.getHaxeClass();
          HaxeGenericResolver resolverForImplicitCast = underlyingResolver.translateFromTo(underlyingClass, castClass);

          @NotNull ResultHolder[] implicitCastSpecifics = resolverForImplicitCast.getSpecifics();
          SpecificHaxeClassReference multiTypeCast = SpecificHaxeClassReference.withGenerics(classReference, implicitCastSpecifics);
          if (canAssignToFrom(to, multiTypeCast, false, context)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static boolean checkStructInitConstructor(SpecificHaxeClassReference to, SpecificHaxeClassReference from,
                                                    @Nullable HaxeAssignContext context) {
    HaxeClassModel toModel = to.getHaxeClassModel();
    HaxeClassModel fromModel = from.getHaxeClassModel();
    if (toModel != null && fromModel != null && toModel.isStructInit() && fromModel.isObjectLiteral()) {

      // looks like haxe is mapping constructor parameter names to anonymous fields names.
      // if names mismatch we get "Object requires field" / "has extra field value"
      // note that parameters can be optional
      List<HaxeObjectLiteralMemberModel> fromMembers = fromModel.getAllMembers(from.getGenericResolver()).stream()
        .filter(model ->  model instanceof  HaxeObjectLiteralMemberModel)
        .map(HaxeObjectLiteralMemberModel.class::cast)
        .toList();
      HaxeGenericResolver toResolver = to.getGenericResolver();
      HaxeMethodModel constructor = toModel.getConstructor(toResolver);
      if (constructor == null) return false;
      boolean allMacteches = true;

      if (context != null ) {
        // remove errors from field check, we are going to add constructor errors if there are any
        context.clearErrors();
      }


      List<HaxeParameterModel> parameters = constructor.getParameters();
      for (HaxeParameterModel parameter : parameters) {
        String parameterName = parameter.getName();

        Optional<HaxeObjectLiteralMemberModel> fieldWithSameName = fromMembers.stream()
          .filter(m -> m.getNamePsi().getIdentifier().textMatches(parameterName))
          .findFirst();

        if (fieldWithSameName.isPresent()) {
          HaxeObjectLiteralMemberModel fromMemberModel = fieldWithSameName.get();
          ResultHolder fromType = ifEnumValueGetType(fromMemberModel.getResultType(toResolver));
          ResultHolder toType = ifEnumValueGetType(parameter.getType());
          ResultHolder resolved = toResolver.resolve(toType);
          if (resolved != null && !resolved.isUnknown())toType = resolved;
          if(!toType.canAssign(fromType)) {
            allMacteches = false;
            if (context != null) {
              context.addWrongTypeMember(fromMemberModel.getPresentableText(null),
                                         parameter.getPresentableText(toResolver),
                                         fromMemberModel.getNamePsi());
            }
          }
        } else {
          if (parameter.isOptional()) continue;
          allMacteches = false;
          if (context != null) {
            context.addMissingMember(parameterName);
          }
        }
      }
      return allMacteches;
    }
    return false;
  }

  private static ResultHolder ifEnumValueGetType(ResultHolder type) {
    if(type.isEnumValueType()) {
      SpecificEnumValueReference enumValueType = type.getEnumValueType();
      if(enumValueType != null) {
        return enumValueType.getEnumClass().createHolder();
      }
    }
    return type;

  }

  private static RecursionGuard<PsiElement> containsMembersRecursionGuard = RecursionManager.createGuard("containsMembersRecursionGuard");

  private static boolean containsAllMembers(SpecificHaxeClassReference to, SpecificHaxeClassReference from,
                                            @Nullable HaxeAssignContext context) {
    if (to.isTypeParameter() || from.isTypeParameter() ) return false; // unable to evaluate members when type is not resolved

    // if one of the types is a Class<T>  its was probably wrapped so we unwrap to T
    if (to.isClass() && to.getSpecifics().length == 1) to = to.getSpecifics()[0].getClassType();
    if (from.isClass()&& from.getSpecifics().length == 1) from = from.getSpecifics()[0].getClassType();

    if (to == null || from == null) return false;

    HaxeClassModel toClassModel = to.getHaxeClassModel();
    HaxeClassModel fromClassModel = from.getHaxeClassModel();

    // unable to determine, consider if we should return true or false in this case
    if (toClassModel == null ||  fromClassModel == null)
       return false;

    boolean isStruct = toClassModel.isStructInit();
    boolean isObjectLiteral = fromClassModel.isObjectLiteral();

    HaxeGenericResolver toResolver = to.getGenericResolver();
    HaxeGenericResolver fromResolver = from.getGenericResolver();
    List<HaxeBaseMemberModel> toMembers = toClassModel.getAllMembers(toResolver);
    List<HaxeBaseMemberModel> fromMembers = fromClassModel.getAllMembers(fromResolver);

    boolean allMembersMatches = true;
    for (HaxeBaseMemberModel toMember : toMembers) {
      String name = toMember.getName();
      // TODO  type check parameter and return type
      boolean memberExists = false;
      boolean optional = false;
      boolean ignored = false;
      if (toMember instanceof HaxeMethodModel methodModel){
        if(!isStruct) {
          // check for declarations in types
          memberExists = fromMembers.stream().filter(model -> model instanceof HaxeMethodModel)
            .map(model -> (HaxeMethodModel)model)
            .filter(mm -> methodModel.getParameters().size() == mm.getParameters().size())
            .anyMatch(model -> model.getNamePsi().textMatches(name));
          if (checkAnonymousMemberTypes) {
            if (!memberExists) {
              Optional<HaxeBaseMemberModel> first = fromMembers.stream().filter(model -> model.getNamePsi().getIdentifier().textMatches(name)).findFirst();
              if (first.isPresent()) {
                memberExists = true;
                if (checkAnonymousMemberTypes) {
                  HaxeBaseMemberModel fromMember = first.get();
                  if (context != null && context.getFromOrigin() != null) {
                    SpecificFunctionReference toType = methodModel.getFunctionType(toResolver);

                    ResultHolder fromType = containsMembersRecursionGuard.computePreventingRecursion(context.getFromOrigin(), false, () ->
                      fromMember.getResultType(isObjectLiteral ? toResolver : fromResolver)
                    );

                    if(toType == null ||  fromType == null) {
                      log.warn("Unable to evaluate fieldType compatibility");
                      return true;
                    }

                    if (!toType.canAssign(fromType)) {
                      String fromText = fromMember.getPresentableText(null, isObjectLiteral ? toResolver : fromResolver);
                      String toText = toMember.getPresentableText(null, toResolver);
                      context.addWrongTypeMember(fromText, toText, fromMember.getBasePsi());
                      allMembersMatches = false;
                    }
                  }
                }
              }
            }
          }
        }else {
          // ignore methods in @:structInit classes
          ignored = true;
        }
      }else if (toMember instanceof HaxeFieldModel fieldModel) {
        optional = fieldModel.isOptional() || fieldModel.hasInitializer();
        ignored = fieldModel.isStatic();

          Optional<HaxeBaseMemberModel> first = fromMembers.stream().filter(model -> model.getNamePsi().getIdentifier().textMatches(name)).findFirst();
          if (first.isPresent()) {
            memberExists = true;
            if (checkAnonymousMemberTypes) {
            HaxeBaseMemberModel fromMember = first.get();
            if (context != null && context.getFromOrigin() != null) {


              ResultHolder toType = containsMembersRecursionGuard.computePreventingRecursion(context.getFromOrigin(), false, () ->
                toMember.getResultType(toResolver)
              );
              ResultHolder fromType = containsMembersRecursionGuard.computePreventingRecursion(context.getFromOrigin(), false, () ->
                 fromMember.getResultType(isObjectLiteral ? toResolver : fromResolver)
              );

              if(toType == null ||  fromType == null) {
                log.warn("Unable to evaluate fieldType compatibility");
                return true;
              }

              if (!toType.canAssign(fromType)) {
                String fromText = fromMember.getPresentableText(null, isObjectLiteral ? toResolver : fromResolver);
                String toText = toMember.getPresentableText(null, toResolver);
                context.addWrongTypeMember(fromText, toText, fromMember.getBasePsi());
                allMembersMatches = false;
              }
            }
          }
        }
      }else  {
        memberExists = fromMembers.stream().anyMatch(model -> model.getNamePsi().getIdentifier().textMatches(name));
      }

      if (!ignored && !memberExists && !optional){
        String missingFieldName = toMember.getPresentableText(null, toResolver);
        if(context != null)context.addMissingMember(missingFieldName);
        allMembersMatches = false;
      }
    }

    // check object literals for too many fields
    if (isStruct && fromClassModel.isObjectLiteral()) {
      for (HaxeBaseMemberModel member : fromMembers) {
        if(toMembers.stream().noneMatch(model -> model.getNamePsi().getIdentifier().textMatches(member.getName()))) {
          //  too many fields
          return false;
        }
      }
    }


    return allMembersMatches;
  }

  private static boolean handleEnumValue(SpecificHaxeClassReference to, SpecificHaxeClassReference from, @Nullable PsiElement fromOrigin) {
    if(to.getHaxeClassReference().refersToSameClass(from.getHaxeClassReference())) return true;
    if (from.isTypeParameter()) return true; // if TypeParameter we do not know the real type( enumValue or not) so we just allow assign for now.
    if(from.isEnumClass()) return false;
    // assigning the "real" enum Type (as in just the name of the enum/class in code) to a variable of enum type  should not be allowed,
    // assigning value from  the result of a method call or anything else that has the type as part of a typeTag should be allowed
    // to be able to separate these 2 cases with the same SpecificHaxeClassReference we need to know the origin of the value
    // this is a hackish attempt at solving this without  breaking  other part of the code (type.context needs to be the correct class for generics to be resolved correclty)
    if (fromOrigin != null) {
      if (from.isContextAnEnumDeclaration()) return true;
      if (from.isEnumType()) {
        return fromOrigin instanceof HaxeParameter
               || fromOrigin instanceof HaxeMethod;
      }
      return false;
    } else {
      return (from.isEnumType() && !from.isContextAType() )|| from.isContextAnEnumDeclaration();
    }

  }
  private static boolean handleClassType(SpecificHaxeClassReference to, SpecificHaxeClassReference from) {
    ResultHolder[] specificsTo = to.getSpecifics();
    ResultHolder[] specificsFrom = from.getSpecifics();

    if(to.getHaxeClass().equals(from.getHaxeClass())) {
      //Class type can only have 1 Specific (Class<T>) so we only check the first one
      if(specificsTo.length == 1 && specificsFrom.length == 1) {
        SpecificHaxeClassReference specificToReference = specificsTo[0].getClassType();
        SpecificHaxeClassReference specificFromReference = specificsFrom[0].getClassType();

        if (specificToReference == null || specificFromReference == null) return false;

        HaxeClassReference toReference = specificToReference.getHaxeClassReference();
        HaxeClassReference fromReference = specificFromReference.getHaxeClassReference();

        if (toReference.refersToSameClass(fromReference)) return true;
        // if "target" has dynamic specific, anything from "source" should be allowed
        if (specificToReference.isDynamic()) return true;


        SpecificTypeReference toType = specificToReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
        SpecificTypeReference fromType = specificFromReference.fullyResolveTypeDefAndUnwrapNullTypeReference();

        if(toType instanceof  SpecificHaxeClassReference ref) specificToReference = ref;
        if(fromType instanceof  SpecificHaxeClassReference ref) specificFromReference = ref;

        //test can assign but do not include implicit cast for abstracts (@:to/@:from methods), only underlying types to/from  casts allowed.
        if(canAssignToFromType(specificToReference, specificFromReference, false, null)) return true;
      }
    }

    if (from.isEnumClass()) return false;
    if(!from.isContextAType()) return false;
    // if from is literal class name
    if (to.isClass()){
      SpecificHaxeClassReference expectedType = to.getSpecifics()[0].getClassType();
      boolean sameClass = canAssignToFromSpecificType(expectedType, from);
      if (sameClass) {
        return true;
      } else {
        // check if "from" class extends "to" class
        PsiClass[] supers = from.getHaxeClass().getSupers();
        for (PsiClass haxeType : supers) {
          if (haxeType instanceof  HaxeClass haxeClass) {
            if (expectedType.getHaxeClass() == haxeClass) return true;
          }
        }
      }
    }

    if(specificsTo.length !=  specificsFrom.length) return false;
    if(specificsTo.length == 0) return false;
    if(specificsFrom.length == 0) return false;
    SpecificHaxeClassReference typeParameterTo =specificsTo[0].getClassType();
    SpecificHaxeClassReference typeParameterFrom =specificsFrom[0].getClassType();
    // The compiler does not accept types to be assigned to Class<Any> without being casted
    // This is probably due to the abstract implicit cast methods only accepting and returning instances.
    if(typeParameterTo.isAny() && !from.isAny()) return false;

    //TODO
    //HACK prevent overflow  when circular, this really should not happen but it does :(
    // need to find where in the code we creates this loop, probably generic propagation
    if (simpleTypeParameterRecursionGuard(to, from, typeParameterTo, typeParameterFrom)) return false;

    return canAssignToFromType(typeParameterTo, typeParameterFrom);
  }
  private static boolean handleEnumType(SpecificHaxeClassReference to, SpecificHaxeClassReference from) {
    if(from.isEnumValueClass()) return false;
    if (from.isClass()) return false;
    // prevent overflow due to wrap in Enum class


    ResultHolder[] specificsTo = to.getSpecifics();
    ResultHolder[] specificsFrom = from.getSpecifics();

    if (specificsTo.length != specificsFrom.length) return false;
    if (specificsTo.length == 0) return false;
    if (specificsFrom.length == 0) return false;

    SpecificHaxeClassReference typeParameterTo = specificsTo[0].getClassType();
    SpecificHaxeClassReference typeParameterFrom = specificsFrom[0].getClassType();
    //TODO
    //HACK prevent overflow  when circular, this really should not happen but it does :(
    // need to find where in the code we creates this loop, probably generic propagation
    // possible hint,  seems to be related to abstracts and/Or enums ?
    if (simpleTypeParameterRecursionGuard(to, from, typeParameterTo, typeParameterFrom)) {
      log.warn("Recursion guard for Enum compare");
      return true;
    }

    return canAssignToFromType(typeParameterTo, typeParameterFrom);
  }

  /**
   * just making sure type and typeParameter is the same class
   */
  private static boolean simpleTypeParameterRecursionGuard(SpecificHaxeClassReference to,
                                                           SpecificHaxeClassReference from,
                                                           SpecificHaxeClassReference typeParameterTo,
                                                           SpecificHaxeClassReference typeParameterFrom) {
    if (to.isWrapper) to = to.getSpecifics()[0].getClassType();
    if (from.isWrapper) from = from.getSpecifics()[0].getClassType();
    if (to == typeParameterTo && to.getSpecifics().length > 0) return true;
    if (from == typeParameterFrom && from.getSpecifics().length > 0) return true;

    return false;
  }

  static public boolean canAssignToFromSpecificType(
    @NotNull SpecificHaxeClassReference to,
    @NotNull SpecificHaxeClassReference from
  ) {
    return canAssignToFromSpecificType(to, from, false, null);
  }
  static public boolean canAssignToFromSpecificType(
    @NotNull SpecificHaxeClassReference to,
    @NotNull SpecificHaxeClassReference from,
    boolean allowEnumToEnumValueTypeParameter
  ) {
    return canAssignToFromSpecificType(to, from, allowEnumToEnumValueTypeParameter, null);
  }
  static private boolean canAssignToFromSpecificType(
    @NotNull SpecificHaxeClassReference to,
    @NotNull SpecificHaxeClassReference from,
    boolean allowEnumToEnumValueTypeParameter,
    @Nullable List<HaxeModel> recursionGuard
  ) {
    if (to.isDynamic() || from.isDynamic()) {
      return true;
    }
    if (to.isTypeParameter() && !from.isTypeParameter()){
      // we allow assign to typeParameter when only one of the types is a type parameter
      return true;
    }

    if (from.isUnknown()) {
      return true;
    }
    // anonymous and object literal definitions does not have specifics
    if (to.isAnonymousType() || from.isAnonymousType()) {
      return to.getHaxeClass() == from.getHaxeClass();
    }
    // make sure no typedef
    if(to.isTypeDefOfClass()) {
      SpecificTypeReference resolvedTo = to.fullyResolveTypeDefAndUnwrapNullTypeReference();
      if (resolvedTo instanceof SpecificHaxeClassReference classReference) to = classReference;
    }
    if(from.isTypeDefOfClass()) {
      SpecificTypeReference resolvedfrom = from.fullyResolveTypeDefAndUnwrapNullTypeReference();
      if (resolvedfrom instanceof SpecificHaxeClassReference classReference) from = classReference;
    }

    if (to.getHaxeClassReference().refersToSameClass(from.getHaxeClassReference())) {
      if (to.getSpecifics().length == from.getSpecifics().length) {
        int specificsLength = to.getSpecifics().length;
        for (int n = 0; n < specificsLength; n++) {

          ResultHolder toHolder = to.getSpecifics()[n];
          ResultHolder fromHolder = from.getSpecifics()[n];
          if(toHolder.equals(fromHolder)) continue;
          // when checking typeParameter constraints we must allow Enum types to be assigned to EnumValue. (only applies to constraints)
          if(allowEnumToEnumValueTypeParameter) {
            if(toHolder.getType().isEnumValueClass() && fromHolder.isEnum()) continue;
          }

          //allow assign if we got unspecified (type parameter) and real type as we cant tell if its right or wrong?
          // added this due to failing test, but in real world code it works just fine without, maybe look at test
          if (toHolder.isTypeParameter() &&  !fromHolder.isTypeParameter()) return true;

          if (typeCanBeWrapped(toHolder) && typeCanBeWrapped(fromHolder)) {
            SpecificHaxeClassReference toSpecific = toHolder.getClassType();
            SpecificHaxeClassReference fromSpecific = fromHolder.getClassType();

            if (toSpecific != null  &&  fromSpecific!= null) {
              //HACK make sure we can assign  literal collections to variable with EnumValue specifics
              if (from.isLiteralMap() || from.isLiteralArray()) {
                if (toSpecific.isEnumValueClass() && fromSpecific.isEnumType()) return true;
              }
            }
            boolean toSpecIsEnum = false;
            if(toSpecific != null ) {
              toSpecIsEnum = toSpecific.isEnumType();
              SpecificHaxeClassReference specific = wrapType(toHolder, to.context, toSpecIsEnum);
              // recursive protection
              if(referencesAreDifferent(to, specific)) toSpecific = specific;
            }
            if(fromSpecific != null ) {
              boolean useEnumWrapper = fromSpecific.isEnumType();
              if (toSpecIsEnum && fromSpecific.isTypeParameter()) useEnumWrapper = true;
              SpecificHaxeClassReference specific = wrapType(fromHolder, from.context, useEnumWrapper);
              // recursive protection
              if(referencesAreDifferent(from, specific)) fromSpecific = specific;
            }
            if (!canAssignToFrom(toSpecific, fromSpecific)) return false;

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
    // if not working with type parameters check inheritance
    if (!to.isTypeParameter() && !from.isTypeParameter()) {
      if (to.getHaxeClass() != null && to.getHaxeClass().isInterface()) {
        Set<SpecificHaxeClassReference> fromInferTypes = from.getInferTypes();
        for (SpecificHaxeClassReference fromInterface : fromInferTypes) {
          if(fromInterface == from) continue;
          HaxeClassModel classModel = fromInterface.getHaxeClassModel();
          if (recursionGuard == null) recursionGuard = new ArrayList<>();
          if (!recursionGuard.contains(classModel)) {
            recursionGuard.add(classModel);
            if (canAssignToFromSpecificType(to, fromInterface, allowEnumToEnumValueTypeParameter, recursionGuard)) return true;
          }
        }
      }
      HaxeClass haxeClass = from.getHaxeClass();
      if (haxeClass != null) {
        boolean isAbstract = haxeClass.isAbstractType();
        List<HaxeType> extendsList = !isAbstract ? haxeClass.getHaxeExtendsList() : haxeClass.getModel().getAbstractToList() ;
          for (HaxeType type : extendsList) {
            PsiElement resolve = type.getReferenceExpression().resolve();
            if (resolve instanceof HaxeClass fromClass) {
              HaxeClassModel fromModel = fromClass.getModel();
              if (fromModel != null) {
                if (recursionGuard == null) recursionGuard = new ArrayList<>();
                if (!recursionGuard.contains(fromModel)) {
                  recursionGuard.add(fromModel);
                  SpecificHaxeClassReference fromReference =
                    SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(fromModel, fromClass));
                  if (canAssignToFromSpecificType(to, fromReference, allowEnumToEnumValueTypeParameter, recursionGuard)) return true;
                }
              }
            }
          }
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
    SpecificHaxeClassReference aClass = getStdClass(useEnum ? "Enum" : "Class", context, new ResultHolder[]{type});
    aClass.isWrapper = true;
    return aClass;
  }

  // We only want to wrap "real" types in Class<T>, ex Class<String>
  // Other combinations makes little to no sense ( Class<Null> or Class<int->int> etc. )
  private static boolean typeCanBeWrapped(ResultHolder holder) {
    if (holder == null)
      return false;
    return holder.isClassType()
           && !holder.isFunctionType()
           && !holder.isAnonymousType()
           && !holder.isUnknown()
           && !holder.isVoid()
           && !holder.isDynamic();
  }
}
