/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeClassDeclaration extends HaxeClass {

  @NotNull
  List<HaxeAutoBuildMacro> getAutoBuildMacroList();

  @NotNull
  List<HaxeBitmapMeta> getBitmapMetaList();

  @NotNull
  List<HaxeBuildMacro> getBuildMacroList();

  @Nullable
  HaxeClassBody getClassBody();

  @Nullable
  HaxeComponentName getComponentName();

  @NotNull
  List<HaxeCustomMeta> getCustomMetaList();

  @NotNull
  List<HaxeFakeEnumMeta> getFakeEnumMetaList();

  @Nullable
  HaxeGenericParam getGenericParam();

  @Nullable
  HaxeInheritList getInheritList();

  @NotNull
  List<HaxeMetaMeta> getMetaMetaList();

  @NotNull
  List<HaxeNativeMeta> getNativeMetaList();

  @NotNull
  List<HaxeNsMeta> getNsMetaList();

  @NotNull
  List<HaxeRequireMeta> getRequireMetaList();

}
