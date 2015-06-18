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
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeClass extends HaxeComponent, PsiClass {
  HaxeClass[] EMPTY_ARRAY = new HaxeClass[0];

  @NotNull
  @NonNls
  String getQualifiedName();

  HaxeClassModel getModel();

  @NotNull
  List<HaxeType> getHaxeExtendsList();

  @NotNull
  List<HaxeType> getHaxeImplementsList();

  boolean isExtern();

  boolean isInterface();

  @NotNull
  List<HaxeMethod> getHaxeMethods();

  @NotNull
  List<HaxeNamedComponent> getHaxeFields();

  @NotNull
  List<HaxeVarDeclaration> getVarDeclarations();

  @Nullable
  HaxeNamedComponent findHaxeFieldByName(@NotNull final String name);

  @Nullable
  HaxeNamedComponent findHaxeMethodByName(@NotNull final String name);

  boolean isGeneric();

  @Nullable
  HaxeGenericParam getGenericParam();
}
