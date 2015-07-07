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
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeParameter;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeTag;
import com.intellij.plugins.haxe.lang.psi.HaxeVarInit;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.Nullable;

public class HaxeParameterModel {
  private HaxeParameter parameter;
  private HaxeMethodModel method;
  private boolean optional;
  private String _name = null;

  public HaxeParameterModel(HaxeParameter parameter, HaxeMethodModel method) {
    this.parameter = parameter;
    this.method = method;
    this.optional = UsefulPsiTreeUtil.getToken(parameter, "?") != null;
  }

  private HaxeDocumentModel getDocument() {
    return method.getDocument();
  }

  public String getName() {
    if (_name == null) {
      PsiElement nameElement = getNamePsi();
      _name = (nameElement != null) ? nameElement.getText() : "";
    }
    return _name;
  }

  public PsiElement getPsi() {
    return this.parameter;
  }

  public PsiElement getNamePsi() {
    return UsefulPsiTreeUtil.getChild(parameter, HaxeComponentName.class);
  }

  public PsiElement getNameOrBasePsi() {
    PsiElement result = getNamePsi();
    if (result == null) result = this.parameter;
    return result;
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

  public HaxeMethodModel getMethod() {
    return method;
  }

  public String getPresentableText() {
    String out = "";
    out += getName();
    out += ":";
    out += getType().toStringWithoutConstant();
    return out;
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
