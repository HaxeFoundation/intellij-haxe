/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

public class HaxeParameterModel extends HaxeMemberModel {

  final private HaxeParameter parameter;
  final private HaxeMemberModel memberModel;
  final private boolean optional;

  public HaxeParameterModel(HaxeParameter parameter) {
    super(parameter);

    this.parameter = parameter;
    this.optional = parameter.getOptionalMark() != null;

    final PsiMember parentPsi = PsiTreeUtil.getParentOfType(parameter, HaxeEnumValueDeclaration.class, HaxeMethod.class);
    if (parentPsi instanceof HaxeMethod) {
      memberModel = ((HaxeMethod)parentPsi).getModel();
    }
    else if (parentPsi instanceof HaxeEnumValueDeclaration) {
      memberModel = new HaxeFieldModel((HaxePsiField)parentPsi);
    }
    else {
      memberModel = null;
    }
  }

  public PsiElement getContextElement() {
    return getNameOrBasePsi();
  }

  public PsiElement getOptionalPsi() {
    return UsefulPsiTreeUtil.getToken(parameter, "?");
  }

  public boolean hasOptionalPsi() {
    return this.optional;
  }

  public boolean isOptional() {
    return this.hasOptionalPsi() || this.hasInit();
  }

  public boolean hasInit() {
    return getVarInitPsi() != null;
  }

  public HaxeVarInit getVarInitPsi() {
    return UsefulPsiTreeUtil.getChild(parameter, HaxeVarInit.class);
  }

  public HaxeTypeTag getTypeTagPsi() {
    return parameter.getTypeTag();
  }

  public ResultHolder getType() {
    return getType(null);
  }

  public ResultHolder getType(@Nullable HaxeGenericResolver resolver) {
    if (resolver != null) {
      ResultHolder typeResult = getType(null);
      ResultHolder resolved = resolver.resolve(typeResult.getType().toStringWithoutConstant());
      if (resolved != null) return resolved;
    }
    return HaxeTypeResolver.getTypeFromTypeTag(getTypeTagPsi(), this.getContextElement());
  }

  public PsiParameter getParameter() {
    return parameter;
  }

  public HaxeMemberModel getMemberModel() {
    return memberModel;
  }

  public String getPresentableText() {
    String out = hasOptionalPsi() ? "?" : "";
    out += getName();
    out += ":";
    out += getType().toStringWithoutConstant();
    return out;
  }

  @Override
  public HaxeComponentName getNamePsi() {
    return parameter.getComponentName();
  }

  @Override
  public HaxeDocumentModel getDocument() {
    return memberModel.getDocument();
  }

  @Override
  public HaxeClassModel getDeclaringClass() {
    return memberModel.getDeclaringClass();
  }

  @Override
  public ResultHolder getResultType() {
    final HaxeTypeTag typeTag = parameter.getTypeTag();
    final HaxeTypeOrAnonymous type = typeTag != null ? typeTag.getTypeOrAnonymous() : null;
    return type != null ? HaxeTypeResolver.getTypeFromTypeOrAnonymous(type) : null;
  }

  @Override
  public String getPresentableText(HaxeMethodContext context) {
    final ResultHolder type = getResultType();
    return type == null ? this.getName() : this.getName() + ":" + type;
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
      }
      else if (nextPsi != null && nextPsi.getText().equals(",")) {
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
}
