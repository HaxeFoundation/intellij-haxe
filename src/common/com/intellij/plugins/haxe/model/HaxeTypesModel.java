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

import com.intellij.plugins.haxe.model.type.HaxeClassReference;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import org.jetbrains.annotations.NotNull;

public class HaxeTypesModel {
  public final HaxeProjectModel project;
  public final SpecificTypeReference DYNAMIC;
  public final SpecificTypeReference FLOAT;
  public final SpecificTypeReference INT;
  public final SpecificTypeReference BOOL;
  public final SpecificTypeReference STRING;

  public HaxeTypesModel(@NotNull HaxeProjectModel project) {
    this.project = project;
    this.DYNAMIC = getPrimitive(project, "Dynamic");
    this.FLOAT = getPrimitive(project, "Float");
    this.INT = getPrimitive(project, "Int");
    this.BOOL = getPrimitive(project, "Bool");
    this.STRING = getPrimitive(project, "String");
  }

  static private SpecificTypeReference getPrimitive(@NotNull HaxeProjectModel project, String name) {
    final HaxeClassModel clazz = project.rootPackage.getHaxeClassFromFileName("StdTypes.hx", name);
    if (clazz != null) {
      return clazz.getInstanceType().getType();
    } else {
      return null;
    }
  }
}
