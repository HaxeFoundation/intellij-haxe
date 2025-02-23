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
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.evaluator.assign.HaxeTypeCompatible;
import com.intellij.plugins.haxe.util.HaxeProjectUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@CustomLog
public abstract class SpecificTypeReference {
  public static final String NULL = "Null";
  public static final String VOID = "Void";
  public static final String BOOL = "Bool";
  public static final String INT = "Int";
  public static final String FLOAT = "Float";
  public static final String SINGLE = "Single";
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
  public static final String MAP_INTERFACE = "haxe.Constraints.IMap";
  public static final String ANY = "Any"; // Specifically, the "Any" class; See <Haxe>/std/Any.hx.
  // varargs
  public static final String REST = "haxe.Rest";
  public static final String EXTERN_REST = "haxe.extern.Rest";

  /**
   * The context is a parent to be used in a treeWalkUp -- see {@link PsiElement#getContext()}.
   */
  final public PsiElement context;

  public SpecificTypeReference(@NotNull PsiElement context) {
    this.context = context;
  }

  public static SpecificHaxeClassReference createArray(@NotNull ResultHolder elementType, PsiElement context) {
    final HaxeClassReference classReference = getStdClassReference(ARRAY, context);
    return SpecificHaxeClassReference.withGenerics(classReference, new ResultHolder[]{elementType}, null);
  }

  public static SpecificHaxeClassReference createMap(@NotNull ResultHolder keyType, @NotNull ResultHolder valueType, final PsiElement context) {
    // The code for this function *should* be 'return getExpectedMapType(keyType, valueType);'.  It is not; because the compiler
    // doesn't *really* map to the other types, though it is documented as such.  A 'trace' of an inferred type *will* output
    // the expected target map type (StringMap, IntMap, etc.).  However, with any map of an inferred target type,
    // the compiler still allows array access; it doesn't when the map type is specified.  So, to remain compatible,
    // we have to leave them as Map objects internally and just allow assignment.

    final ResultHolder[] generics = new ResultHolder[]{keyType, valueType};
    return getStdClass(MAP, context, generics);
  }

