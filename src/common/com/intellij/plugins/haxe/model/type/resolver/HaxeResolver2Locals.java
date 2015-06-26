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
package com.intellij.plugins.haxe.model.type.resolver;

import com.intellij.plugins.haxe.model.type.ResultHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class HaxeResolver2Locals extends HaxeResolver2 {
  @NotNull public HaxeResolver2 parent;
  private Map<String, ResultHolder> locals = new HashMap<String, ResultHolder>();

  public HaxeResolver2Locals(@NotNull HaxeResolver2 parent) {
    this.parent = parent;
  }

  public HaxeResolver2Locals put(String key, ResultHolder result) {
    locals.put(key, result);
    return this;
  }

  @Nullable
  @Override
  public ResultHolder get(String key) {
    ResultHolder result = locals.get(key);
    if (result == null) {
      return parent.get(key);
    }
    return result;
  }

  @Override
  public void addResults(@NotNull Map<String, ResultHolder> results) {
    parent.addResults(results);
    for (String s : locals.keySet()) {
      results.put(s, locals.get(s));
    }
  }

  @Override
  public boolean isInStaticContext() {
    return parent.isInStaticContext();
  }

  public HaxeResolver2Locals createChild() {
    return new HaxeResolver2Locals(this);
  }
}
