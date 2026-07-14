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
import com.intellij.plugins.haxe.lang.psi.impl.HaxeMethodPsiMixinImpl;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeMethodStub;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.model.type.HaxeArgument;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.intellij.openapi.util.Key;

@EqualsAndHashCode(callSuper = true)
//TODO, not really an  HaxeExposableModel ?
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

  @NotNull
  public List<HaxeMethodModel> getOverloadsFromMeta() {
    if (haxeMethod.hasCompileTimeMetadata(HaxeMetadataCompileTimeMeta.OVERLOAD)) {
      return extractOverloadsForMethod().stream()
              .map(HaxeMethodPsiMixin::getModel)
              .toList();

    }
    return List.of();
  }

  public List<HaxeMethod> extractOverloadsForMethod() {
    HaxeMetadataList metadataList = haxeMethod.getMetadataList(HaxeMetadataCompileTimeMeta.class);
    return metadataList.stream()
            .filter(haxeMeta -> haxeMeta.isType(HaxeMetadataCompileTimeMeta.OVERLOAD))
            .map(HaxeMeta::getContent)
            .map(content -> PsiTreeUtil.findChildOfType(content, HaxeCompiletimeMetaArg.class))
            .filter(Objects::nonNull)
            .map(PsiElement::getFirstChild)
            .filter(psiElement -> psiElement instanceof HaxeMethod)
            .map(HaxeMethod.class::cast)
            .toList();
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
    HaxeParameterList parameterList = PsiTreeUtil.getStubChildOfType(this.haxeMethod, HaxeParameterList.class);
    if (parameterList != null) {
      for (HaxeParameter parameter : parameterList.getParameterList()) {
        _parameters.add(new HaxeParameterModel(parameter));
      }
    }
    return _parameters;
  }

  public int getParameterCount() {
    HaxeParameterList parameterList = PsiTreeUtil.getStubChildOfType(this.haxeMethod, HaxeParameterList.class);
    return null == parameterList ? 0 : parameterList.getParametersCount();
  }

  public List<HaxeParameterModel> getParametersWithContext(@Nullable HaxeMethodContext context) {
    List<HaxeParameterModel> params = getParameters();
    if (context!= null && context.isExtensionMethod()) {
      params = new ArrayList<>(params);
      if(!params.isEmpty()) params.remove(0);
    }
    return params;
  }

  @Nullable
  public HaxeTypeTag getReturnTypeTagPsi() {
    return PsiTreeUtil.getStubChildOfType(this.haxeMethod, HaxeTypeTag.class);
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
    ResultHolder result = CachedValuesManager.getProjectPsiDependentCache(haxeMethod, HaxeMethodModel::getReturnTypeCacheProvider);
    if (resolver != null) {
      ResultHolder resolve = resolver.resolve(result);
      if(resolve != null && resolve.containsUnknownOrUnresolvedTypeParameters()){
        // Special corner-case, might be only for multi-type abstracts ?
        // if we dont have any typeTag the return type is resolved come from an expression, and for abstracts that can be underlying type
        // and in the case of abstract Map(IMap) methods like the "get" method that use underlying type that is an interface so we need to translate
        // our resolver to that interface
        if(haxeMethod instanceof HaxeMethodDeclaration declaration && declaration.getTypeTag() == null) {
          HaxeTypeTag typeTag = PsiTreeUtil.getParentOfType(result.getContext(), HaxeTypeTag.class);
          if(typeTag != null) {
            HaxeMethod sourceMethod = PsiTreeUtil.getParentOfType(typeTag, HaxeMethod.class);
            if(sourceMethod != null) {
              if (haxeMethod.getContainingClass() instanceof HaxeClass originalClass
                  && sourceMethod.getContainingClass() instanceof HaxeClass SourceClass) {
                HaxeGenericResolver sourceResolver = resolver.translateFromTo(originalClass, SourceClass);
                ResultHolder sourceResult = sourceResolver.resolve(result);
                if (sourceResult != null && !sourceResult.isUnknown()) resolve = sourceResult;
              }
            }
          }
        }
      }
      if (resolve != null && !resolve.isUnknown()) result = resolve;
    }
    return result;

  }

  private static ResultHolder getReturnTypeCacheProvider(HaxeMethod haxeMethod) {
    return HaxeTypeResolver.getFieldOrMethodReturnType(haxeMethod, null);
  }

  public SpecificFunctionReference getFunctionType(@Nullable HaxeGenericResolver resolver) {
    LinkedList<HaxeArgument> args = new LinkedList<>();
    List<HaxeParameterModel> parameters = this.getParameters();
    for (int i = 0; i < parameters.size(); i++) {
      HaxeParameterModel param = parameters.get(i);
      args.add(new HaxeArgument(param.getParameterPsi(), i, param.isOptional(), param.isRest(), param.getType(resolver), param.getName()));
    }
    return new SpecificFunctionReference(args, getReturnType(resolver), this, haxeMethod);
  }

  public HaxeMethodModel getParentMethod(@Nullable HaxeGenericResolver resolver) {
    // Module-level functions have no declaring class, so there is no parent method to find.
    final HaxeClassModel declaringClass = getDeclaringClass();
    if (declaringClass == null) return null;
    final HaxeClassModel aClass = declaringClass.getParentClass();
    return (aClass != null) ? aClass.getMethod(this.getName(), resolver) : null;
  }

  public boolean isDynamic() {
    return hasModifier(HaxePsiModifier.DYNAMIC);
  }

  public boolean isAbstract() {
    return hasModifier(HaxePsiModifier.ABSTRACT);
  }

  public boolean isMacro() {
    return hasModifier(HaxePsiModifier.MACRO) || hasModifier(HaxePsiModifier.MACRO_META);
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
      for (HaxeGenericListPart part : haxeMethod.getGenericParam().getGenericListPartList()) {
        out.add(part.getModel());
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
    // TODO add getStubToInterface
    if(haxeMethod instanceof HaxeMethodPsiMixinImpl mixin) {
      HaxeMethodStub stub = mixin.getStub();
      if(stub!= null) {
        return stub.hasMetadata(HaxeCompilerMetadata.NO_USING);
      }
    }

    return HaxeMetadataUtils.hasMeta(getBasePsi(), HaxeMeta.NO_USING);
  }

  @Nullable
  public HaxeModel getParameterWithName(@NotNull String paramName) {
    for (HaxeParameterModel parameter : getParameters()) {
      if (paramName.equals(parameter.getName())) {
        return parameter;
      }
    }
    return null;
  }

  @Nullable
  public HaxeMethodModel getAncestorMethod(@Nullable HaxeGenericResolver resolver) {
    HaxeClassModel declaringClass = getDeclaringClass();
    if(declaringClass == null) return null;
    return declaringClass.getAncestorMethod(getName(), resolver);
  }

    public boolean isMacroMember() {
        return isMacro() && !isStatic();
    }

    public boolean hasGenericParams() {
        return getMethod().getGenericParam() != null;
    }

    // useful for performance optmizations (tells us that we dont neeed to resolve generics)
    public boolean canContainGenerics() {
      boolean methodHasGenerics = hasGenericParams();
      HaxeClassModel declaringClass = getDeclaringClass();
      boolean parentClassHasGenerics =  declaringClass != null  && declaringClass.hasGenericParams();
      return methodHasGenerics || (parentClassHasGenerics && isStatic());
    }
}

