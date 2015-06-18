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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

// @TODO: First approach, then use a resolver interface
public class HaxeScope<T> {
  @Nullable
  final public HaxeScope<T> parent;

  private Map<String, T> items = new HashMap<String, T>();

  public HaxeScope() {
    this(null);
  }

  public HaxeScope<T> set(@NotNull String name, T value) {
    this.items.put(name, value);
    return this;
  }

  public HaxeScope<T> setWhereDefined(@NotNull String name, T value) {
    if (items.containsKey(name)) {
      this.items.put(name, value);
    } else if (parent != null) {
      parent.setWhereDefined(name, value);
    }
    return this;
  }

  @Nullable
  public boolean has(@NotNull String name) {
    return get(name) != null;
  }

  @Nullable
  public T get(@NotNull String name) {
    if (items.containsKey(name)) {
      return items.get(name);
    } else if (parent != null) {
      return parent.get(name);
    }
    return null;
  }

  public HaxeScope(HaxeScope<T> parent) {
    this.parent = parent;
  }
}
