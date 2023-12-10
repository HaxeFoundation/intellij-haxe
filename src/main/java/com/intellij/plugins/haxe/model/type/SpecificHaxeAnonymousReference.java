/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
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
package com.intellij.plugins.haxe.model.type;

import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeTypeDefImpl;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.resolver.ResolveSource;
import com.intellij.plugins.haxe.util.HaxeDebugUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.*;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.model.type.HaxeMacroUtil.isMacroMethod;

@CustomLog
@EqualsAndHashCode
public class SpecificHaxeAnonymousReference extends SpecificHaxeClassReference {

  // anonymous types does not have generics but can inherit from Parent, this class lets us keep parent resolver with the reference
  private final HaxeGenericResolver genericResolver;

  public SpecificHaxeAnonymousReference(@NotNull HaxeClassReference classReference,
                                        @Nullable HaxeGenericResolver genericResolver,
                                        @Nullable Object constantValue,
                                        @Nullable HaxeRange rangeConstraint,
                                        @NotNull PsiElement context) {
    super(classReference,genericResolver == null ? ResultHolder.EMPTY : genericResolver.getSpecificsFor(classReference.getHaxeClass()), constantValue, rangeConstraint, context);
    this.genericResolver = genericResolver;
  }

  public static SpecificHaxeClassReference withGenerics(@NotNull HaxeClassReference clazz,  @Nullable HaxeGenericResolver genericResolver) {
    return new SpecificHaxeAnonymousReference(clazz, genericResolver, null, null, clazz.elementContext);
  }


  @Override
  public @NotNull HaxeGenericResolver getGenericResolver() {
    return genericResolver;
  }
}
