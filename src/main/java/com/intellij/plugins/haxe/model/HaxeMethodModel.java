/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2020 Eric Bishton
 * Copyright 2017-2018 Ilya Malanin
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

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.model.type.HaxeArgument;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValuesManager;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.intellij.openapi.util.Key;

@EqualsAndHashCode
public class HaxeMethodModel extends HaxeMemberModel implements HaxeExposableModel {

  private static final Key<Boolean> isVoidReturn = Key.create("isReturnTypeVoid");

  private HaxeMethod haxeMethod;
  private String name;


  public HaxeMethodModel(HaxeMethod haxeMethod) {
    super(haxeMethod);
    this.haxeMethod = haxeMethod;
    this.name = getName();
  }

  @Override
  public PsiElement getBasePsi() {
    return haxeMethod;
  }
  public HaxeMethod getMethod() {
    return haxeMethod;
  }

  public HaxeMethodPsiMixin getMethodPsi() {
    return haxeMethod;
  }

  public PsiElement getBodyPsi() {
    PsiElement[] children = haxeMethod.getChildren();
    if (children.length == 0) return null;
    PsiElement child = children[children.length - 1];
    // if we dont have any real body (as with abstract classes) last psi item will be part of the method declaration,
    // so we make sure return null if parameter list or the optional typeTag.
    if (child instanceof  HaxeParameterList) return null;
    if (child instanceof  HaxeTypeTag) return null;
    return child;
  }

  public List<HaxeParameterModel> getParameters() {
    List<HaxeParameterModel> _parameters = new ArrayList<HaxeParameterModel>();
    HaxeParameterList parameterList = UsefulPsiTreeUtil.getChild(this.haxeMethod, HaxeParameterList.class);
    if (parameterList != null) {
      for (HaxeParameter parameter : parameterList.getParameterList()) {
        _parameters.add(new HaxeParameterModel(parameter));
      }
    }
    return _parameters;
  }

  public int getParameterCount() {
    HaxeParameterList parameterList = UsefulPsiTreeUtil.getChild(this.haxeMethod, HaxeParameterList.class);
    return null == parameterList ? 0 : parameterList.getParametersCount();
  }

  public List<HaxeParameterModel> getParametersWithContext(@Nullable HaxeMethodContext context) {
    List<HaxeParameterModel> params = getParameters();
    if (context!= null && context.isExtensionMethod()) {
      params = new ArrayList<>(params);
      params.remove(0);
    }
    return params;
  }

  @Nullable
  public HaxeTypeTag getReturnTypeTagPsi() {
    return UsefulPsiTreeUtil.getChild(this.haxeMethod, HaxeTypeTag.class);
  }

  public PsiElement getReturnTypeTagOrNameOrBasePsi() {
    HaxeTypeTag psi = getReturnTypeTagPsi();
    return (psi != null) ? psi : getNameOrBasePsi();
  }

  public String getFullName() {
    HaxeClassModel classModel = this.getDeclaringClass();
    if(classModel != null) {
      return classModel.getName() + "." + this.getName();
    }else {
      return  this.getName();
    }
  }

  public boolean isConstructor() {
    return this.getName().equals(HaxeTokenTypes.ONEW.toString());
  }

  public boolean isStaticInit() {
    return this.getName().equals("__init__");
  }

  public boolean isArrayAccessor() {
    return HaxeMetadataUtils.hasMeta(getBasePsi(), HaxeMeta.ARRAY_ACCESS);
  }

  @Override
  public String getPresentableText(HaxeMethodContext context) {
    return getPresentableText(context, null);
  }
  @Override
  public String getPresentableText(@Nullable HaxeMethodContext context, @Nullable HaxeGenericResolver resolver) {
    StringBuilder out = new StringBuilder();
    out.append(this.getName());
    out.append("(");
    int index = 0;
    for (HaxeParameterModel param : this.getParametersWithContext(context)) {
      if (index > 0) out.append(", ");
      out.append(param.getPresentableText(resolver));
      index++;
    }
    out.append(")");
    if (!isConstructor()) {
      out.append(":").append(getResultType(resolver));
    }
    return out.toString();
  }

  @Deprecated // Delete ASAP
  public SpecificFunctionReference getFunctionType() {
    // WARNING: If your calling function is failing dealing with type parameters, then you need to call the version that takes a resolver.
    return getFunctionType(null);
  }

  public ResultHolder getReturnType(@Nullable HaxeGenericResolver resolver) {
    // attempt att caching returnType for methods that does not change by resolver or parameters



    if ((resolver == null || resolver.isEmpty()) // must not use resolver
        && haxeMethod.getReturnType() !=null // must have type tag
        && haxeMethod.getGenericParam() != null) { // must not have generics
      return CachedValuesManager.getProjectPsiDependentCache(haxeMethod,  HaxeMethodModel::getReturnTypeCacheProvider);
    }else {
      return HaxeTypeResolver.getFieldOrMethodReturnType(haxeMethod, resolver);

      //TODO this way of caching seems to be unreliable for some reason

      //Boolean data = haxeMethod.getUserData(isVoidReturn);
      //if (data == Boolean.TRUE) {
      //  return SpecificHaxeClassReference.getVoid(haxeMethod).createHolder();
      //}else {
      //  ResultHolder type = HaxeTypeResolver.getFieldOrMethodReturnType(haxeMethod, resolver);
      //  if(type.isVoid() && haxeMethod instanceof AbstractHaxeNamedComponent component) {
      //    component.registerCacheKey(isVoidReturn);
      //    component.putUserData(isVoidReturn, Boolean.TRUE);
      //  }
      //  return type;
      //}
    }
  }

