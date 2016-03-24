/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.Nullable;

public class HaxeParameterModel extends HaxeMemberModel {

  private HaxeParameter parameter;
  private HaxeMethodModel methodModel;
  private boolean optional;

  public HaxeParameterModel(HaxeParameter parameter) {
    super(parameter, parameter, parameter);
    this.parameter = parameter;
    final HaxeMethod method = (HaxeMethod) UsefulPsiTreeUtil.getParentOfType(parameter, HaxeMethod.class);
    this.methodModel = method != null ? method.getModel() : null;
    this.optional = UsefulPsiTreeUtil.getToken(parameter, "?") != null;
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

  public HaxeMethodModel getMethodModel() {
    return methodModel;
  }

  public String getPresentableText() {
    String out = "";
    out += getName();
    out += ":";
    out += getType().toStringWithoutConstant();
    return out;
  }

  @Override
  public PsiElement getNamePsi() {
    return UsefulPsiTreeUtil.getChild(parameter, HaxeComponentName.class);
  }

  @Override
  public HaxeDocumentModel getDocument() {
    return methodModel.getDocument();
  }

  @Override
  public HaxeClassModel getDeclaringClass() {
    return methodModel.getDeclaringClass();
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
    PsiElement psi = getPsi();
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
}
