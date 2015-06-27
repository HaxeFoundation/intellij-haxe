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
package com.intellij.plugins.haxe.model.resolver;

import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeFileModel;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class HaxeResolver2Class extends HaxeResolver2 {
  public HaxeClassModel clazz;
  public HaxeFileModel file;
  public HaxeResolver2 fileResolver;
  public boolean inStaticContext;

  public HaxeResolver2Class(@NotNull HaxeClassModel clazz, boolean inStaticContext) {
    this.clazz = clazz;
    this.file = clazz.getFile();
    this.fileResolver = this.file.getResolver();
    this.inStaticContext = inStaticContext;
  }

  @Nullable
  @Override
  public ResultHolder get(String key) {
    ResultHolder result = null;

    HaxeMemberModel member = clazz.getMember(key);
    if (member != null) {
      result = member.getMemberType();
    }

    if (result == null) {
      result = this.fileResolver.get(key);
    }

    return result;
  }

  @Override
  public void addResults(@NotNull Map<String, ResultHolder> results) {
    this.fileResolver.addResults(results);

    for (HaxeMemberModel member : clazz.getMembers()) {
      if (inStaticContext == member.isStatic()) {
        results.put(member.getName(), member.getMemberType());
      }
    }
  }

  @Override
  public boolean isInStaticContext() {
    return inStaticContext;
  }
}
