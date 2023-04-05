/*
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
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.plugins.haxe.lang.psi.impl.HaxeAbstractDeclarationImpl;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeFunctionArgumentImpl;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HaxeSpecificFunction extends HaxeAbstractDeclarationImpl implements HaxeFunctionType {

  public class Argument extends HaxeFunctionArgumentImpl {
    final private HaxeParameter parameter;

    public Argument(@NotNull HaxeParameter parameter) {
      super(parameter.getNode());
      this.parameter = parameter;
    }

    @NotNull
    public ResultHolder getType() {
      return HaxeTypeResolver.getTypeFromTypeTag(parameter.getTypeTag(), parameter);
    }

    @Nullable
    @Override
    public HaxeComponentName getComponentName() {
      return parameter.getComponentName();
    }

    @Nullable
    @Override
    public HaxeFunctionType getFunctionType() {
      HaxeTypeTag tag = parameter.getTypeTag();
      return tag != null ? tag.getFunctionType() : null;
    }

    @Nullable
    @Override
    public HaxeOptionalMark getOptionalMark() {
      return super.getOptionalMark();
    }

    public boolean isOptional() {
      return super.getOptionalMark() != null;
    }

    @Nullable
    @Override
    public HaxeTypeOrAnonymous getTypeOrAnonymous() {
      HaxeTypeTag typeTag = parameter.getTypeTag();
      return null != typeTag ? typeTag.getTypeOrAnonymous() : null;
    }
  }

  final private HaxeGenericSpecialization specialization;
  final private HaxeAbstractDeclaration psiClass;
  final private HaxeFunctionType functionType;
  final private HaxeMethod method;

  private HaxeSpecificFunction(@NotNull HaxeAbstractDeclaration psiClass,
                               @Nullable HaxeMethod method,
                               @Nullable HaxeFunctionType functionType,
                               @NotNull HaxeGenericSpecialization specialization) {
    super(psiClass.getNode());
    this.psiClass = psiClass;
    this.specialization = specialization;
    this.functionType = functionType;
    this.method = method;
  }

  public HaxeSpecificFunction(@NotNull HaxeAbstractDeclaration psiClass,
                              @NotNull HaxeMethod method,
                              @NotNull HaxeGenericSpecialization specialization) {
    this(psiClass, method, null, specialization);
  }

  public HaxeSpecificFunction(@NotNull HaxeAbstractDeclaration psiClass,
                              @NotNull HaxeFunctionType functionType,
                              @NotNull HaxeGenericSpecialization specialization) {
    this(psiClass, null, functionType, specialization);
  }

  public HaxeSpecificFunction(@NotNull HaxeFunctionType functionType, @NotNull HaxeGenericSpecialization specialization) {
    this(getFunctionClass(functionType), functionType, specialization);
  }

  public HaxeSpecificFunction(@NotNull HaxeMethod method, @NotNull HaxeGenericSpecialization specialization) {
    this(getFunctionClass(method), method, specialization);
  }

  private static HaxeAbstractDeclaration getFunctionClass(PsiElement context) {
    return (HaxeAbstractDeclaration)SpecificHaxeClassReference.getFunction(context).getHaxeClass();
  }

  public HaxeGenericSpecialization getSpecialization() {
    return specialization;
  }

  @NotNull
  @Override
  public List<HaxeFunctionArgument> getFunctionArgumentList() {
    if (null != functionType) {
      return functionType.getFunctionArgumentList();
    }

    // Convert the method parameters into a function argument list.
    List<HaxeFunctionArgument> args = new ArrayList<>();
    for (PsiParameter param : method.getParameterList().getParameters()) {
      args.add(new Argument((HaxeParameter)param));
    }
    return args;
  }

  @Nullable
  @Override
  public HaxeFunctionReturnType getFunctionReturnType() {
    return null != functionType ? functionType.getFunctionReturnType()
                                : (HaxeFunctionReturnType)method.getReturnType();
  }

  @Nullable
  public HaxeFunctionType getFunctionType() {
    return functionType;
  }

  @Nullable
  public HaxeMethod getMethod() {
    return method;
  }
}
