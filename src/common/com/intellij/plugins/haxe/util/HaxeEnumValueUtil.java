/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2020 AS3Boyan
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
package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

import static com.intellij.plugins.haxe.model.type.SpecificTypeReference.getStdClass;
import static com.intellij.plugins.haxe.model.type.SpecificTypeReference.getStdTypeModel;

/**
 * Util to easily get EnumValueClass reference and model
 */
public class HaxeEnumValueUtil {

  //TODO can any of these  results be cached ?

  @NotNull
  public static SpecificHaxeClassReference getEnumValueClass(@NotNull PsiElement context) {
    return getStdClass("EnumValue", context, new ResultHolder[]{});
  }

  @Nullable
  public static HaxeClassModel getEnumValueClassModel(@NotNull PsiElement context) {
    return getStdTypeModel("EnumValue", context);
  }
  public static List<HaxeMemberModel> getEnumValueClassMembers(@NotNull PsiElement context, @Nullable HaxeGenericResolver resolver) {
    HaxeClassModel enumValueModel = getStdTypeModel("EnumValue", context);
    if(enumValueModel == null) return new LinkedList<>();
    return  enumValueModel.getMembers(resolver);
  }
}
