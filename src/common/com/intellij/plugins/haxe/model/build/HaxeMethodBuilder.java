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

import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.util.HaxeNameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class HaxeMethodBuilder {
  public final String name;
  @Nullable public final ResultHolder retval;
  public final List<HaxeArgumentBuilder> args;

  public HaxeMethodBuilder(String name, @Nullable ResultHolder retval, HaxeArgumentBuilder... args) {
    this.name = name;
    this.retval = retval;
    this.args = new LinkedList<HaxeArgumentBuilder>(Arrays.asList(args));
  }

  static public HaxeMethodBuilder fromModel(HaxeMethodModel model) {
    final HaxeMethodBuilder builder = new HaxeMethodBuilder(model.getName(), model.getReturnType(null));
    for (HaxeParameterModel parameter : model.getParameters().parameters) {
      builder.addArgument(parameter.getName(), parameter.getType());
    }
    return builder;
  }

  public HaxeMethodBuilder(String name, @Nullable ResultHolder retval, Collection<HaxeArgumentBuilder> args) {
    this(name, retval, args.toArray(new HaxeArgumentBuilder[args.size()]));
  }

  public void addArgument(HaxeArgumentBuilder arg) {
    this.args.add(arg);
  }

  public void addArgument(@NotNull String name, @Nullable ResultHolder type) {
    for (HaxeArgumentBuilder arg : args) {
      if (arg.name.equals(name)) {
        name = HaxeNameUtils.incrementNumber(name);
      }
    }

    this.args.add(new HaxeArgumentBuilder(name, type));
  }

  public HaxeMethodBuilder(String name) {
    this(name, null);
  }

  public String toString() {
    String out = "";
    out += "function " + name + "(" + StringUtils.join(args, ", ") + ")";
    if (retval != null) out += ":" + retval;
    out += " {\n}";
    return out;
  }
}