  private static ResultHolder getReturnTypeCacheProvider(HaxeMethod haxeMethod) {
    return HaxeTypeResolver.getFieldOrMethodReturnType(haxeMethod, null);
  }

  public SpecificFunctionReference getFunctionType(@Nullable HaxeGenericResolver resolver) {
    LinkedList<HaxeArgument> args = new LinkedList<>();
    List<HaxeParameterModel> parameters = this.getParameters();
    for (int i = 0; i < parameters.size(); i++) {
      HaxeParameterModel param = parameters.get(i);
      args.add(new HaxeArgument(i, param.isOptional(), param.isRest(), param.getType(resolver), param.getName()));
    }
    return new SpecificFunctionReference(args, getReturnType(resolver), this, haxeMethod);
  }

  public HaxeMethodModel getParentMethod(@Nullable HaxeGenericResolver resolver) {
    final HaxeClassModel aClass = getDeclaringClass().getParentClass();
    return (aClass != null) ? aClass.getMethod(this.getName(), resolver) : null;
  }

  public boolean isAbstract() {
    return hasModifier(HaxePsiModifier.ABSTRACT);
  }
  public boolean isMacro() {
    return hasModifier(HaxePsiModifier.MACRO) || hasModifier(HaxePsiModifier.MACRO2);
  }

  @Override
  public String toString() {
    String parameters = null;
    try {
      parameters = this.getParameters().toString();
    } catch (ProcessCanceledException e) {
      parameters = "?";
    }
    return "HaxeMethodModel(" + this.name + ", " + parameters + ")";
  }

  @Override
  public List<HaxeModel> getExposedMembers() {
    return null;
  }

  @Nullable
  @Override
  public HaxeExposableModel getExhibitor() {
    return getDeclaringClass();
  }

  @NotNull
  public List<HaxeGenericParamModel> getGenericParams() {
    final List<HaxeGenericParamModel> out = new ArrayList<>();
    if (haxeMethod.getGenericParam() != null) {
      int index = 0;
      for (HaxeGenericListPart part : haxeMethod.getGenericParam().getGenericListPartList()) {
        out.add((HaxeGenericParamModel)part.getModel());
        index++;
      }
    }
    return out;
  }

  /**
   * Get a generic resolver for *this* method.  Does NOT include parent entries.
   * @param parentResolver - To resolve parent names to type constraints.
   * @return A resolver that has all of the types that this method declares/uses.
   */
  @NotNull
  public HaxeGenericResolver getGenericResolver(HaxeGenericResolver parentResolver) {
    HaxeGenericResolver resolver = new HaxeGenericResolver();
    if (haxeMethod.getGenericParam() != null) {
      for (HaxeGenericListPart part : haxeMethod.getGenericParam().getGenericListPartList()) {
        HaxeGenericParamModel model = part.getModel();
        ResultHolder constraint = model.getConstraint(parentResolver);
        if (null == constraint) {
          constraint = new ResultHolder(SpecificTypeReference.getUnknown(getBasePsi()));
        }
        resolver.addConstraint(model.getTypeParameter(), constraint);
      }
    }
    return resolver;
  }

  public boolean HasNoUsingMeta() {
    return HaxeMetadataUtils.hasMeta(getBasePsi(), HaxeMeta.NO_USING);
  }

  //// TODO make it support optional args and varargs
  //// attempts at evaluating generic resolver values when arguments can override
  //public HaxeGenericResolver getGenericResolver(HaxeGenericResolver parentResolver, List<ResultHolder> arguments) {
  //  HaxeGenericResolver genericResolver = getGenericResolver(parentResolver);
  //  List<HaxeParameterModel> parameters = this.getParameters();
  //  TypeParameterTable typeParamTable = createTypeParameterConstraintTable(haxeMethod, genericResolver, true);
  //  for (int i = 0; i < parameters.size(); i++) {
  //    if (i >= arguments.size()) break;
  //
  //    HaxeParameterModel parameter = parameters.get(i);
  //    ResultHolder param = parameter.getType();
  //    ResultHolder arg = arguments.get(i);
  //    // not including implicit cast here as it easily cause stack overflows
  //    boolean canAssign = HaxeTypeCompatible.canAssignToFrom(param.getType(), arg.getType(), false, null);
  //    if (canAssign) {
  //      genericResolver.translateFromTo(param.getType(), arg.getType())
  //      return findTypeParametersToInherit(param.getType(), arg.getType(), genericResolver, typeParamTable);
  //    }
  //  }
  //
  //  return genericResolver;
  //}
}

