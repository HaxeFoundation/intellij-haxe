/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Eric Bishton
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
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.HaxeDebugUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

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
    if (null != element) {
      return getInnerSpecialization(element).toGenericResolver(null);
    }

    HaxeGenericResolver resolver = new HaxeGenericResolver();
    for (String key : map.keySet()) {
      // Get and convert the resolveResults into ResultHolders,
      HaxeClass parameterClass = map.get(key).getHaxeClass();
      HaxeClassModel model = HaxeClassModel.fromElement(parameterClass);
      if (null != parameterClass && null != model) {
        HaxeClassReference reference = new HaxeClassReference(model, parameterClass);
        ResultHolder holder = SpecificHaxeClassReference.withoutGenerics(reference).createHolder();
        resolver.add(key, holder);
      } // TODO: else create a reference to Dynamic or Any??
    }
    return resolver;
  }

  @NotNull
  public static HaxeGenericSpecialization fromGenericResolver(@NotNull PsiElement element, @Nullable HaxeGenericResolver resolver) {
    HaxeGenericSpecialization specialization = new HaxeGenericSpecialization();
    if (null != resolver) {
      for (String name : resolver.names()) {
        ResultHolder holder = resolver.resolve(name);
        PsiElement held = holder.getElementContext();
        if (held instanceof HaxeClass) {
          HaxeClassResolveResult resolved =
            HaxeClassResolveResult.create((HaxeClass)held, fromGenericResolver(held, holder.getClassType().getGenericResolver()));
          specialization.put(element, name, resolved);
        }
      }
    }
    return specialization;
  }

  public void put(PsiElement element, String genericName, HaxeClassResolveResult resolveResult) {
    map.put(getGenericKey(element, genericName), resolveResult);
  }

  public boolean containsKey(@Nullable PsiElement element, String genericName) {
    return map.containsKey(getGenericKey(element, genericName));
  }

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
  public HaxeGenericSpecialization getInnerSpecialization(PsiElement element) {
    final String prefixToRemove = getGenericKey(element, "");
    final Map<String, HaxeClassResolveResult> result = new THashMap<String, HaxeClassResolveResult>();
    for (String key : map.keySet()) {
      final HaxeClassResolveResult value = map.get(key);
      String newKey = key;
      if (newKey.startsWith(prefixToRemove)) {
        newKey = newKey.substring(prefixToRemove.length());
      }
      result.put(newKey, value);
    }
    return new HaxeGenericSpecialization(result);
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
        // class -> typeOrAnonymous -> anonymous
        final PsiElement parent = haxeClass.getParent().getParent();
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
