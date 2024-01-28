/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018-2020 Eric Bishton
 * Copyright 2018 Ilya Malanin
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
import com.intellij.plugins.haxe.lang.psi.impl.HaxeDummyASTNode;
import com.intellij.plugins.haxe.lang.psi.impl.HaxePsiCompositeElementImpl;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceImpl;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.model.HaxeProjectModel;
import com.intellij.plugins.haxe.util.HaxeProjectUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@CustomLog
public abstract class SpecificTypeReference {
  public static final String NULL = "Null";
  public static final String VOID = "Void";
  public static final String BOOL = "Bool";
  public static final String INT = "Int";
  public static final String FLOAT = "Float";
  public static final String STRING = "String";
  public static final String ARRAY = "Array";
  public static final String DYNAMIC = "Dynamic";
  public static final String ENUM_VALUE = "EnumValue";
  public static final String ENUM = "Enum";  // For Enum<EnumName>
  public static final String CLASS = "Class";  // For Class<Type>
  public static final String FLAT_ENUM = "haxe.Constraints.FlatEnum";
  public static final String UNKNOWN = "unknown"; // TODO: Should NOT a legal type name.
  public static final String ITERATOR = "Iterator";
  public static final String FUNCTION = "Function";
  public static final String INVALID = "@@Invalid";
  public static final String MAP = "haxe.ds.Map";
  public static final String IMAP = "haxe.Constraints.IMap"; // Underlying interface for Map types.
  public static final String INT_MAP = "haxe.ds.IntMap";
  public static final String STRING_MAP = "haxe.ds.StringMap";
  public static final String OBJECT_MAP = "haxe.ds.ObjectMap";
  public static final String ENUM_VALUE_MAP = "haxe.ds.EnumValueMap";
  public static final String ANY = "Any"; // Specifically, the "Any" class; See <Haxe>/std/Any.hx.

  /**
   * The context is a parent to be used in a treeWalkUp -- see {@link PsiElement#getContext()}.
   */
  final protected PsiElement context;

  public SpecificTypeReference(@NotNull PsiElement context) {
    this.context = context;
  }

  public static SpecificTypeReference createArray(@NotNull ResultHolder elementType, PsiElement context) {
    final HaxeClassReference classReference = getStdClassReference(ARRAY, context);
    return SpecificHaxeClassReference.withGenerics(classReference, new ResultHolder[]{elementType}, null);
  }

  public static SpecificTypeReference createMap(@NotNull ResultHolder keyType, @NotNull ResultHolder valueType, final PsiElement context) {
    // The code for this function *should* be 'return getExpectedMapType(keyType, valueType);'.  It is not; because the compiler
    // doesn't *really* map to the other types, though it is documented as such.  A 'trace' of an inferred type *will* output
    // the expected target map type (StringMap, IntMap, etc.).  However, with any map of an inferred target type,
    // the compiler still allows array access; it doesn't when the map type is specified.  So, to remain compatible,
    // we have to leave them as Map objects internally and just allow assignment.

    final ResultHolder[] generics = new ResultHolder[]{keyType, valueType};
    return getStdClass(MAP, context, generics);
  }

  /**
   * Creates a context to use when there is none to be found.  Try *very* hard not to use this, please.
   *
   * @return a context that leads nowhere
   */
  public static PsiElement createUnknownContext() {
    return new HaxePsiCompositeElementImpl(new HaxeDummyASTNode(UNKNOWN, HaxeProjectUtil.getLikelyCurrentProject()));
  }

