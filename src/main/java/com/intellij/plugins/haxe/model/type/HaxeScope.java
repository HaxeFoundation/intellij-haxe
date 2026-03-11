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
package com.intellij.plugins.haxe.model.type;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

// @TODO: First approach, then use a resolver interface
public class HaxeScope<T> {
  @Nullable
  final public HaxeScope<T> parent;

  // we may save time by not looking into every loop or if statement
  // if they are not relevant to what we are trying to solve
  public  boolean deepSearchForReturnValues = false;
  private Map<PsiElement, T> items = new HashMap<>();

  public UnificationRules unificationRules = UnificationRules.DEFAULT;

  public HaxeScope() {
    this(null);
  }

  public HaxeScope<T> set(@NotNull PsiElement key, T value) {
    this.items.put(key, value);
    return this;
  }

  public HaxeScope<T> setWhereDefined(@NotNull PsiElement key, T value) {
    if (items.containsKey(key)) {
      this.items.put(key, value);
    } else if (parent != null) {
      parent.setWhereDefined(key, value);
    }
    return this;
  }

  @Nullable
  public boolean has(@NotNull PsiElement name) {
    return get(name) != null;
  }

  @Nullable
  public T get(@NotNull PsiElement name) {
    if (items.containsKey(name)) {
      return items.get(name);
    } else if (parent != null) {
      return parent.get(name);
    }
    return null;
  }

  public HaxeScope(HaxeScope<T> parent) {
    this.parent = parent;
    if (parent != null) {
      deepSearchForReturnValues = parent.deepSearchForReturnValues;
      unificationRules = parent.unificationRules;
    }
  }
}
