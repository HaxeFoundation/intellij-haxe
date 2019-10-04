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

import com.intellij.plugins.haxe.lang.psi.impl.HaxeAbstractClassDeclarationImpl;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeSpecificFunction extends HaxeAbstractClassDeclarationImpl implements HaxeFunctionType {

  private HaxeFunctionType functionType;
  private HaxeGenericSpecialization specialization;

  public HaxeSpecificFunction(@NotNull HaxeAbstractClassDeclaration psiClass, @NotNull HaxeFunctionType functionType, @NotNull HaxeGenericSpecialization specialization) {
    super(psiClass.getNode());
    this.functionType = functionType;
    this.specialization = specialization;
  }

  public HaxeSpecificFunction(@NotNull HaxeFunctionType functionType, @NotNull HaxeGenericSpecialization specialization) {
    this(getFunctionClass(functionType), functionType, specialization);
  }

  private static HaxeAbstractClassDeclaration getFunctionClass(HaxeFunctionType context) {
    return (HaxeAbstractClassDeclaration)SpecificHaxeClassReference.getFunction(context);
  }

  public HaxeFunctionType getFunctionType() {
    return functionType;
  }

  public HaxeGenericSpecialization getSpecialization() {
    return specialization;
  }

  @NotNull
  @Override
  public List<HaxeFunctionArgument> getFunctionArgumentList() {
    return functionType.getFunctionArgumentList();
  }

  @Nullable
  @Override
  public HaxeFunctionReturnType getFunctionReturnType() {
    return functionType.getFunctionReturnType();
  }

}
