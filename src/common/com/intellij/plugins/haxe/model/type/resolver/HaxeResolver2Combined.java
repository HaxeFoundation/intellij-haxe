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

import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HaxeResolver2Combined extends HaxeResolver2 {
  @NotNull
  public List<HaxeResolver2> resolvers;

  public HaxeResolver2Combined(@NotNull List<HaxeResolver2> resolvers) {
    this.resolvers = resolvers;
  }
  public HaxeResolver2Combined(HaxeResolver2... resolvers) {
    this.resolvers = new ArrayList<HaxeResolver2>(resolvers.length);
    for (HaxeResolver2 resolver : resolvers) {
      this.resolvers.add(resolver);
    }
  }

  @Nullable
  @Override
  public ResultHolder get(String key) {
    int size = resolvers.size();
    for (int n = size - 1; n >= 0; n--) {
      HaxeResolver2 resolver = resolvers.get(n);
      ResultHolder result = resolver.get(key);
      if (result != null) return result;
    }
    return null;
  }

  @Override
  public void addResults(@NotNull Map<String, ResultHolder> results) {
    for (HaxeResolver2 resolver : resolvers) {
      resolver.addResults(results);
    }
  }

  @Override
  public boolean isInStaticContext() {
    for (HaxeResolver2 resolver : resolvers) {
      if (resolver.isInStaticContext()) {
        return true;
      }
    }
    return false;
  }
}
