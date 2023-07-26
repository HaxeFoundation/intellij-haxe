/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018-2019 Eric Bishton
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

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeGenericSpecialization;
import com.intellij.plugins.haxe.model.HaxeGenericParamModel;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HaxeGenericResolver {
  // This must remain ordered, thus the LinkedHashMap.
  final private LinkedHashMap<String, ResultHolder> resolvers;

  public HaxeGenericResolver() {
    this.resolvers = new LinkedHashMap<String, ResultHolder>();
  }

  public HaxeGenericResolver withoutUnknowns() {
    HaxeGenericResolver resolver = new HaxeGenericResolver();
    for (Map.Entry<String, ResultHolder> entry : resolvers.entrySet()) {
      if(!entry.getValue().isUnknown()) {
        resolver.resolvers.put(entry.getKey(), entry.getValue());
      }
    }
    return resolver;
  }

  /*
     when resolving types inside a method with generic parameters we want to show the generic types and not unknown
      this method creates a new resolver and replaces its unknowns with  GenericParams from generic params models
   */
  public HaxeGenericResolver withTypeParametersAsType(@NotNull List<HaxeGenericParamModel> params) {
    HaxeGenericResolver resolver = new HaxeGenericResolver();
    resolver.resolvers.putAll(resolvers);

    for (HaxeGenericParamModel param : params) {
      String name = param.getName();
      if(resolver.resolvers.containsKey(name) && resolver.resolvers.get(name).isUnknown()) {
        HaxeClassReference classReference = new HaxeClassReference(name, param.getPsi(), true);
        ResultHolder holder = new ResultHolder(SpecificHaxeClassReference.withoutGenerics(classReference));
        resolver.resolvers.put(name, holder);
      }
    }
    return resolver;
  }
  public ResultHolder add(@NotNull String name, @NotNull ResultHolder specificType) {
    resolvers.put(name, specificType);
    return specificType;
  }

  @NotNull
  public HaxeGenericResolver addAll(@Nullable HaxeGenericResolver parentResolver) {
    if (null != parentResolver) {
      this.resolvers.putAll(parentResolver.resolvers);
    }
    return this;
  }

  @Nullable
  public ResultHolder resolve(String name) {
    return resolvers.get(name);
  }

  @Nullable
  public ResultHolder resolve(ResultHolder resultHolder) {
    if (null == resultHolder ) return null;
    return HaxeTypeResolver.resolveParameterizedType(resultHolder, this);
  }

  /**
   * @return The names of all generics in this resolver in order of their adding.
   */
  @NotNull
  public String[] names() {
    String[] names = new String[resolvers.size()];
    int i = 0;
    for (String name : resolvers.keySet()) {
      names[i++] = name;
    }
    return names;
  }

  /**
   * @return All specific generic types in this resolver in the order of their adding.
   */
  @NotNull
  public ResultHolder[] getSpecifics() {
    if (resolvers.isEmpty()) return ResultHolder.EMPTY;
    ResultHolder results[] = new ResultHolder[resolvers.size()];
    int i = 0;
    for (ResultHolder result : resolvers.values()) {
      results[i++] = result;
    }
    return results;
  }

  @NotNull
  public ResultHolder[] getSpecificsFor(@Nullable HaxeClassReference clazz) {
    return getSpecificsFor(clazz != null ? clazz.getHaxeClass() : null);
  }

  @NotNull
  public ResultHolder[] getSpecificsFor(@Nullable HaxeClass hc) {
    if (null == hc) return ResultHolder.EMPTY;

    return HaxeTypeResolver.resolveDeclarationParametersToTypes(hc, this);
  }

  /**
   * @return whether or not this resolver has any entries.
   */
  public boolean isEmpty() {
    return resolvers.isEmpty();
  }

  @NotNull
  public HaxeGenericSpecialization getSpecialization(@Nullable PsiElement element) {
    return HaxeGenericSpecialization.fromGenericResolver(element, this);
  }
}
