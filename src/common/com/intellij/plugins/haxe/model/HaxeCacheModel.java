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
package com.intellij.plugins.haxe.model;

import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class HaxeCacheModel {
  public long stamp;
  private Map<Key, Object> values = new HashMap<Key, Object>();

  public HaxeCacheModel(long stamp) {
    this.stamp = stamp;
  }

  public <T> void put(Key<T> key, T value) {
    values.put(key, value);
  }

  @Nullable
  public <T> T get(Key<T> key) {
    return (T)values.get(key);
  }

  public <T> boolean has(Key<T> key) {
    return get(key) != null;
  }

  public <T> void remove(Key<T> key) {
    put(key, null);
  }
}
