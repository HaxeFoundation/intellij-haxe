/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
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

import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeTypeDefImpl;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeClassReferenceModel;
import com.intellij.plugins.haxe.model.HaxeGenericParamModel;
import com.intellij.plugins.haxe.util.HaxeDebugUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class SpecificHaxeClassReference extends SpecificTypeReference {
  private static final String CONSTANT_VALUE_DELIMITER = " = ";
  private static final Key<String> CACHE_NAME_KEY = new Key<>("HAXE_CACHE_NAME_KEY");
  private static final Key<Set<SpecificHaxeClassReference>> COMPATIBLE_TYPES_KEY = new Key<>("HAXE_COMPATIBLE_TYPES");
  private static final Key<Set<SpecificHaxeClassReference>> INFER_TYPES_KEY = new Key<>("HAXE_INFER_TYPES");
  private static final ThreadLocal<Stack<HaxeClass>> processedElements = ThreadLocal.withInitial(Stack::new);

  @NotNull private final HaxeClassReference classReference;
  @NotNull private final ResultHolder[] specifics;
  @Nullable private final Object constantValue;
  @Nullable private final HaxeRange rangeConstraint;

  @Nullable private SpecificFunctionReference typeDefFunction;
  @Nullable private SpecificHaxeClassReference typeDefClass;

  @Nullable private HaxeClass clazz;

  public SpecificHaxeClassReference(
    @NotNull HaxeClassReference classReference,
    @NotNull ResultHolder[] specifics,
    @Nullable Object constantValue,
    @Nullable HaxeRange rangeConstraint,
    @NotNull PsiElement context
  ) {
    super(context);
    this.classReference = classReference;
    this.specifics = specifics;
    this.constantValue = constantValue;
    this.rangeConstraint = rangeConstraint;
  }

  public static SpecificHaxeClassReference withoutGenerics(@NotNull HaxeClassReference clazz) {
    return new SpecificHaxeClassReference(clazz, ResultHolder.EMPTY, null, null, clazz.elementContext);
  }

  public static SpecificHaxeClassReference withoutGenerics(@NotNull HaxeClassReference clazz, Object constantValue) {
    return new SpecificHaxeClassReference(clazz, ResultHolder.EMPTY, constantValue, null, clazz.elementContext);
  }

  public static SpecificHaxeClassReference withGenerics(@NotNull HaxeClassReference clazz, ResultHolder[] specifics) {
    return new SpecificHaxeClassReference(clazz, specifics, null, null, clazz.elementContext);
  }

  public static SpecificHaxeClassReference withGenerics(@NotNull HaxeClassReference clazz, ResultHolder[] specifics, Object constantValue) {
    return new SpecificHaxeClassReference(clazz, specifics, constantValue, null, clazz.elementContext);
  }

  @Nullable
  public HaxeClass getHaxeClass() {
    if(clazz == null || !clazz.isValid()) {
      clazz = this.getHaxeClassReference().getHaxeClass();
    }
    return clazz;
  }

  public boolean isEnumType() {
    return (this.getHaxeClass() instanceof  HaxeEnumDeclaration);
  }

  public boolean isExpr() {
    return getHaxeClass().getQualifiedName().equals("haxe.macro.Expr");
  }

  public boolean isRest() {
    return getHaxeClass().getQualifiedName().equals("haxe.extern.Rest");
  }

  public HaxeClassModel getHaxeClassModel() {
    final HaxeClass aClass = getHaxeClass();
    return (aClass != null) ? aClass.getModel() : null;
  }

  @Nullable
  public String getClassName() {
    return this.getHaxeClassReference().getName();
  }

  public SpecificHaxeClassReference withConstantValue(Object constantValue) {
    return new SpecificHaxeClassReference(getHaxeClassReference(), getSpecifics().clone(), constantValue, null, context);
  }

  @Override
  public SpecificTypeReference withRangeConstraint(HaxeRange range) {
    if (this.getRangeConstraint() == range) return this;
    return new SpecificHaxeClassReference(getHaxeClassReference(), getSpecifics().clone(), getConstant(), range, context);
  }

  @Override
  public HaxeRange getRangeConstraint() {
    return this.rangeConstraint;
  }

  @Override
  public Object getConstant() {
    return constantValue;
  }

  public String toPresentationString() {
    StringBuilder out = new StringBuilder(this.getHaxeClassReference().getName());
    ResultHolder [] specifics = getSpecifics();
    if (specifics.length > 0) {
      out.append("<");
      for (int n = 0; n < specifics.length; n++) {
        if (n > 0) out.append(", ");
        ResultHolder specific = specifics[n];
        out.append(specific == null ? UNKNOWN : specific.toStringWithoutConstant());
      }
      out.append(">");
    }
    String result = out.toString();
    if(result.equals("Dynamic<Dynamic>")) return "Dynamic";
    if(result.equals("Dynamic<unknown>")) return "Dynamic";
    return result;
  }

  public String toStringWithoutConstant() {
    return toPresentationString();
  }

  public String toStringWithConstant() {
    String out = toStringWithoutConstant();
    if (getConstant() != null) {
      out += CONSTANT_VALUE_DELIMITER + getConstant().toString();

      //switch (out) {
      //  case SpecificTypeReference.INT:
      //    out += CONSTANT_VALUE_DELIMITER + (int)HaxeTypeUtils.getDoubleValue(getConstant());
      //    break;
      //  case SpecificTypeReference.STRING:
      //    out += CONSTANT_VALUE_DELIMITER + getConstant();
      //    break;
      //  default:
      //    out += CONSTANT_VALUE_DELIMITER + getConstant();
      //    break;
      //}
    }

    if(classReference.classModel != null) {
      if(classReference.classModel.haxeClass instanceof HaxeSpecificFunction) {
        HaxeSpecificFunction  specificFunction   = (HaxeSpecificFunction)classReference.classModel.haxeClass;
        String functionRepresentation = SpecificFunctionReference.createFromMethodModel(specificFunction.getMethod().getModel()).toString();
        if(specificFunction.getMethod() != null)out += "<" + functionRepresentation +">";
      }
    }
    if (getRangeConstraint() != null) {
      out += " [" + getRangeConstraint() + "]";
    }
    return out;
  }

  @Override
  public String toString() {
    return toStringWithConstant();
  }

  public HaxeGenericResolver getGenericResolver() {
    HaxeGenericResolver resolver = new HaxeGenericResolver();
    HaxeClassModel model = getHaxeClassModel();
    if (model != null) {
      List<HaxeGenericParamModel> params = model.getGenericParams();
      for (int n = 0; n < params.size(); n++) {
        HaxeGenericParamModel paramModel = params.get(n);
        ResultHolder specific = (n < getSpecifics().length) ? this.getSpecifics()[n] : getUnknown(context).createHolder();
        if(specific == null)  specific = getUnknown(context).createHolder();// null safety
        resolver.add(paramModel.getName(), specific);
      }
    }
    return resolver;
  }

  /** Get the return type of the named method or field in the class referenced by this object. */
  @Nullable
  @Override
  public ResultHolder access(String name, HaxeExpressionEvaluatorContext context, HaxeGenericResolver resolver) {
    if (this.isDynamic()) return this.withoutConstantValue().createHolder();

    if (name == null) {
      return null;
    }
    HaxeClass aClass = this.getHaxeClassReference().getHaxeClass();
    if (aClass == null) {
      return null;
    }
    AbstractHaxeNamedComponent method = (AbstractHaxeNamedComponent)aClass.findHaxeMethodByName(name, resolver);
    if (method != null) {
      if (context.root == method) return null;
      return HaxeTypeResolver.getMethodFunctionType(method, resolver);
    }
    AbstractHaxeNamedComponent field = (AbstractHaxeNamedComponent)aClass.findHaxeFieldByName(name, resolver);
    if (field != null) {
      if (context.root == field) return null;
      return HaxeTypeResolver.getFieldOrMethodReturnType(field, resolver);
    }
    return null;
  }

  public enum Compatibility {
    ASSIGNABLE_TO,   // Assignable via @:to or "to <Type>" on an abstract.
    ASSIGNABLE_FROM  // Assignable via @:from or "from <Type>" on an abstract.
  }

  Set<SpecificHaxeClassReference> getCompatibleTypes(Compatibility direction) {
    HaxeClassModel model = getHaxeClassModel();

    boolean skipCachingForDebug = HaxeDebugUtil.isCachingDisabled();
    validateCache(model, context);

    if (!skipCachingForDebug && (null == model || !model.hasGenericParams())) {
      // If we want to cache all results, then we need a better caching mechanism.
      // This breaks for generic types.  The first check of the generic sets up
      // the compatible types, and then all other instances are checked against that set.
      // So, this fails because the cached types for Null<T> are Null<String> and String:
      //  class Test{
      //    var x:Null<String> = null;
      //    var y:Null<Test> = null;
      //    function new() {
      //      x = "New String";
      //      y = this; // <<< ERROR SomethingElse should be Null<SomethingElse>.
      //    }
      //  }
      Set<SpecificHaxeClassReference> result = context.getUserData(COMPATIBLE_TYPES_KEY);
      if (result == null) {
        processedElements.get().clear();
        result = getCompatibleTypesInternal(direction);
        result.add(this);
        context.putUserData(COMPATIBLE_TYPES_KEY, result);
      }
      return result;
    } else {
      processedElements.get().clear();
      Set<SpecificHaxeClassReference>result = getCompatibleTypesInternal(direction);
      result.add(this);
      return result;
    }
  }

  Set<SpecificHaxeClassReference> getInferTypes() {
    HaxeClassModel model = getHaxeClassModel();
    if (null == model || !model.hasGenericParams()) {
      // Breaks on generics.  See note on getCompatibleTypes.
      Set<SpecificHaxeClassReference> result = context.getUserData(INFER_TYPES_KEY);
      if (result == null) {
        processedElements.get().clear();
        result = getInferTypesInternal();
        context.putUserData(INFER_TYPES_KEY, result);
      }
      return result;
    } else {
      processedElements.get().clear();
      return getInferTypesInternal();
    }
  }

  //TODO : in some strange cases the cache does not seem to be invalidated when a type changes for a field
  // causing interface comparability to fail, this is a cheap workaround for thins problem that invalidates
  // the cache if the type name changes.
  private void validateCache(HaxeClassModel model, PsiElement context) {
    if (model == null) return;
    String name = model.getName();
    String cacheName = this.context.getUserData(CACHE_NAME_KEY);
    if(true || !name.equals(cacheName)) {
      context.putUserData(COMPATIBLE_TYPES_KEY, null);
      context.putUserData(INFER_TYPES_KEY, null);
      context.putUserData(CACHE_NAME_KEY, name);
    }
  }


  private Set<SpecificHaxeClassReference> getCompatibleTypesInternal(Compatibility direction) {
    final Stack<HaxeClass> stack = processedElements.get();
    final HaxeClassModel model = getHaxeClassModel();
    final HaxeGenericResolver genericResolver = getGenericResolver();

    final Set<SpecificHaxeClassReference> list = new HashSet<>();
    if (model == null) return list;
    if (stack.contains(model.haxeClass)) return list;
    stack.push(model.haxeClass);

    // TODO: list.addAll(getCompatibleFunctionTypes(model, genericResolver));
    literalCollectionAssignment(direction, list);
    emptyCollectionAssignment(direction, list);

    if (!model.isAbstract()) {
      if (model.haxeClass instanceof HaxeTypedefDeclaration) {
        SpecificHaxeClassReference type = ((AbstractHaxeTypeDefImpl)model.haxeClass).getTargetClass(genericResolver);
        if (type != null) {
          list.add(type);
          list.addAll(type.getCompatibleTypesInternal(direction));
        }
      } else
      for (HaxeType extendsType : model.haxeClass.getHaxeExtendsList()) {
        SpecificHaxeClassReference type = propagateGenericsToType(extendsType, genericResolver);
        if (type != null) {
          if (model.isInterface()) list.add(type);
          list.addAll(type.getCompatibleTypesInternal(direction));
        }
      }
      // var myVar:MyClass can not be assigned any object with the same interface,
      // but an interface can be assigned any object that implements it
      if(direction == Compatibility.ASSIGNABLE_TO) {
        final List<HaxeClassReferenceModel> interfaces = model.getImplementingInterfaces();
        for (HaxeClassReferenceModel interfaceReference : interfaces) {
          SpecificHaxeClassReference type = propagateGenericsToType(interfaceReference.getPsi(), genericResolver);
          if (type != null) {
            list.add(type);
            list.addAll(type.getCompatibleTypesInternal(direction));
          }
        }
      }
    } else {

      List<HaxeType> typeList = direction == Compatibility.ASSIGNABLE_FROM ? model.getAbstractFromList() : model.getAbstractToList();
      for (HaxeType extendsType : typeList) {
        SpecificHaxeClassReference type = propagateGenericsToType(extendsType, genericResolver);
        if (type != null) {
          list.add(type);
          list.addAll(type.getCompatibleTypesInternal(direction));
        }
      }
    }

    return list;
  }

  private void emptyCollectionAssignment(Compatibility direction, Set<SpecificHaxeClassReference> list) {
    if (direction == Compatibility.ASSIGNABLE_TO && context instanceof HaxeArrayLiteral && null == ((HaxeArrayLiteral)context).getExpressionList()) {
      ResultHolder unknownHolderKey = SpecificTypeReference.getUnknown(context).createHolder();
      ResultHolder unknownHolderValue = SpecificTypeReference.getDynamic(context).createHolder();
      SpecificHaxeClassReference holder = (SpecificHaxeClassReference)SpecificHaxeClassReference.createMap(unknownHolderKey, unknownHolderValue, context);
      list.add(holder);
    }
  }
  private void literalCollectionAssignment(Compatibility direction, Set<SpecificHaxeClassReference> list) {
    // adds "Any" collections to compatibility list if collection is empty literal
    if (direction == Compatibility.ASSIGNABLE_TO) {
      if(isEmptyCollection()) {
        if (isLiteralArray()) {
            ResultHolder unknownHolder = SpecificTypeReference.getAny(context).createHolder();
            SpecificHaxeClassReference array = (SpecificHaxeClassReference)SpecificHaxeClassReference.createArray(unknownHolder, context);
            list.add(array);
          }
        if (isLiteralMap()) {
          ResultHolder unknownHolderKey = SpecificTypeReference.getAny(context).createHolder();
          ResultHolder unknownHolderValue = SpecificTypeReference.getAny(context).createHolder();
          SpecificHaxeClassReference anyAnyMap = (SpecificHaxeClassReference)SpecificHaxeClassReference.createMap(unknownHolderKey, unknownHolderValue, context);
          list.add(anyAnyMap);
        }
      }
    }
  }


  public boolean isContextAnEnumType() {
    if(context instanceof HaxeReferenceExpression) {
      HaxeReferenceExpression element = (HaxeReferenceExpression)context;
      PsiElement resolve = element.resolve();

      if(resolve instanceof HaxeClass) {
        HaxeClass resolved = (HaxeClass) resolve;
        return resolved.isEnum();
      }
    }
    return false;
  }
  public  boolean isContextAnEnumDeclaration() {
    return context instanceof HaxeEnumDeclaration;
  }

  public  boolean isContextAType() {
    if (context instanceof HaxeType) {
      return true;
    }
    else if (context instanceof HaxeReferenceExpression) {
      HaxeReferenceExpression element = (HaxeReferenceExpression)context;
      PsiElement resolve = element.resolve();
      return resolve instanceof HaxeClass;
    }
    return false;
  }

  public boolean isTypeDef() {
    if(clazz instanceof HaxeTypedefDeclaration) return true;
      return getHaxeClassModel() != null && getHaxeClassModel().isTypedef();
  }
  public boolean isTypeDefOfClass() {
    return isTypeDef() && ((AbstractHaxeTypeDefImpl)getHaxeClassModel().haxeClass).getTargetClass() != null;
  }

  public boolean isTypeDefOfFunction() {
    return isTypeDef() && ((AbstractHaxeTypeDefImpl)getHaxeClassModel().haxeClass).getFunctionType() != null;
  }

  public SpecificHaxeClassReference resolveTypeDefClass() {
    if (typeDefClass != null) return typeDefClass;
    if (isTypeDef()) {
      typeDefClass = ((AbstractHaxeTypeDefImpl)getHaxeClassModel().haxeClass).getTargetClass(getGenericResolver());
      return typeDefClass;
    }
    return null;
  }

  public SpecificFunctionReference resolveTypeDefFunction() {
    if (typeDefFunction != null) return typeDefFunction;
    if (isTypeDef()) {
      HaxeFunctionType type = ((AbstractHaxeTypeDefImpl)getHaxeClassModel().haxeClass).getFunctionType();
      if (type != null) {
        HaxeSpecificFunction function = new HaxeSpecificFunction(type, getGenericResolver().getSpecialization(this.getElementContext()));
        typeDefFunction = SpecificFunctionReference.create(function);
        return typeDefFunction;
      }
    }
    return null;
  }

  public boolean isCoreType() {
    HaxeMetadataList list = HaxeMetadataUtils.getMetadataList(this.getHaxeClass());
    return list.stream().anyMatch(meta -> meta.isCompileTimeMeta() &&  meta.isType("coreType"));
  }

  private String getTypeName(PsiElement context) {
    if (context instanceof HaxeType) {
      return context.getText();
    }
    else if (context instanceof HaxeReferenceExpression) {
      HaxeReferenceExpression element = (HaxeReferenceExpression)context;
      PsiElement resolve = element.resolve();

      if (resolve instanceof HaxeClass) {
        HaxeClass resolved = (HaxeClass)resolve;
        if (element.getText().equals(resolved.getName())) {
          return resolved.getName();
        }
      }
    }
    return null;
  }

  private Set<SpecificHaxeClassReference> getInferTypesInternal() {
    final Stack<HaxeClass> stack = processedElements.get();
    final HaxeClassModel model = getHaxeClassModel();
    final HaxeGenericResolver genericResolver = getGenericResolver();

    final Set<SpecificHaxeClassReference> list = new HashSet<>();
    list.add(this);

    if (model == null) return list;
    if (stack.contains(model.haxeClass)) return list;
    stack.push(model.haxeClass);

    if (!model.isAbstract()) {
      if (model.haxeClass instanceof HaxeTypedefDeclaration) {
        SpecificHaxeClassReference type = ((AbstractHaxeTypeDefImpl)model.haxeClass).getTargetClass(genericResolver);
        if (type != null) {
          list.add(type);
          list.addAll(type.getCompatibleTypes(Compatibility.ASSIGNABLE_FROM));
        }
      } else
      for (HaxeType extendsType : model.haxeClass.getHaxeExtendsList()) {
        SpecificHaxeClassReference type = propagateGenericsToType(extendsType, genericResolver);
        if (type != null) {
          list.addAll(type.getInferTypesInternal());
        }
      }

      final List<HaxeClassReferenceModel> interfaces = model.getImplementingInterfaces();
      for (HaxeClassReferenceModel interfaceReference : interfaces) {
        SpecificHaxeClassReference type = propagateGenericsToType(interfaceReference.getPsi(), genericResolver);
        if (type != null) {
          list.addAll(type.getInferTypesInternal());
        }
      }
    } else {
      for (HaxeType extendsType : model.getAbstractToList()) {
        SpecificHaxeClassReference type = propagateGenericsToType(extendsType, genericResolver);
        if (type != null) {
          list.addAll(type.getInferTypesInternal());
        }
      }
    }

    return list;
  }

  public static SpecificHaxeClassReference propagateGenericsToType(@Nullable HaxeType type,
                                                             HaxeGenericResolver genericResolver) {
    if (type == null) return null;
    SpecificHaxeClassReference classType = HaxeTypeResolver.getTypeFromType(type, genericResolver).getClassType();
    return propagateGenericsToType(classType, genericResolver);
  }

  public static SpecificHaxeClassReference propagateGenericsToType(@Nullable SpecificHaxeClassReference type,
                                                             @Nullable HaxeGenericResolver genericResolver) {
    if (type == null) return null;
    if (genericResolver == null) return type;

    if (type.canBeTypeVariable()) {
      String typeVariableName = type.getHaxeClassReference().name;
      ResultHolder possibleValue = genericResolver.resolve(typeVariableName);
      if (possibleValue != null) {
        SpecificHaxeClassReference possibleType = possibleValue.getClassType();
        if (possibleType != null) {
          type = possibleType;
        }
      }
    }
    for (ResultHolder specific : type.getSpecifics()) {
      final SpecificHaxeClassReference classType = propagateGenericsToType(specific.getClassType(), genericResolver);
      if (null != classType) {
        specific.setType(classType);
      }
    }
    return type;
  }

  @Override
  public boolean canBeTypeVariable() {
    return classReference.classModel == null;

  }

  @NotNull
  public HaxeClassReference getHaxeClassReference() {
    return classReference;
  }

  @NotNull
  public ResultHolder[] getSpecifics() {
    return specifics;
  }

  @NotNull
  public HaxeClassResolveResult asResolveResult() {
    HaxeClass clazz = getHaxeClass();
    return HaxeClassResolveResult.create(clazz, getGenericResolver().getSpecialization(clazz));
  }
}
