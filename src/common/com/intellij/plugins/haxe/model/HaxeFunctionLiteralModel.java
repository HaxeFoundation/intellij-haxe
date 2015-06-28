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

import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeFunctionLiteralImpl;
import com.intellij.plugins.haxe.model.resolver.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeFunctionLiteralModel implements HaxeFunctionModel {
  private HaxeFunctionLiteralImpl literal;

  public HaxeFunctionLiteralModel(HaxeFunctionLiteralImpl literal) {
    this.literal = literal;
  }

  @Nullable
  public PsiElement getBodyPsi() {
    return HaxeFunctionModelUtils.getBodyPsi(literal);
  }

  public HaxeMethodModel getDeclaringMethod() {
    return UsefulPsiTreeUtil.getAncestor(literal, HaxeMethod.class).getModel();
  }

  public HaxeClassModel getDeclaringClass() {
    return getDeclaringMethod().getDeclaringClass();
  }

  @Override
  public HaxeParametersModel getParameters() {
    return HaxeParametersModel.fromHaxeParameterList(this, literal.getParameterList(), false);
  }

  @Override
  public HaxeResolver2 getResolver(@NotNull HaxeFileModel referencedInFile) {
    HaxeResolver2Class classResolver = this.getDeclaringClass().getResolver(isStatic(), referencedInFile);
    HaxeResolver2Parameters parameterResolvers = getParameters().getResolver();
    return new HaxeResolver2Locals(new HaxeResolver2Combined(classResolver, parameterResolvers));
  }

  @Override
  public boolean isStatic() {
    return getDeclaringMethod().isStatic();
  }
}
