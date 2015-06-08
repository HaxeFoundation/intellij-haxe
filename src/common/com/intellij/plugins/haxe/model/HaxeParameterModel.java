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

import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeParameter;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeTag;
import com.intellij.plugins.haxe.lang.psi.HaxeVarInit;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.plugins.haxe.util.HaxePsiUtils;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiParameter;

public class HaxeParameterModel {
  private HaxeParameter parameter;
  private HaxeMethodModel method;
  private boolean optional;
  private String _name = null;

  public HaxeParameterModel(HaxeParameter parameter, HaxeMethodModel method) {
    this.parameter = parameter;
    this.method = method;
    this.optional = HaxePsiUtils.getToken(parameter, "?") != null;
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
    return HaxePsiUtils.getChild(parameter, HaxeComponentName.class);
  }

  public PsiElement getNameOrBasePsi() {
    PsiElement result = getNamePsi();
    if (result == null) result = this.parameter;
    return result;
  }

  public PsiElement getOptionalPsi() {
    return HaxePsiUtils.getToken(parameter, "?");
  }

  public boolean isOptional() {
    return this.optional;
  }

  public boolean isOptionalOrHasInit() {
    return this.isOptional() || this.hasInit();
  }

  public boolean hasInit() {
    return getVarInitPsi() != null;
  }

  public HaxeVarInit getVarInitPsi() {
    return HaxePsiUtils.getChild(parameter, HaxeVarInit.class);
  }

  public HaxeTypeTag getTypeTagPsi() {
    return parameter.getTypeTag();
  }

  public SpecificTypeReference getType() {
    return HaxeTypeResolver.getTypeFromTypeTag(getTypeTagPsi());
  }

  public PsiParameter getParameter() {
    return parameter;
  }

  public HaxeMethodModel getMethod() {
    return method;
  }
}
