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

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodPsiMixin;
import com.intellij.plugins.haxe.lang.psi.HaxeParameterList;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeTag;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.model.resolver.*;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificFunctionReference;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;

public class HaxeMethodModel extends HaxeMemberModel implements HaxeFunctionModel {
  private HaxeMethodPsiMixin haxeMethod;
  private boolean isExtensionMethod;

  public HaxeMethodModel(HaxeMethodPsiMixin haxeMethod) {
    this(haxeMethod, false);
  }

  public HaxeMethodModel(HaxeMethodPsiMixin haxeMethod, boolean isExtensionMethod) {
    super(haxeMethod, haxeMethod, haxeMethod);
    this.haxeMethod = haxeMethod;
    this.isExtensionMethod = isExtensionMethod;
  }

  public HaxeMethodModel asExtensionMethod() {
    return new HaxeMethodModel(haxeMethod, true);
  }

  public HaxeMethodPsiMixin getMethodPsi() {
    return haxeMethod;
  }

  @Nullable
  public PsiElement getBodyPsi() {
    return HaxeFunctionModelUtils.getBodyPsi(haxeMethod);
  }

  //private List<HaxeParameterModel> _parameters;
  public HaxeParametersModel getParameters() {
    HaxeParameterList params = UsefulPsiTreeUtil.getChild(this.haxeMethod, HaxeParameterList.class);
    return HaxeParametersModel.fromHaxeParameterList(this, params, isExtensionMethod);
  }

  @Nullable
  public HaxeParameterModel getParameter(int index) {
    return getParameters().get(index);
  }

  @Nullable
  public HaxeTypeTag getReturnTypeTagPsi() {
    return UsefulPsiTreeUtil.getChild(this.haxeMethod, HaxeTypeTag.class);
  }

  public PsiElement getReturnTypeTagOrNameOrBasePsi() {
    HaxeTypeTag psi = getReturnTypeTagPsi();
    return (psi != null) ? psi : getNameOrBasePsi();
  }

  @Override
  public ResultHolder getMemberType() {
    return getFunctionType().createHolder();
  }

  public String getFullName() {
    return this.getDeclaringClass().getName() + "." + this.getName();
  }

  public boolean isConstructor() {
    return this.getName().equals("new");
  }

  public boolean isStaticInit() {
    return this.getName().equals("__init__");
  }

  @Override
  public String getPresentableText() {
    String out = "";
    out += this.getName();
    out += "(";
    int index = 0;
    for (HaxeParameterModel param : this.getParameters()) {
      if (index > 0) out += ", ";
      out += param.getPresentableText();
      index++;
    }
    out += ")";
    if (!isConstructor()) {
      out += ":" + getResultType();
    }
    return out;
  }

  public SpecificFunctionReference getFunctionType() {
    return getFunctionType(null);
  }

  @NotNull
  public ResultHolder getReturnType(@Nullable HaxeGenericResolver resolver) {
    return HaxeTypeResolver.getFieldOrMethodReturnType((AbstractHaxeNamedComponent)this.getPsi(), resolver);
  }

  public SpecificFunctionReference getFunctionType(@Nullable HaxeGenericResolver resolver) {
    LinkedList<ResultHolder> args = new LinkedList<ResultHolder>();
    for (HaxeParameterModel param : this.getParameters()) {
      args.add(param.getType(resolver));
    }
    return new SpecificFunctionReference(args, getReturnType(resolver), this, haxeMethod);
  }

  public HaxeMethodModel getParentMethod() {
    final HaxeClassModel aClass = getDeclaringClass().getParentClass();
    return (aClass != null) ? aClass.getMethod(this.getName()) : null;
  }

  @Override
  public String toString() {
    return "HaxeMethodModel(" + this.getName() + ", " + this.getParameters() + ")";
  }

  public HaxeResolver2 getResolver(@NotNull HaxeFileModel referencedInFile) {
    HaxeResolver2Class classResolver = this.getDeclaringClass().getResolver(isStatic(), referencedInFile);
    HaxeResolver2Parameters parameterResolvers = getParameters().getResolver();
    return new HaxeResolver2Locals(new HaxeResolver2Combined(classResolver, parameterResolvers));
  }
}

