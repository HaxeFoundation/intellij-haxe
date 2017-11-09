/*
 * Copyright 2017-2017 Elias Ku
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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class HaxeStdTypes {
  public final SpecificHaxeClassReference DYNAMIC;
  public final SpecificHaxeClassReference FLOAT;
  public final SpecificHaxeClassReference INT;
  public final SpecificHaxeClassReference BOOL;
  public final SpecificHaxeClassReference VOID;
  public final SpecificHaxeClassReference STRING;
  public final SpecificHaxeClassReference ARRAY;
  public final SpecificHaxeClassReference CLASS;

  public HaxeStdTypes(@NotNull HaxeProjectModel project) {
    this.DYNAMIC = getPrimitive(project, "Dynamic");
    this.FLOAT = getPrimitive(project, "Float");
    this.INT = getPrimitive(project, "Int");
    this.BOOL = getPrimitive(project, "Bool");
    this.VOID = getPrimitive(project, "Void");
    this.STRING = getPrimitive(project, "String");
    this.ARRAY = getPrimitive(project, "Array");
    this.CLASS = getPrimitive(project, "Class");

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      assert this.DYNAMIC != null;
      assert this.FLOAT != null;
      assert this.INT != null;
      assert this.BOOL != null;
      assert this.VOID != null;
      assert this.STRING != null;
      assert this.ARRAY != null;
      assert this.CLASS != null;
    }
  }

  static private SpecificHaxeClassReference getPrimitive(@NotNull HaxeProjectModel project, String name) {
    final HaxeClassModel clazz = project.getStdPackage().getClassModel(name);
    return clazz == null ? null : clazz.getInstanceType().getClassType();
  }

  public static HaxeStdTypes fromElement(PsiElement element) {
    return HaxeProjectModel.fromElement(element).getStdTypes();
  }
}