  public static SpecificTypeReference wrapInRest(PsiElement basePsi, ResultHolder holder) {
      return getStdClass(REST, basePsi, new ResultHolder[]{holder});
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
    return SpecificHaxeClassReference.withoutGenerics(HaxeClassReference.createNoModelClassReference(INVALID, context));
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

  public static SpecificHaxeClassReference getNull(@NotNull PsiElement context, @NotNull ResultHolder specific) {
    return SpecificHaxeClassReference.withGenerics(getStdClassReference(NULL, context), new ResultHolder[]{specific});
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
    return isInt() || isFloat() || isSingle();
  }

  final public boolean isBool() {
    return this.isNamedType(BOOL);
  }

  final public boolean isFloat() {
    return this.isNamedType(FLOAT);
  }
  final public boolean isSingle() {
    return this.isNamedType(SINGLE);
  }

  final public boolean isString() {
    return this.isNamedType(STRING);
  }

  final public boolean isArray() {
    return isNamedType(ARRAY);
  }

  final public boolean isMapType() {
    if (this instanceof SpecificHaxeClassReference reference) {
      // try to check for map interface first
      HaxeClassModel refModel = reference.getHaxeClassModel();
      if(refModel != null) {
        List<HaxeClassReferenceModel> interfaces = refModel.getImplementingInterfaces();
        for (HaxeClassReferenceModel anInterface : interfaces) {
          HaxeClassModel classModel = anInterface.getHaxeClassModel();
          if (classModel != null) {
            String qualifiedName = classModel.haxeClass.getQualifiedName();
            if (MAP_INTERFACE.equals(qualifiedName)) return true;
          }
        }
      }
      // TODO consider checking underlying type of abstract for MAP_INTERFACE

      HaxeClass haxeClass = reference.getHaxeClass();
      String name = haxeClass != null
              ? haxeClass.getQualifiedName()
              : reference.getHaxeClassReference().getName();

      // fallback checking common maps (useful when std ins not configured)
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

  final public boolean isTypeParameterWithConstraints() {
    if (this instanceof SpecificHaxeClassReference specificHaxeClassReference) {
      HaxeClassReference haxeClassReference = specificHaxeClassReference.getHaxeClassReference();
      HaxeClass haxeClass = haxeClassReference.getHaxeClass();
      if(haxeClass != null) {
      if (haxeClass.getModel() instanceof HaxeGenericParamModel model) {
        return model.hasConstraint();
      }
    }
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
    if (this instanceof SpecificHaxeAnonymousReference) return true;
    if (this instanceof SpecificHaxeClassReference specificHaxeClassReference) {
      HaxeClass aClass = specificHaxeClassReference.getHaxeClassReference().getHaxeClass();
      if (aClass instanceof HaxeAnonymousType) return true;
      if(aClass instanceof  HaxeTypedefDeclaration typedefDeclaration) {
        if (typedefDeclaration.getTypeOrAnonymous() != null ) {
          return typedefDeclaration.getTypeOrAnonymous().getAnonymousType() != null;
        }
      }
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

  public  boolean isEnumReference() {
    if (this instanceof  SpecificHaxeClassReference classReference) {
      HaxeClassModel classModel = classReference.getHaxeClassModel();
      return classModel != null && classModel.isEnum();
    }
    return false;
  }

  private boolean isNamedType(String typeName) {
    if (this instanceof SpecificHaxeClassReference reference) {
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
        HaxeBaseMemberModel hasNext = model.getMember("hasNext", resolver);
        HaxeBaseMemberModel next = model.getMember("next", resolver);
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

  public String toPresentationString() {
    return toPresentationString(false);
  }
  abstract public String toPresentationString(boolean showOnlyConstraintForTypeParam);

  abstract public String toString();

  abstract public String toStringWithoutConstant();

  abstract public String toStringWithConstant();

  /** Get the return type of the named method or field in the class referenced by this object. */
  @Nullable
  public ResultHolder access(String name, HaxeExpressionEvaluatorContext context, HaxeGenericResolver resolver) {
    return null;
  }

  final public boolean canAssign(SpecificTypeReference type2) {
    return HaxeTypeCompatible.canAssignToFromReference(this, type2);
  }

  final public boolean canAssign(ResultHolder type2) {
    return canAssign(type2.getType());
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
      classReference = HaxeClassReference.createNoModelClassReference(className, context);
    }
    return classReference;
  }

  @Nullable
  public static HaxeClassModel getStdTypeModel(String name, PsiElement context) {
    return HaxeProjectModel.fromElement(context).getStdPackage().getClassModel(name);
  }

  @NotNull
  private static HaxeClassReference getUnknownClassReference(@NotNull PsiElement context) {
    return new HaxeClassReference( UNKNOWN, HaxeClass.createUnknownClass(context.getNode()).getModel(), context);
  }

  public boolean isLiteralArray() {
    return (isArray() && context instanceof HaxeArrayLiteral);
  }
  public boolean isEmptyLiteralCollection() {
    return (isArray() && context instanceof HaxeArrayLiteral literal) && literal.getExpressionList() == null;
  }
  public boolean isLiteralMap() {
    return (isMapType() && context instanceof  HaxeMapLiteral);
  }


  public boolean isSameTypeAndGenerics(SpecificTypeReference other) {
    if (other == null) return false;
    if (!isSameType(other)) return false;
    if (this instanceof SpecificHaxeClassReference thisClass
        && other instanceof SpecificHaxeClassReference otherClass) {
      @NotNull ResultHolder[] thisSpecifics = thisClass.getSpecifics();
      @NotNull ResultHolder[] otherSpecifics = otherClass.getSpecifics();
      if (thisSpecifics.length != otherSpecifics.length) return false;
      for (int i = 0; i < thisSpecifics.length; i++) {
        ResultHolder thisSpecific = thisSpecifics[i];
        ResultHolder otherSpecific = otherSpecifics[i];
        if (!thisSpecific.getType().isSameTypeAndGenerics(otherSpecific.getType())){
          return false;
        }
      }
    }
    return true;
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


  public boolean isExpr() {
     if (this instanceof SpecificHaxeClassReference classReference) {
       HaxeClass aClass = classReference.getHaxeClass();
       if (aClass == null) return false;
       String qualifiedName = aClass.getQualifiedName();
       return qualifiedName!= null && qualifiedName.equalsIgnoreCase(HaxeMacroTypeUtil.EXPR);
     }
    return false;
  }
  public boolean isExprOf() {
     if (this instanceof SpecificHaxeClassReference classReference) {
       HaxeClass aClass = classReference.getHaxeClass();
       if (aClass == null) return false;
       String qualifiedName = aClass.getQualifiedName();
       return qualifiedName!= null && qualifiedName.equalsIgnoreCase(HaxeMacroTypeUtil.EXPR_OF);
     }
    return false;
  }

  public abstract SpecificTypeReference withElementContext(PsiElement element);


  public List<ResultHolder> getTypeParameters() {
    List<ResultHolder> typeParams = new ArrayList<>();
    if (this instanceof SpecificHaxeClassReference classReference) {
      @NotNull ResultHolder[] specifics = classReference.getSpecifics();
      for (ResultHolder specific : specifics) {
        if (specific.isTypeParameter()){
          typeParams.add(specific);
        }else {
          typeParams.addAll(specific.getType().getTypeParameters());
        }
      }
    }
    return typeParams;
  }

  // context is important for recursion guards, make sure you dont use the same context for type and null-wrapped type
  public SpecificHaxeClassReference wrapInNullType(@NotNull PsiElement context) {
    return SpecificHaxeClassReference.getNull(context, this.createHolder());
  }
}
