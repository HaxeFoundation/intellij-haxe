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
package com.intellij.plugins.haxe.model.build;

import com.intellij.plugins.haxe.model.type.ResultHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeArgumentBuilder {
  @NotNull public final String name;
  @Nullable public final ResultHolder type;

  public HaxeArgumentBuilder(@NotNull String name, @Nullable ResultHolder type) {
    this.name = name;
    this.type = type;
  }

  public HaxeArgumentBuilder(@NotNull String name) {
    this(name, null);
  }

  public String toString() {
    return (type != null) ? (name + ":" + type) : name;
  }
}
