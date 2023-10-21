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
import com.intellij.plugins.haxe.model.type.resolver.ResolverEntry;
import com.intellij.plugins.haxe.model.type.resolver.ResolveSource;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HaxeGenericResolver {
  // This must remain ordered, thus the LinkedHashMap.
  //final private LinkedHashMap<String, ResolverEntry> resolvers;
  final private LinkedList<ResolverEntry> resolvers;
  final private LinkedList<ResolverEntry> constaints;

  public HaxeGenericResolver() {
    this.resolvers = new LinkedList<>();
    this.constaints = new LinkedList<>();
  }

  public HaxeGenericResolver withoutUnknowns() {
    HaxeGenericResolver resolver = new HaxeGenericResolver();
    resolvers.stream().filter(entry -> !entry.type().isUnknown()).forEach(resolver.resolvers::add);
    constaints.stream().filter(entry -> !entry.type().isUnknown()).forEach(resolver.constaints::add);
    return resolver;
  }

  /*
     when resolving types inside a method with generic parameters we want to show the generic types and not unknown
      this method creates a new resolver and replaces its unknowns with  GenericParams from generic params models
   */
  public HaxeGenericResolver withTypeParametersAsType(@NotNull List<HaxeGenericParamModel> params) {
    HaxeGenericResolver resolver = new HaxeGenericResolver();
    resolver.resolvers.addAll(resolvers);

    for (HaxeGenericParamModel param : params) {
      String name = param.getName();
      Optional<ResolverEntry> match = resolver.resolvers.stream()
        .filter(entry -> entry.name().equals(name))
        .filter(entry -> entry.type().isUnknown())
        .findAny();

      if(match.isPresent()) {
        HaxeClassReference classReference = new HaxeClassReference(name, param.getPsi(), true);
        ResultHolder holder = new ResultHolder(SpecificHaxeClassReference.withoutGenerics(classReference));
        resolver.resolvers.add(new ResolverEntry(name, holder, ResolveSource.TODO));
      }
    }
    resolver.constaints.addAll(constaints);
    return resolver;
  }
  public ResultHolder add(@NotNull String name, @NotNull ResultHolder specificType) {
    resolvers.add(new ResolverEntry(name, specificType, ResolveSource.TODO));
    return specificType;
  }
  public ResultHolder add(@NotNull String name, @NotNull ResultHolder specificType, ResolveSource resolveSource) {
    resolvers.add(new ResolverEntry(name, specificType, resolveSource));
    return specificType;
  }
  public ResultHolder addConstraint(@NotNull String name, @NotNull ResultHolder specificType, ResolveSource resolveSource) {
    constaints.add(new ResolverEntry(name, specificType, resolveSource));
    return specificType;
  }

  @NotNull
  public HaxeGenericResolver addAll(@Nullable HaxeGenericResolver parentResolver) {
    if (null != parentResolver) {
      this.resolvers.addAll(parentResolver.resolvers);
      this.constaints.addAll(parentResolver.constaints);
    }
    return this;
  }

  @Nullable
  public ResultHolder resolve(String name) {
    ResultHolder holder = resolvers.stream()
      .filter(entry -> entry.name().equals(name)).min(this::ResolverPrioritySort)
      .map(ResolverEntry::type)
      .orElse(null);
    // fallback to constraints ?
    if (holder == null) {
      holder = constaints.stream()
        .filter(entry -> entry.name().equals(name)).min(this::ResolverPrioritySort)
        .map(ResolverEntry::type)
        .orElse(null);
    }
    return holder;
  }

  private int ResolverPrioritySort(ResolverEntry entry, ResolverEntry entry1) {
    int priorityA = entry.resolveSource().priority;
    int priorityB = entry1.resolveSource().priority;
    return Integer.compare(priorityA, priorityB);
  }

  @Nullable
  public ResultHolder resolveReturnType(SpecificHaxeClassReference reference) {
    if (null == reference ) return null;
    if (reference.isFromTypeParameter()) {
      String className = reference.getHaxeClassReference().name;
      List<ResolverEntry> resolveValues = resolvers.stream().filter(entry -> entry.name().equals(className)).toList();
      List<ResolverEntry> constraints = constaints.stream().filter(entry -> entry.name().equals(className)).toList();
      if (resolveValues.isEmpty())  {
        Optional<ResolverEntry> assign = findAsignToType();

        // if we know expected value and dont have any resolves
        if (assign.isPresent()) {
          return assign.get().type();
        }
        // if we got constraints but no resolve value, use constraint
        if (!constraints.isEmpty()) {
          return constraints.get(0).type();
        }
      }else {
        // list should be sorted so first element is correct
        return resolveValues.get(0).type();
      }
    }
    // todo recursion guard
    if (!reference.isFromTypeParameter()) {
      return HaxeTypeResolver.resolveParameterizedType(reference.createHolder(), this, true);
    }
    return null;
  }

  @NotNull
  private Optional<ResolverEntry> findAsignToType() {
    return resolvers.stream()
      .filter(entry -> entry.resolveSource() == ResolveSource.ASSIGN_TYPE)
      .findFirst();
  }

  @Nullable
  public ResultHolder resolveReturnType(ResultHolder resultHolder) {
    if (null == resultHolder ) return null;
    if (resultHolder.getType().isFromTypeParameter()) {
      String className = resultHolder.getType().context.getText();
      List<ResolverEntry> list = resolvers.stream().filter(entry -> entry.name().equals(className)).sorted(this::ResolverPrioritySort).toList();
      if (list.isEmpty())  {
        Optional<ResolverEntry> assign = findAsignToType();
        if (assign.isPresent()) {
          return assign.get().type();
        }
      }else {
        // list should be sorted so first element is correct
        return list.get(0).type();
      }
    }
    if (!resultHolder.getType().isFromTypeParameter()) {
      return HaxeTypeResolver.resolveParameterizedType(resultHolder, this, true);
    }
    return null;

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
    return resolvers.stream().map(ResolverEntry::name).toArray(String[]::new);
  }

  /**
   * @return All specific generic types in this resolver in the order of their adding.
   */
  @NotNull
  public ResultHolder[] getSpecifics() {
    if (resolvers.isEmpty()) return ResultHolder.EMPTY;
    ResultHolder results[] = new ResultHolder[resolvers.size()];
    int i = 0;
    for (ResolverEntry resolverEntry : resolvers) {
      results[i++] = resolverEntry.type();
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
    return resolvers.isEmpty() && constaints.isEmpty();
  }

  @NotNull
  public HaxeGenericSpecialization getSpecialization(@Nullable PsiElement element) {
    return HaxeGenericSpecialization.fromGenericResolver(element, this);
  }


}