  public static SpecificTypeReference getExpectedMapType(@NotNull ResultHolder keyType, @NotNull ResultHolder valueType) {
    // Maps are created as specific types, using these rules (from Map.hx constructor documentation):
    //   [Map new()] becomes a constructor call to one of the specialization types in
    //   the output. The rules for that are as follows:
    //
    //     1. if K is a `String`, `haxe.ds.StringMap` is used
    //     2. if K is an `Int`, `haxe.ds.IntMap` is used
    //     3. if K is an `EnumValue`, `haxe.ds.EnumValueMap` is used
    //     4. if K is any other class or structure, `haxe.ds.ObjectMap` is used
    //     5. if K is any other type, it causes a compile-time error

    final PsiElement context = keyType.getElementContext();
    final SpecificTypeReference keyTypeReference = keyType.getType();
    String mapClass = MAP;
    if (keyTypeReference.isString()) {
      mapClass = STRING_MAP;
    }
    else if (keyTypeReference.isInt()) {
      mapClass = INT_MAP;
    }
    else if (keyTypeReference.isEnumValue()       // For Map<EnumValue, ...>
             ||  keyTypeReference.isEnumClass()) {    // For Map<Enum<name>, ...>
      mapClass = ENUM_VALUE_MAP;
    }
    // TODO: Implement anonymous structures (HaxeObjectLiterals) in HaxeExpressionEvaluator.
    //else if (keyTypeReference.isAnonymous()) {
    //  mapClass = OBJECT_MAP;
    //}
    else {
      SpecificHaxeClassReference keyClassReference = keyType.getClassType();
      if (null != keyClassReference) {
        HaxeClassModel classModel = keyClassReference.getHaxeClassModel();
        if (null != classModel) {
          HaxeClass modelPsi = classModel.getPsi();
          if (modelPsi instanceof HaxeAnonymousType) {
            mapClass = OBJECT_MAP;
          }
          else if (modelPsi instanceof HaxeEnumDeclaration) {
            mapClass = ENUM_VALUE_MAP;
          }
        }
      }
    }

    final ResultHolder[] generics = MAP.equals(mapClass) || ENUM_VALUE_MAP.equals(mapClass)
                                    ? new ResultHolder[]{keyType, valueType}
                                    : new ResultHolder[]{valueType};

    return getStdClass(mapClass, context, generics);

  }

  public static SpecificHaxeClassReference getStdClass(@NotNull String mapClass, @NotNull PsiElement context, @NotNull ResultHolder[] specifics) {
    final HaxeClassReference classReference = getStdClassReference(mapClass, context);
    return SpecificHaxeClassReference.withGenerics(classReference, specifics, null);
  }

  public static SpecificHaxeClassReference getVoid(@NotNull PsiElement context) {
    return primitive(VOID, context);
  }

  public static SpecificHaxeClassReference getBool(@NotNull PsiElement context) {
    return primitive(BOOL, context);
  }

  public static SpecificHaxeClassReference getInt(@NotNull PsiElement context) {
    return primitive(INT, context);
  }
  public static SpecificHaxeClassReference getFloat(@NotNull PsiElement context) {
    return primitive(FLOAT, context);
  }

  public static SpecificHaxeClassReference getInt(@NotNull PsiElement context, int value) {
    return primitive(INT, context, value);
  }

  public static SpecificHaxeClassReference getDynamic(@NotNull PsiElement context) {
    return primitive(DYNAMIC, context);
  }

  public static SpecificHaxeClassReference getAny(@NotNull PsiElement context) {
    return primitive(ANY, context);
  }

  public static SpecificHaxeClassReference getString(@NotNull PsiElement context) {
    HaxeClassReference ref = getStdClassReference(STRING, context);
    return SpecificHaxeClassReference.withoutGenerics(ref);
  }

  public static SpecificHaxeClassReference getEnumValue(@NotNull PsiElement context) {
    HaxeClassReference ref = getStdClassReference(ENUM_VALUE, context);
    return SpecificHaxeClassReference.withoutGenerics(ref);
  }

  public static SpecificHaxeClassReference getEnum(@NotNull PsiElement context, @NotNull SpecificHaxeClassReference enumType) {
    HaxeClassReference ref = getStdClassReference(ENUM, context);
    return SpecificHaxeClassReference.withGenerics(ref, new ResultHolder[]{enumType.createHolder()});
  }

  public static SpecificHaxeClassReference getUnknown(@NotNull PsiElement context) {
    return SpecificHaxeClassReference.withoutGenerics(getUnknownClassReference(context));
  }

