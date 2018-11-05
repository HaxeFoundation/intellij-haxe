/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Eric Bishton
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

import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.model.type.SpecificFunctionReference.Argument;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class HaxeMethodModel extends HaxeMemberModel implements HaxeExposableModel {
  private HaxeMethod haxeMethod;

  public HaxeMethodModel(HaxeMethod haxeMethod) {
    super(haxeMethod);
    this.haxeMethod = haxeMethod;
  }

  @Override
  public PsiElement getBasePsi() {
    return haxeMethod;
  }

  public HaxeMethodPsiMixin getMethodPsi() {
    return haxeMethod;
  }

  public PsiElement getBodyPsi() {
    PsiElement[] children = haxeMethod.getChildren();
    if (children.length == 0) return null;
    return children[children.length - 1];
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

  public List<HaxeParameterModel> getParametersWithContext(HaxeMethodContext context) {
    List<HaxeParameterModel> params = getParameters();
    if (context.isExtensionMethod()) {
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
    return this.getDeclaringClass().getName() + "." + this.getName();
  }

  public boolean isConstructor() {
    return this.getName().equals(HaxeTokenTypes.ONEW.toString());
  }

  public boolean isStaticInit() {
    return this.getName().equals("__init__");
  }

  public boolean isArrayAccessor() {
    // Would be nice if this worked, but it won't until the lexer and/or parser stops using MACRO_ID:
    //   return null != UsefulPsiTreeUtil.getChild(this.haxeMethod, HaxeArrayAccessMeta.class);
    for (HaxeCustomMeta meta : UsefulPsiTreeUtil.getChildren(this.getMethodPsi(), HaxeCustomMeta.class)) {
      if ("@:arrayAccess".equals(meta.getText())) {
        return true;
      }
    }
    return false;
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
    if (!isConstructor()) {
      out += ":" + getResultType();
    }
    return out;
  }

  public SpecificFunctionReference getFunctionType() {
    return getFunctionType(null);
  }

  public ResultHolder getReturnType(@Nullable HaxeGenericResolver resolver) {
    return HaxeTypeResolver.getFieldOrMethodReturnType((AbstractHaxeNamedComponent)this.getBasePsi(), resolver);
  }

  public SpecificFunctionReference getFunctionType(@Nullable HaxeGenericResolver resolver) {
    LinkedList<Argument> args = new LinkedList<>();
    List<HaxeParameterModel> parameters = this.getParameters();
    for (int i = 0; i < parameters.size(); i++) {
      HaxeParameterModel param = parameters.get(i);
      args.add(new Argument(i, param.isOptional(), param.getType(resolver), param.getName()));
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

  @Override
  public List<HaxeModel> getExposedMembers() {
    return null;
  }

  @Nullable
  @Override
  public HaxeExposableModel getExhibitor() {
    return getDeclaringClass();
  }

  @Nullable
  public List<HaxeGenericParamModel> getGenericParams() {
    final List<HaxeGenericParamModel> out = new ArrayList<>();
    if (haxeMethod.getGenericParam() != null) {
      int index = 0;
      for (HaxeGenericListPart part : haxeMethod.getGenericParam().getGenericListPartList()) {
        out.add(new HaxeGenericParamModel(part, index));
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
        HaxeGenericParamModel model = new HaxeGenericParamModel(part, 0);
        ResultHolder constraint = model.getConstraint(parentResolver);
        if (null == constraint) {
          constraint = new ResultHolder(SpecificTypeReference.getUnknown(getBasePsi()));
        }
        resolver.add(model.getName(), constraint );
      }
    }
    return resolver;
  }
}

