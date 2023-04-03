/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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

import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.HaxeDebugUtil;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeGenericSpecialization implements Cloneable {

  public static final HaxeGenericSpecialization EMPTY = new HaxeGenericSpecialization() {
    @Override
    public void put(PsiElement element, String genericName, HaxeClassResolveResult resolveResult) {
      throw new HaxeDebugUtil.InvalidValueException("Must not modify (shared) EMPTY specialization.");
    }
  };

  final Map<String, HaxeClassResolveResult> map;

  public HaxeGenericSpecialization() {
    this(new THashMap<String, HaxeClassResolveResult>());
  }

  @Override
  protected HaxeGenericSpecialization clone() {
    final Map<String, HaxeClassResolveResult> clonedMap = new THashMap<String, HaxeClassResolveResult>();
    for (String key : map.keySet()) {
      clonedMap.put(key, map.get(key));
    }
    return new HaxeGenericSpecialization(clonedMap);
  }

  protected HaxeGenericSpecialization(Map<String, HaxeClassResolveResult> map) {
    this.map = map;
  }

  /**
   * @return the values in this specialization as a HaxeGenericResolver.
   **/
  public HaxeGenericResolver toGenericResolver(@Nullable PsiElement element) {
    /*
     * Optimally, this method would be part of the model layer (which uses HaxeGenericResolvers),
     * rather than part of this one, but that forces us to leak the details of how keys are created
     * and stored.
     *
     * Another thought is to make the HaxeGenericResolver be a sub-class of this one.
     *
     * A third would be to remove HaxeGenericResolver altogether and make the models use this class.
     */
    if (null == element) {
      element = SpecificTypeReference.createUnknownContext();
    }

    Map<String, HaxeClassResolveResult> innerMap = getMapWithInnerSpecializations(element);

    HaxeGenericResolver resolver = new HaxeGenericResolver();
    for (String key : innerMap.keySet()) {
      HaxeClassResolveResult resolveResult = innerMap.get(key);
      HaxeClass resolveClass = resolveResult.getHaxeClass();
      if (null == resolveClass) {
        resolveClass = SpecificHaxeClassReference.getUnknown(element).getHaxeClass();
      }
      ResultHolder resultHolder = resolveResult.getSpecificClassReference(resolveClass, null).createHolder();
      resolver.add(key, resultHolder);
    }
    return resolver;
  }

  @NotNull
  public static HaxeGenericSpecialization fromGenericResolver(@Nullable PsiElement element, @Nullable HaxeGenericResolver resolver) {
    HaxeGenericSpecialization specialization = new HaxeGenericSpecialization();
    if (null != resolver) {
      for (String name : resolver.names()) {
        ResultHolder holder = resolver.resolve(name);
        PsiElement context = holder.getElementContext();
        if (context == element) {
          // Going circular. Happens on a function with type parameters.  Skip this one
          // because the caller is already dealing with it, and there is no further type info.
          continue;
        }
        if (context instanceof HaxeClass) {
          HaxeClassResolveResult resolved =
            HaxeClassResolveResult.create((HaxeClass)context, fromGenericResolver(context, holder.getClassType().getGenericResolver()));
          specialization.put(element, name, resolved);
        } else if (holder.getClassType() != null) {
          HaxeClass clazz = holder.getClassType().getHaxeClass();
          HaxeClassResolveResult resolved =
            HaxeClassResolveResult.create(clazz, fromGenericResolver(context, holder.getClassType().getGenericResolver()));
          specialization.put(element, name, resolved);
        }
      }
    }
    return specialization;
  }

  public void put(@Nullable PsiElement element, @NotNull String genericName, @Nullable HaxeClassResolveResult resolveResult) {
    map.put(getGenericKey(element, genericName), resolveResult);
  }

  public boolean containsKey(@Nullable PsiElement element, @NotNull String genericName) {
    return map.containsKey(getGenericKey(element, genericName));
  }

  @NotNull
  public HaxeGenericSpecialization filterInnerKeys() {
    HaxeGenericSpecialization filtered = new HaxeGenericSpecialization();
    for (String key : map.keySet()) {
      if (key.contains("-")) {
        filtered.map.put(key, map.get(key));
      }
    }
    return filtered;
  }

  public HaxeClassResolveResult get(@Nullable PsiElement element, @NotNull String genericName) {
    return map.get(getGenericKey(element, genericName));
  }

  @NotNull
  public HaxeGenericSpecialization getInnerSpecialization(@Nullable PsiElement element) {
    final Map<String, HaxeClassResolveResult> result = getMapWithInnerSpecializations(element);
    return new HaxeGenericSpecialization(result);
  }

  @NotNull
  private Map<String, HaxeClassResolveResult> getMapWithInnerSpecializations(@Nullable PsiElement element) {
    // We are no longer removing fully-qualified entries for the element.  Rather,
    // we are duplicating them without the FQDN in the key so that pieces of the
    // code that do not have FQDN info can continue to match (which is what we
    // always did before).  Now, newer code that always carries the FQDN can also match
    // after an inner specialization is requested.

    final String prefixToRemove = getGenericKey(element, "");
    final Map<String, HaxeClassResolveResult> result = new THashMap<>();
    for (String key : map.keySet()) {
      final HaxeClassResolveResult value = map.get(key);
      if (!prefixToRemove.isEmpty() && key.startsWith(prefixToRemove)) {
        String newKey = key.substring(prefixToRemove.length());
        result.put(newKey, value);
      }
      result.put(key, value);
    }
    return result;
  }

  public static String getGenericKey(@Nullable PsiElement element, @NotNull String genericName) {
    final StringBuilder result = new StringBuilder();
    final HaxeNamedComponent namedComponent = PsiTreeUtil.getParentOfType(element, HaxeNamedComponent.class, false);
    if (namedComponent instanceof HaxeClass) {
      result.append(((HaxeClass)namedComponent).getQualifiedName());
    }
    else if (namedComponent != null) {
      HaxeClass haxeClass = PsiTreeUtil.getParentOfType(namedComponent, HaxeClass.class);
      if (haxeClass instanceof HaxeAnonymousType) {
        final PsiElement parent = HaxeResolveUtil.findTypeParameterContributor(haxeClass);
        haxeClass = parent instanceof HaxeClass ? (HaxeClass)parent : haxeClass;
      }
      if (haxeClass != null) {
        result.append(haxeClass.getQualifiedName());
      }
      if (PsiTreeUtil.getChildOfType(namedComponent, HaxeGenericParam.class) != null) {
        // generic method
        result.append(":");
        result.append(namedComponent.getName());
      }
    }
    if (result.length() > 0) {
      result.append("-");
    }
    result.append(genericName);
    return result.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HaxeGenericSpecialization that = (HaxeGenericSpecialization)o;
    return map.equals(that.map);
  }

  @Override
  public int hashCode() {
    return Objects.hash(map);
  }

  public String debugDump() {
    return debugDump(null);
  }

  public String debugDump( String linePrefix ) {
    StringBuilder builder = new StringBuilder();
    if (linePrefix == null) {
      linePrefix = "";
    }
    builder.append(linePrefix);
    builder.append(getClass().getName());
    builder.append(" : size=");
    builder.append(map.size());
    builder.append("\n");

    String prefix = linePrefix + "    ";

    for (String key : map.keySet()) {
      builder.append(prefix);
      builder.append(key);
      builder.append(" -> ");
      HaxeClassResolveResult result = map.get(key);
      builder.append(result == null ? "<no value>\n" : result.debugDump(prefix));
    }
    return builder.toString();
  }
}
