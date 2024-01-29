/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2019 Eric Bishton
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

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference.propagateGenericsToType;

/**
 * HaxeParameterModels (parameter) appear in method and catch parameters -- not type parameters.
 */
@EqualsAndHashCode
public class HaxeParameterModel extends HaxeBaseMemberModel implements HaxeModel {
  private static final Key<HaxeMemberModel> PARAMETER_MEMBER_MODEL_KEY = new Key<>("HAXE_PARAMETER_MEMBER_MODEL");

  public HaxeParameterModel(HaxeParameter parameter) {
    super(parameter);
  }

  public HaxeParameter getParameterPsi() {
    return (HaxeParameter)basePsi;
  }

  @Override
  public String getName() {
    return getParameterPsi().getName();
  }

  public PsiElement getContextElement() {
    return getBasePsi();
  }

  public PsiElement getOptionalPsi() {
    return getParameterPsi().getOptionalMark();
  }

  public boolean hasOptionalPsi() {
    return getOptionalPsi() != null;
  }

  public boolean isOptional() {
    return this.hasOptionalPsi() || this.hasInit();
  }

  public boolean isRest() {
    return basePsi instanceof  HaxeRestParameter;
  }

  public boolean hasInit() {
    return getVarInitPsi() != null;
  }

  public HaxeVarInit getVarInitPsi() {
    return getParameterPsi().getVarInit();
  }

  public HaxeTypeTag getTypeTagPsi() {
    return CachedValuesManager.getCachedValue(basePsi, () -> new CachedValueProvider.Result<>(getParameterPsi().getTypeTag(), basePsi));
    //return getParameterPsi().getTypeTag();
  }

  private ResultHolder typeReplacement;// allow us to replace ExprOf<T> with T

  // TODO mlo find a new way to cache type
  @NotNull
  public ResultHolder getType() {
    if (typeReplacement != null) {
      return typeReplacement.duplicate();
    }
    ResultHolder type = null;
    HaxeTypeTag psi = getTypeTagPsi();
    if (psi != null) {
      type = HaxeTypeResolver.getTypeFromTypeTag(psi, this.getContextElement());
      //caching when we know there's no generics involved
      if (!type.isTypeParameter() && type.getClassType() != null && type.getClassType().getSpecifics().length == 0) {
        if (psi.textMatches(type.getType().toString())) { // make sure we are not caching a resolved value (ex. param:T being resolved to param:String)
          typeReplacement = type;
        }
      }
    }else if (this.getVarInitPsi() != null) {
      type = HaxeExpressionEvaluator.evaluate(this.getVarInitPsi(), null).result;
    }else {
      type = new ResultHolder(SpecificHaxeClassReference.getUnknown(this.basePsi));
    }
    return type;
  }

  @NotNull
  public ResultHolder getType(@Nullable HaxeGenericResolver resolver) {
    ResultHolder typeResult = getType();
    if (resolver != null) {
      SpecificTypeReference type = typeResult.getType();
      if(type instanceof SpecificHaxeClassReference classReference) {
        propagateGenericsToType(classReference, resolver);
      }
      else if(type instanceof SpecificFunctionReference functionReference) {
      // TODO propagate to functionTypes
        //propagateGenericsToType(classReference, resolver);
      }

      if(type instanceof SpecificHaxeClassReference classReference) {
        ResultHolder resolved = resolver.resolve(classReference.getClassName());
        if (resolved != null) return resolved;
      }

    }
    return typeResult;
  }

  public HaxeMemberModel getMemberModel() {
    HaxeMemberModel model = getBasePsi().getUserData(PARAMETER_MEMBER_MODEL_KEY);
    if (model == null) {
      final PsiMember parentPsi = PsiTreeUtil.getParentOfType(getBasePsi(), HaxeEnumValueDeclaration.class, HaxeMethod.class);
      if (parentPsi instanceof HaxeMethod) {
        model = ((HaxeMethod)parentPsi).getModel();
      } else if (parentPsi instanceof HaxeEnumValueDeclaration haxeEnumValueDeclaration) {
        model = (HaxeFieldModel) haxeEnumValueDeclaration.getModel();
      }
      if (model != null) {
        getBasePsi().putUserData(PARAMETER_MEMBER_MODEL_KEY, model);
      }
    }
    return model;
  }

  public String getPresentableText(@Nullable HaxeGenericResolver resolver) {
    String out = hasOptionalPsi() ? "?" : "";
    out += getName();
    out += ":";
    out += getType(resolver).toStringWithoutConstant();
    return out;
  }

  @Override
  public HaxeComponentName getNamePsi() {
    return getParameterPsi().getComponentName();
  }

  @Override
  @NotNull
  public HaxeDocumentModel getDocument() {
    return getMemberModel().getDocument();
  }

  @Override
  public HaxeClassModel getDeclaringClass() {
    return getMemberModel().getDeclaringClass();
  }

  @Override
  public ResultHolder getResultType() {
    final HaxeTypeTag typeTag = getTypeTagPsi();
    final HaxeTypeOrAnonymous type = typeTag != null ? typeTag.getTypeOrAnonymous() : null;
    ResultHolder holder = type != null ? HaxeTypeResolver.getTypeFromTypeOrAnonymous(type) : null;
    if (holder != null) return holder;

    HaxeVarInit initPsi = getVarInitPsi();
    if (initPsi == null)  return  SpecificHaxeClassReference.getUnknown(getParameterPsi()).createHolder();
    return HaxeExpressionEvaluator.evaluate(initPsi, new HaxeExpressionEvaluatorContext(initPsi), null).result;
  }

  @Override
  public String getPresentableText(HaxeMethodContext context) {
    return getPresentableText(context, null);
  }
  @Override
  public String getPresentableText(HaxeMethodContext context, HaxeGenericResolver resolver) {
    final ResultHolder type = getResultType(resolver);
    if (type == null) {
      return this.getName();
    }else {
      if (!type.isUnknown()) {
        return this.getName() + ":" + type;
      }else {
        return this.getName() + ":" + this.getType();
      }
    }
  }

  public void remove() {
    PsiElement psi = getBasePsi();
    if (psi != null) {
      PsiElement prePsi = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpaces(psi, true);
      PsiElement nextPsi = UsefulPsiTreeUtil.getNextSiblingNoSpaces(psi);
      TextRange range = psi.getTextRange();
      StripSpaces stripSpaces = StripSpaces.NONE;

      if (prePsi != null && prePsi.getText().equals(",")) {
        range = range.union(prePsi.getTextRange());
        stripSpaces = StripSpaces.BEFORE;
      } else if (nextPsi != null && nextPsi.getText().equals(",")) {
        range = range.union(nextPsi.getTextRange());
        stripSpaces = StripSpaces.AFTER;
      }
      getDocument().replaceElementText(range, "", stripSpaces);
    }
  }

  @Nullable
  @Override
  public HaxeExposableModel getExhibitor() {
    return getMemberModel() instanceof HaxeExposableModel ? (HaxeExposableModel)getMemberModel() : null;
  }

  @Nullable
  @Override
  public FullyQualifiedInfo getQualifiedInfo() {
    return null;
  }

  public HaxeParameterModel replaceType(ResultHolder type) {
    HaxeParameterModel model = new HaxeParameterModel(getParameterPsi());
    model.typeReplacement = type;
    return model;
  }
}
