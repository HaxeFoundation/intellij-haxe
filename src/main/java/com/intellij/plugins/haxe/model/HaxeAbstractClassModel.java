/*
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
package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterDeclaration;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataContent;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionContext;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionEvaluation;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionUtil;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

import static com.intellij.plugins.haxe.util.HaxeMetadataUtil.getMethodsWithMetadata;

public class HaxeAbstractClassModel extends HaxeClassModel {
  private static final Logger LOG = Logger.getLogger("#HaxeAbstractClassModel");

  public HaxeAbstractClassModel(@NotNull HaxeAbstractTypeDeclaration haxeClass) {
    super(haxeClass);
  }

  @Nullable
  @Override
  public HaxePsiCompositeElement getBodyPsi() {
    return getAbstractClassBody();
  }

  public boolean hasForwards() {
    return hasCompileTimeMeta(HaxeMeta.FORWARD);
  }

  public HaxeClass getUnderlyingClass(@Nullable final HaxeGenericResolver resolver) {
    HaxeUnderlyingType underlyingTypePsi = getAbstractClass().getUnderlyingType();
    if (underlyingTypePsi == null) {
      // Null is a special type with no declared underlying class, but the generic
      // declares the underlying type.  (e.g. Underlying class for "Null<String>" is "String").
      if ("Null".equals(getName()) && isCoreType()) {
        if (null == resolver) {
          LOG.warning("Can't find underlying class for Null<> because there is no resolver/specialization.");
          return null;
        }
        List<HaxeGenericParamModel> params = getGenericParams();
        if (!params.isEmpty()) {
          ResultHolder holder = resolver.resolveTypeParameter(params.get(0).getTypeParameter());
          if(holder != null && holder.getClassType() != null )return holder.getClassType().getHaxeClass();
        }
      }
      return null;
    }
    HaxeTypeOrAnonymous anonymous = underlyingTypePsi.getTypeOrAnonymous();
    final HaxeType underlyingType = anonymous != null ? anonymous.getType() : null;
    if (underlyingType != null) {
      HaxeReferenceExpression referenceExpression = underlyingType.getReferenceExpression();
      PsiElement resolve = referenceExpression.resolve();
      if(resolve instanceof HaxeTypeParameterDeclaration typeParameter) {
        ResultHolder holder = resolver == null ? null : resolver.resolveTypeParameter(typeParameter);
        if(holder != null && holder.getClassType() != null )return holder.getClassType().getHaxeClass();
      }else if (resolve instanceof  HaxeClass underlHaxeClass) {
        if(resolver != null) {
          ResultHolder resolvedUnderlyingClass = resolver.resolve(underlHaxeClass);
          if(resolvedUnderlyingClass != null && !resolvedUnderlyingClass.isUnknown()) {
            if(resolvedUnderlyingClass.getClassType() != null) {
              return resolvedUnderlyingClass.getClassType().getHaxeClass();
            }
          }
        }
        return  underlHaxeClass;
      }
    }

    return null;
  }


  public HaxeAbstractTypeDeclaration getAbstractClass() {
    return (HaxeAbstractTypeDeclaration)getBasePsi();
  }

  protected HaxeAbstractBody getAbstractClassBody() {
    return getAbstractClass().getAbstractBody();
  }

  public boolean isForwarded(String name) {
    if (null == name) return false;
    if(!hasForwards()) return false;

    boolean allEmpty = true;
    HaxeMetadataList forwardMetaList = HaxeMetadataUtils.getMetadataList(getBasePsi(), HaxeMeta.COMPILE_TIME, HaxeMeta.FORWARD);
    for (HaxeMeta forward : forwardMetaList) {
      HaxeMetadataContent content = forward.getContent();
      if (null != content) {
        List<HaxeExpression> expressions = HaxeMetadataUtils.getCompileTimeExpressions(content);
        for (HaxeExpression expression : expressions) {
          allEmpty = false;
          if (expression.getText().equals(name)) {
            return true;
          }
        }
      }
    }
    return allEmpty;
  }

  public List<SpecificTypeReference> getDirectCastToTypes(@NotNull HaxeGenericResolver resolver) {
    List<HaxeFunctionType> functionTypes = getDirectCastToPsiFunctionTypes();
    List<HaxeType> classTypes = getDirectCastToPsiTypes();

    List<SpecificTypeReference> typeList = new ArrayList<>();

    for (HaxeFunctionType functionType : functionTypes) {
      ResultHolder typeFromFunctionType = HaxeTypeResolver.getTypeFromFunctionType(functionType, resolver);
      SpecificTypeReference resultHolderType = typeFromFunctionType.getType();
      typeList.add(resultHolderType);
    }

    for (HaxeType type : classTypes) {
      ResultHolder typeFromType = HaxeTypeResolver.getTypeFromType(type, resolver);
      SpecificTypeReference resultHolderType = typeFromType.getType();
      typeList.add(resultHolderType);
    }

    return typeList;

  }

  public List<SpecificTypeReference> getDirectCastFromTypes(@NotNull HaxeGenericResolver resolver) {
    List<HaxeFunctionType> functionTypes = getDirectCastFromPsiFunctionTypes();
    List<HaxeType> classTypes = getDirectCastFromPsiTypes();

    List<SpecificTypeReference> typeList = new ArrayList<>();

    for (HaxeFunctionType functionType : functionTypes) {
      ResultHolder typeFromFunctionType = HaxeTypeResolver.getTypeFromFunctionType(functionType);
      SpecificTypeReference resultHolderType = typeFromFunctionType.getType();
      typeList.add(resultHolderType);
    }

    for (HaxeType type : classTypes) {
      ResultHolder typeFromType = HaxeTypeResolver.getTypeFromType(type, resolver);
      SpecificTypeReference resultHolderType = typeFromType.getType();
      typeList.add(resultHolderType);
    }

    return typeList;
  }

  /**
   * <code>@to</code> methods can be either static with input argument (<code>this type -> new type</code>) or member method  (<code> () -> new Type</code>)
   */
  public List<SpecificTypeReference> getImplicitCastToTypes(SpecificHaxeClassReference classReference, @NotNull HaxeGenericResolver resolver) {
    List<SpecificTypeReference>  typeList = new ArrayList<>();
    List<HaxeMethodModel> castToMethods = getMethodsWithMetadata(haxeClass.getModel(), HaxeMeta.TO, HaxeMeta.COMPILE_TIME, resolver);
    for (HaxeMethodModel method : castToMethods) {
      List<HaxeParameterModel> parameters = method.getParameters();
      // can be both static method with parameter and member method without any parameters
      if(parameters.isEmpty()) {
        // Note: // Note: should probably evaluate function, we need to correctly use typeHint  @:to method<T>():T
        typeList.add(method.getReturnType(resolver).getType());
      }else {
        List<SpecificTypeReference> arguments = List.of(classReference);

        SpecificTypeReference paramType = parameters.getFirst().getType().getType();
        //  try to cast our class to parameter type if its a class (to get correct typeParameters)
        if (paramType instanceof SpecificHaxeClassReference paramClass) {
          SpecificHaxeClassReference casted = classReference.tryCastTo(paramClass);
          // if we can not cast our type to match the parameter type then this method cant be used and we skip it
          if (casted == null) continue;
          arguments = List.of(casted);
        }

        HaxeCallExpressionContext context = HaxeCallExpressionUtil.createContextForMethodCall(arguments, method, null);
        context.setCallie(classReference);
        HaxeCallExpressionEvaluation evaluation = context.evaluate();
        if (evaluation.isValid()) {
          typeList.add(evaluation.getReturnType().getType());
        }
      }
    }
    return typeList;
  }

  public Map<SpecificFunctionReference, SpecificTypeReference> getImplicitCastToFunctionAndTypes(SpecificHaxeClassReference classReference, @NotNull HaxeGenericResolver resolver) {
    Map<SpecificFunctionReference, SpecificTypeReference>  functionAndType = new HashMap<>();
    List<HaxeMethodModel> castToMethods = getMethodsWithMetadata(haxeClass.getModel(), HaxeMeta.TO, HaxeMeta.COMPILE_TIME, resolver);
    for (HaxeMethodModel method : castToMethods) {
      SpecificFunctionReference functionType = method.getFunctionType(null);
      List<HaxeParameterModel> parameters = method.getParameters();
      // can be both static method with parameter and member method without any parameters
      if(parameters.isEmpty()) {
        // Note: // Note: should probably evaluate function, we need to correctly use typeHint  @:to method<T>():T
        functionAndType.put(functionType, method.getReturnType(resolver).getType());
      }else {
        List<SpecificTypeReference> arguments = List.of(classReference);
        HaxeCallExpressionContext context = HaxeCallExpressionUtil.createContextForMethodCall(arguments, method, null);
        HaxeCallExpressionEvaluation evaluation = context.evaluate();
        if (evaluation.isValid()) {
          functionAndType.put(functionType, evaluation.getReturnType().getType());
        }
      }
    }
    return functionAndType;
  }

  /**
   * All <code>@:from</code> methods should have a parameter (some type -> this type),we want to collect the parameter
   * types for methods that returns our type (important to check type parameters here as the type might be the same
   * but the type parameters might be incompatible)
   */
  public List<SpecificTypeReference> getImplicitCastFromTypes(SpecificTypeReference argument, SpecificHaxeClassReference ourClassReference) {
    List<SpecificTypeReference>  typeList = new ArrayList<>();
    HaxeGenericResolver genericResolver = ourClassReference.getGenericResolver();
    List<HaxeMethodModel> castToMethods = getMethodsWithMetadata(haxeClass.getModel(), HaxeMeta.FROM, HaxeMeta.COMPILE_TIME, genericResolver);
    for (HaxeMethodModel method : castToMethods) {
      List<HaxeParameterModel> parameters = method.getParameters();
      // should never be empty for @:from methods
      if (!parameters.isEmpty()) {

        List<SpecificTypeReference> arguments;
        // if argument is a class and parameter is class,
        // try to cast argument to parameter type so we get correct typeParameters
        if (argument instanceof SpecificHaxeClassReference argumentAsClass) {
          ResultHolder paramType = parameters.getFirst().getType();
          if (paramType.isClassType()) {
            SpecificHaxeClassReference classType = paramType.getClassType();
            if(classType != null) {
              // fully resolve target before trying to cast (target might be a random typedef)
              SpecificTypeReference typeReference = classType.fullyResolveTypeDefReference();
              if (typeReference instanceof SpecificHaxeClassReference reference) classType = reference;
            }
            if (classType!= null && classType.isTypeParameter()) {
              arguments = List.of(argument);
            } else {
              SpecificHaxeClassReference casted = argumentAsClass.tryCastTo(classType);
              // if we can not cast to parameter type then this method cant be used and we skip it
              if (casted == null) continue;
              arguments = List.of(casted);
            }
          } else {
            arguments = List.of(argument);
          }
        } else {
          arguments = List.of(argument);
        }
        // checking that cast method accepts argument and that the result is compatible with our type;
        HaxeCallExpressionContext context = HaxeCallExpressionUtil.createContextForMethodCall(arguments, method, null);
        HaxeCallExpressionEvaluation evaluation = context.evaluate();
        if (evaluation.isValid()) {
          SpecificTypeReference type = evaluation.getReturnType().getType();
          if (ourClassReference.createHolder().canAssign(type.createHolder())) {
            typeList.add(evaluation.getParameterType(0).getType());
          }
        }
      }
    }
    return typeList;
  }

  public Map<SpecificFunctionReference, SpecificTypeReference> getImplicitCastFromFunctionAndTypes(SpecificTypeReference argument, SpecificHaxeClassReference ourClassReference) {
    Map<SpecificFunctionReference, SpecificTypeReference> functionAndType = new HashMap<>();
    HaxeGenericResolver genericResolver = ourClassReference.getGenericResolver();
    List<HaxeMethodModel> castToMethods = getMethodsWithMetadata(haxeClass.getModel(), HaxeMeta.FROM, HaxeMeta.COMPILE_TIME, genericResolver);
    for (HaxeMethodModel method : castToMethods) {
      SpecificFunctionReference functionType = method.getFunctionType(null);
      List<HaxeParameterModel> parameters = method.getParameters();
      // should never be empty for @:from methods
      if (!parameters.isEmpty()) {
        // checking that cast method accepts argument and that the result is compatible with our type
        HaxeCallExpressionContext context = HaxeCallExpressionUtil.createContextForMethodCall(List.of(argument), method, null);
        HaxeCallExpressionEvaluation evaluation = context.evaluate();
        if (evaluation.isValid()) {
          SpecificTypeReference type = evaluation.getReturnType().getType();
          if (ourClassReference.createHolder().canAssign(type.createHolder())) {
            // todo get parameter from evaluation ? (it should have correct typeParameters etc)
            SpecificTypeReference acceptedType = parameters.getFirst().getType(genericResolver).getType();
            functionAndType.put(functionType, acceptedType);
          }
        }
      }
    }
    return functionAndType;
  }

  private @NotNull List<HaxeType> getDirectCastToPsiTypes() {
    //TODO cache
    List<HaxeType> types = new LinkedList<>();
    if (haxeClass instanceof HaxeAbstractTypeDeclaration abstractClass) {
      List<HaxeAbstractToType> list = abstractClass.getAbstractToTypeList();
      for (HaxeAbstractToType toType : list) {
        HaxeTypeOrAnonymous typeOrAnonymous = toType.getTypeOrAnonymous();
        if (typeOrAnonymous != null && typeOrAnonymous.getType() != null) {
          types.add(typeOrAnonymous.getType());
        }
      }
    }
    return types;
  }
  private @NotNull List<HaxeFunctionType> getDirectCastToPsiFunctionTypes() {
    //TODO cache
    List<HaxeFunctionType> types = new LinkedList<>();
    if (haxeClass instanceof HaxeAbstractTypeDeclaration abstractClass) {
      List<HaxeAbstractToType> list = abstractClass.getAbstractToTypeList();
      for (HaxeAbstractToType toType : list) {
        if (toType.getFunctionType() != null) {
          types.add(toType.getFunctionType());
        }
      }
    }
    return types;
  }
  private @NotNull List<HaxeType> getDirectCastFromPsiTypes() {
    //TODO mlo: cache
    List<HaxeType> types = new LinkedList<>();
    if (haxeClass instanceof HaxeAbstractTypeDeclaration abstractClass) {
      List<HaxeAbstractFromType> list = abstractClass.getAbstractFromTypeList();
      for (HaxeAbstractFromType fromType : list) {
        HaxeTypeOrAnonymous typeOrAnonymous = fromType.getTypeOrAnonymous();
        if (typeOrAnonymous != null && typeOrAnonymous.getType() != null) {
          types.add(typeOrAnonymous.getType());
        }
      }
    }
    return types;
  }
  private @NotNull List<HaxeFunctionType> getDirectCastFromPsiFunctionTypes() {
    //TODO mlo: cache
    List<HaxeFunctionType> types = new LinkedList<>();
    if (haxeClass instanceof HaxeAbstractTypeDeclaration abstractClass) {
      List<HaxeAbstractFromType> list = abstractClass.getAbstractFromTypeList();
      for (HaxeAbstractFromType fromType : list) {
        if (fromType.getFunctionType() != null) {
          types.add(fromType.getFunctionType());
        }
      }
    }
    return types;
  }



}
