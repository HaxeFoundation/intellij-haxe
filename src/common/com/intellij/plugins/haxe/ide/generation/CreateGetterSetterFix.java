/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.ide.generation;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public class CreateGetterSetterFix extends BaseCreateMethodsFix {
  public static enum Strategy {
    GETTER {
      @Override
      boolean accept(String name, Set<String> names) {
        return !names.contains(HaxePresentableUtil.getterName(name));
      }
    }, SETTER {
      @Override
      boolean accept(String name, Set<String> names) {
        return !names.contains(HaxePresentableUtil.setterName(name));
      }
    }, GETTERSETTER {
      @Override
      boolean accept(String name, Set<String> names) {
        return !names.contains(HaxePresentableUtil.getterName(name)) ||
               !names.contains(HaxePresentableUtil.setterName(name));
      }
    };

    abstract boolean accept(String name, Set<String> names);
  }

  private final Strategy myStratagy;

  public CreateGetterSetterFix(final HaxeClass haxeClass, Strategy strategy) {
    super(haxeClass);
    myStratagy = strategy;
  }

  @Override
  protected String buildFunctionsText(HaxeNamedComponent namedComponent) {
    if (!(namedComponent instanceof HaxeVarDeclarationPart)) {
      return "";
    }
    final String typeText = HaxePresentableUtil.buildTypeText(namedComponent, ((HaxeVarDeclarationPart)namedComponent).getTypeTag());
    final StringBuilder result = new StringBuilder();
    if (myStratagy == Strategy.GETTER || myStratagy == Strategy.GETTERSETTER) {
      if (null == myHaxeClass.findHaxeMethodByName(HaxePresentableUtil.getterName(namedComponent.getName()))) {
        buildGetter(result, namedComponent, typeText);
      }
    }
    if (myStratagy == Strategy.SETTER || myStratagy == Strategy.GETTERSETTER) {
      if (null == myHaxeClass.findHaxeMethodByName(HaxePresentableUtil.setterName(namedComponent.getName()))) {
        buildSetter(result, namedComponent, typeText);
      }
    }
    return result.toString();
  }

  @Override
  protected void modifyElement(HaxeNamedComponent namedComponent) {
    if (!(namedComponent instanceof HaxeVarDeclarationPart)) {
      return;
    }
    if (((HaxeVarDeclarationPart)namedComponent).getPropertyDeclaration() != null) {
      // todo: modify
      return;
    }

    final String typeText = HaxePresentableUtil.buildTypeText(namedComponent, ((HaxeVarDeclarationPart)namedComponent).getTypeTag());

    final HaxeVarDeclaration declaration =
      HaxeElementGenerator.createVarDeclaration(namedComponent.getProject(), buildVarDeclaration(namedComponent, typeText));
    final HaxePropertyDeclaration propertyDeclaration = declaration.getVarDeclarationPart().getPropertyDeclaration();
    if (propertyDeclaration != null) {
      HaxeVarDeclaration varDeclaration = PsiTreeUtil.getParentOfType(namedComponent, HaxeVarDeclaration.class, false);
      if (varDeclaration != null) {
        varDeclaration.replace(declaration);
      }
    }
  }

  private String buildVarDeclaration(HaxeNamedComponent namedComponent, String typeText) {
    final StringBuilder result = new StringBuilder();
    result.append("@:isVar ");
    if (namedComponent.isPublic()) {
      result.append("public ");
    }
    if (namedComponent.isStatic()) {
      result.append("static ");
    }
    result.append("var ");
    result.append(namedComponent.getName());
    result.append("(");
    if (myStratagy == Strategy.GETTER || myStratagy == Strategy.GETTERSETTER) {
      result.append("get");
    }
    else {
      result.append("null");
    }
    result.append(",");
    if (myStratagy == Strategy.SETTER || myStratagy == Strategy.GETTERSETTER) {
      result.append("set");
    }
    else {
      result.append("null");
    }
    result.append(")");

    if (!typeText.isEmpty()) {
      result.append(":" + typeText);
    }

    result.append(";");
    return result.toString();
  }

  private static void buildGetter(StringBuilder result, HaxeNamedComponent namedComponent, String typeText) {
    build(result, namedComponent, typeText, true);
  }

  private static void buildSetter(StringBuilder result, HaxeNamedComponent namedComponent, String typeText) {
    build(result, namedComponent, typeText, false);
  }

  private static void build(StringBuilder result, HaxeNamedComponent namedComponent, String typeText, boolean isGetter) {
    Boolean isStatic = namedComponent.isStatic();
    String name = namedComponent.getName();
    if (isStatic) {
      result.append("static ");
    }
    result.append("function ");
    result.append(isGetter ? HaxePresentableUtil.getterName(name) : HaxePresentableUtil.setterName(name));
    result.append("(");
    if (!isGetter) {
      result.append("value");

      if (!typeText.isEmpty()) {
        result.append(":");
        result.append(typeText);
      }
    }
    result.append(")");
    if (isGetter && !typeText.isEmpty()) {
      result.append(":");
      result.append(typeText);
    }
    result.append("{\n");
    if (isGetter) {
      result.append("return ");
      result.append(name);
      result.append(";");
    }
    else {
      result.append("return ");
      if (!isStatic) {
        result.append("this.");
      }
      result.append(name);
      result.append("= value;");
    }
    result.append("\n}");
  }
}