  public static SpecificHaxeClassReference getInvalid(@NotNull PsiElement context) {
    return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(INVALID, context));
  }

  public static SpecificHaxeClassReference getIterator(SpecificHaxeClassReference type) {
    final PsiElement context = type.getElementContext();
    final HaxeClassReference classReference = getStdClassReference(ITERATOR, context);
    return SpecificHaxeClassReference.withGenerics(classReference, new ResultHolder[]{type.createHolder()});
  }

  public static SpecificHaxeClassReference getFunction(@NotNull PsiElement context) {
    return primitive("haxe.Constraints.Function", context);  // Not simply FUNCTION, because it's a class inside of the Constraints file.
  }

  public static SpecificHaxeClassReference primitive(String name, @NotNull PsiElement context) {
    return SpecificHaxeClassReference.withoutGenerics(getStdClassReference(name, context));
  }

  public static SpecificHaxeClassReference primitive(String name, @NotNull PsiElement context, Object constant) {
    return SpecificHaxeClassReference.withoutGenerics(getStdClassReference(name, context), constant);
  }

  public SpecificTypeReference withRangeConstraint(HaxeRange range) {
    return this;
  }

  final public boolean isUnknown() {
    return this.isNamedType(UNKNOWN);
  }

  final public boolean isDynamic() {
    return this.isNamedType(DYNAMIC);
  }

  final public boolean isInvalid() {
    return this.isNamedType(INVALID);
  }

  final public boolean isVoid() {
    return this.isNamedType(VOID);
  }

  final public boolean isInt() {
    return this.isNamedType(INT);
  }

  final public boolean isNumeric() {
    return isInt() || isFloat();
  }

  final public boolean isBool() {
    return this.isNamedType(BOOL);
  }

  final public boolean isFloat() {
    return this.isNamedType(FLOAT);
  }

  final public boolean isString() {
    return this.isNamedType(STRING);
  }

  final public boolean isArray() {
    return isNamedType(ARRAY);
  }

  final public boolean isMap() {
    if (this instanceof SpecificHaxeClassReference) {
      final SpecificHaxeClassReference reference = (SpecificHaxeClassReference)this;
      String name = reference.getHaxeClassReference().getName();
      return MAP.equals(name)
        || INT_MAP.equals(name)
        || OBJECT_MAP.equals(name)
        || ENUM_VALUE_MAP.equals(name)
        || STRING_MAP.equals(name);
    }
    return false;
  }

  final public boolean isAny() {
    return isNamedType(ANY);
  }

  final public boolean isFunction() {
    return isNamedType(FUNCTION);
  }
  public boolean isNullType() {
    return isNamedType(NULL);
  }

  public boolean isEnumType() {
    if (this instanceof  SpecificHaxeClassReference classReference) {
      return (classReference.getHaxeClass() instanceof HaxeEnumDeclaration);
    }
    return false;
  }

  /** Tell whether the class is the Enum<type> abstract class. */
  final public boolean isEnumClass() {
    return isNamedType(ENUM);
  }
  final public boolean isClass() {
    return isNamedType(CLASS);
  }
  final public boolean isAbstractType() {
    if (this instanceof SpecificHaxeClassReference) {
      final SpecificHaxeClassReference reference = (SpecificHaxeClassReference)this;
      return reference.getHaxeClass() instanceof HaxeAbstractTypeDeclaration;
    }
    return false;
  }
  /**
   * In some cases we can not resolve typeParameters (ex. class level typeParameters used in class members)
   * We currently work around this by creating synthetic types,  this method will return true if the type is
   * one of these synthetic types.
   */
  final public boolean isTypeParameter() {
    if (this instanceof SpecificHaxeClassReference specificHaxeClassReference) {
      return specificHaxeClassReference.getHaxeClassReference().isTypeParameter();
    }
    return false;
  }

  /**
   * checks if type reference is from a typeParameter (ex. `Array<TypeParameter>`)
   * or if its a normal reference (ex `var x = String;` or `MyType.staticMethod()`)
   *
   * @return is class reference from typeParameter.
   */
  final public boolean isReferenceFromTypeParameter() {
    //TODO cache ?
    HaxeTypeParam type = PsiTreeUtil.getParentOfType(this.context, HaxeTypeParam.class);
    return type != null;
  }

  public boolean isAnonymousType() {
    if (this instanceof SpecificHaxeClassReference specificHaxeClassReference) {
      if(specificHaxeClassReference.getHaxeClassReference().getHaxeClass() instanceof  HaxeTypedefDeclaration typedefDeclaration) {
        if (typedefDeclaration.getTypeOrAnonymous() != null ) {
          return typedefDeclaration.getTypeOrAnonymous().getAnonymousType() != null;
        }
      }
    }
    return false;
  }

  final public boolean isPureClassReference() {
    if (context instanceof HaxeReferenceImpl reference && this instanceof  SpecificHaxeClassReference classReference) {
      @NotNull ResultHolder[] specifics = classReference.getSpecifics();
      if (specifics.length != 1) return false;
      return reference.isPureClassReferenceOf(specifics[0].getClassType().getClassName());
    }
    return false;
  }

  final public boolean isEnumValue() {
    return (this instanceof SpecificEnumValueReference)
        || (this.getConstant() instanceof HaxeEnumValueDeclaration)
        || isEnumValueClass();
  }

  final public boolean isEnumValueClass() {
    return isNamedType(ENUM_VALUE);
  }

  private boolean isNamedType(String typeName) {
    if (this instanceof SpecificHaxeClassReference) {
      final SpecificHaxeClassReference reference = (SpecificHaxeClassReference)this;
      final String name = reference.getHaxeClassReference().getName();
      return null != name && name.equals(typeName);
    }
    return false;
  }

  final public ResultHolder getArrayElementType() {
    if (isArray()) {
      final ResultHolder[] specifics = ((SpecificHaxeClassReference)this).getSpecifics();
      if (specifics.length >= 1) return specifics[0];
    }
    return getUnknown(context).createHolder();
  }

  final public ResultHolder getIterableElementType(HaxeGenericResolver parentResolver) {
    if (isArray()) {
      return getArrayElementType();
    }

    if (this instanceof  SpecificHaxeClassReference classReference) {
      HaxeGenericResolver resolver = classReference.getGenericResolver();
      resolver.addAll(parentResolver);

      HaxeClassModel model = classReference.getHaxeClassModel();
      if (model != null) {
        HaxeMemberModel hasNext = model.getMember("hasNext", resolver);
        HaxeMemberModel next = model.getMember("next", resolver);
        if (hasNext != null && next != null) {
          return next.getResultType(resolver);
        }
      }
    }
    return null;
  }

  abstract public HaxeResolveResult asResolveResult();
  abstract public SpecificTypeReference withConstantValue(Object constantValue);

  final public SpecificTypeReference withoutConstantValue() {
    return withConstantValue(null);
  }

  public boolean isConstant() {
    return this.getConstant() != null;
  }

  public HaxeRange getRangeConstraint() {
    return null;
  }

  public Object getConstant() {
    return null;
  }

  final public boolean getConstantAsBool() {
    return HaxeTypeUtils.getBoolValue(getConstant());
  }

  final public double getConstantAsDouble() {
    return HaxeTypeUtils.getDoubleValue(getConstant());
  }

  final public int getConstantAsInt() {
    return HaxeTypeUtils.getIntValue(getConstant());
  }

  @NotNull
  final public PsiElement getElementContext() {
    return context;
  }

  abstract public String toPresentationString();

  abstract public String toString();

  abstract public String toStringWithoutConstant();

  abstract public String toStringWithConstant();

  /** Get the return type of the named method or field in the class referenced by this object. */
  @Nullable
  public ResultHolder access(String name, HaxeExpressionEvaluatorContext context, HaxeGenericResolver resolver) {
    return null;
  }

  final public boolean canAssign(SpecificTypeReference type2) {
    return HaxeTypeCompatible.canAssignToFrom(this, type2);
  }

  final public boolean canAssign(ResultHolder type2) {
    return HaxeTypeCompatible.canAssignToFrom(this, type2);
  }

  public ResultHolder createHolder() {
    return new ResultHolder(this);
  }


  @NotNull
  private static HaxeClassReference getStdClassReference(String className, PsiElement context) {
    final HaxeClassModel model = getStdTypeModel(className, context);
    HaxeClassReference classReference;
    if (model != null) {
      classReference = new HaxeClassReference(model, context);
    } else {
      classReference = new HaxeClassReference(className, context);
    }
    return classReference;
  }

  @Nullable
  public static HaxeClassModel getStdTypeModel(String name, PsiElement context) {
    return HaxeProjectModel.fromElement(context).getStdPackage().getClassModel(name);
  }

  @NotNull
  private static HaxeClassReference getUnknownClassReference(@NotNull PsiElement context) {
    return new HaxeClassReference(HaxeClass.createUnknownClass(context.getNode()).getModel(), context);
  }

  public boolean isLiteralArray() {
    return (isArray() && context instanceof HaxeArrayLiteral);
  }
  public boolean isLiteralMap() {
    return ( isMap() &&  context instanceof  HaxeMapLiteral);
  }


  public static SpecificTypeReference propagateGenericsToType(@Nullable HaxeType type,
                                                              HaxeGenericResolver genericResolver) {
    if (type == null) return null;
    SpecificHaxeClassReference classType = HaxeTypeResolver.getTypeFromType(type, genericResolver).getClassType();
    return propagateGenericsToType(classType, genericResolver);
  }

  public static SpecificTypeReference propagateGenericsToType(@Nullable SpecificTypeReference originalType,
                                                              @Nullable HaxeGenericResolver genericResolver) {
    return propagateGenericsToType(originalType, genericResolver , false);
  }
  public static SpecificTypeReference propagateGenericsToType(@Nullable SpecificTypeReference originalType,
                                                              @Nullable HaxeGenericResolver genericResolver,
                                                              boolean isReturnType
  ) {
    SpecificTypeReference type = originalType;
    if (type == null) return null;
    if (genericResolver == null) return type;

    if (type.isTypeParameter() && type instanceof  SpecificHaxeClassReference classReference) {
      String typeVariableName = classReference.getHaxeClassReference().name;
      ResultHolder possibleValue = isReturnType ? genericResolver.resolveReturnType(classReference) : genericResolver.resolve(typeVariableName);
      if (possibleValue != null) {
        SpecificTypeReference possibleType = possibleValue.getType();
        if (possibleType != null) {
          type = possibleType;
        }
      }
    }
    if (type instanceof  SpecificHaxeClassReference classReference) {
      for (ResultHolder specific : classReference.getSpecifics()) {
        // recursive guard
        if (specific.getClassType() != originalType) {
          //Note to self:  this one caused stackoverflow due to recursion alternating between 2 types
          //final SpecificTypeReference typeReference = propagateGenericsToType(specific.getClassType(), genericResolver);
          if (specific.getClassType() != null) {
            SpecificTypeReference typeReference = null;
            if (!specific.getClassType().isTypeParameter()) {
              HaxeGenericResolver resolver = specific.getClassType().getGenericResolver().without(specific);
              typeReference = propagateGenericsToType(specific.getClassType(), resolver, isReturnType);
            }else {
              if (isReturnType) {
                ResultHolder resolve = genericResolver.resolveReturnType(specific);
                if (resolve != null) typeReference = resolve.getType();
              }
              else {
                ResultHolder resolve = genericResolver.resolve(specific);
                if (resolve != null) typeReference = resolve.getType();
              }
            }
            if (null != typeReference) {
              specific.setType(typeReference);
            }
          }
        }
        else {
          log.warn("can not propagate Generics To Type, type and specific is the same type");
        }
      }
    }
    return type;
  }


  public boolean isSameType(@NotNull SpecificTypeReference other) {
    if (!this.getClass().equals(other.getClass())) {
      return false;
    }
    if (this instanceof SpecificHaxeClassReference thisReference && other instanceof SpecificHaxeClassReference otherReference) {
      HaxeClassModel thisModel = thisReference.getHaxeClassModel();
      HaxeClassModel otherModel = otherReference.getHaxeClassModel();
      if (hasSameQualifiedInfo(thisModel, otherModel)) {
        return true;
      }
      return false;// one or more unknown models,  need a different way to compare
    }
    if (this instanceof SpecificEnumValueReference thisReference && other instanceof SpecificEnumValueReference otherReference) {
      HaxeClassModel thisModel = thisReference.getEnumClass().getHaxeClassModel();
      HaxeClassModel otherModel = otherReference.getEnumClass().getHaxeClassModel();
      if (hasSameQualifiedInfo(thisModel, otherModel)) {
        String thisName = thisReference.getModel().getName();
        String otherName = otherReference.getModel().getName();
        return Objects.equals(thisName, otherName);
      }

    }
    if (this instanceof  SpecificFunctionReference thisReference && other instanceof SpecificEnumValueReference SpecificFunctionReference) {
      //TODO mlo: implement
      log.warn("isSameType NOT IMPLEMENTED for SpecificFunctionReference");
    }
    return false;
  }

  private static boolean hasSameQualifiedInfo(HaxeClassModel thisModel, HaxeClassModel otherModel) {
    if (thisModel != null && otherModel != null) {
      FullyQualifiedInfo thisInfo = thisModel.getQualifiedInfo();
      FullyQualifiedInfo otherInfo = otherModel.getQualifiedInfo();
      if (thisInfo == null || otherInfo == null) return false;
      return thisInfo.getClassPath().equals(otherInfo.getClassPath());
    }
    return false;
  }


}
