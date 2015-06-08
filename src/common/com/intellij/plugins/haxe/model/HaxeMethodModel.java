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

import com.intellij.navigation.ItemPresentation;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxePsiUtils;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HaxeMethodModel extends HaxeMemberModel {
  private HaxeMethodPsiMixin haxeMethod;

  public HaxeMethodModel(HaxeMethodPsiMixin haxeMethod) {
    super(haxeMethod, haxeMethod, haxeMethod);
    this.haxeMethod = haxeMethod;
  }

  @Override
  public PsiElement getPsi() {
    return haxeMethod;
  }

  public HaxeMethodPsiMixin getMethodPsi() {
    return haxeMethod;
  }



  //private List<HaxeParameterModel> _parameters;
  public List<HaxeParameterModel> getParameters() {
    List<HaxeParameterModel> _parameters = null;
//    if (_parameters == null) {
      HaxeParameterList parameterList = HaxePsiUtils.getChild(this.haxeMethod, HaxeParameterList.class);
      _parameters = new ArrayList<HaxeParameterModel>();
      if (parameterList != null) {
        for (HaxeParameter parameter : parameterList.getParameterList()) {
          _parameters.add(new HaxeParameterModel(parameter, this));
        }
      }
  //  }
    return _parameters;
  }

  public List<HaxeParameterModel> getParametersWithContext(HaxeMethodContext context) {
    List<HaxeParameterModel> params = getParameters();
    if (context.isExtensionMethod()) {
      params = new ArrayList<HaxeParameterModel>(params);
      params.remove(0);
    }
    return params;
  }

  @Nullable public HaxeTypeTag getReturnTypeTagPsi() {
    return HaxePsiUtils.getChild(this.haxeMethod, HaxeTypeTag.class);
  }

  public boolean isStatic() {
    return getModifiers().hasModifier(HaxeModifierType.STATIC);
  }

  private HaxeClassModel _declaringClass = null;
  public HaxeClassModel getDeclaringClass() {
    if (_declaringClass == null) {
      HaxeClass aClass = (HaxeClass)this.haxeMethod.getContainingClass();
      _declaringClass = (aClass != null) ? aClass.getModel() : null;
    }
    return _declaringClass;
  }

  public String getFullName() {
    return this.getDeclaringClass().getName() + "." + this.getName();
  }

  private HaxeDocumentModel _document = null;
  @NotNull
  public HaxeDocumentModel getDocument() {
    if (_document == null) _document = new HaxeDocumentModel(haxeMethod);
    return _document;
  }

  public boolean isConstructor() {
    return this.getName().equals("new");
  }

  public boolean isStaticInit() {
    return this.getName().equals("__init__");
  }

  @Override
  public String getPresentableText(HaxeMethodContext context) {
    String out = "";
    out += this.getName();
    out += "(";
    int index = 0;
    for (HaxeParameterModel param : this.getParametersWithContext(context)) {
      if (index > 0) out += ", ";
      out += param.getPresentableText();
      index++;
    }
    out += ")";
    out += ":" + getResultType();
    return out;
  }

}

