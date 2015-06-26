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

import com.intellij.psi.PsiElement;
import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxePackageModel {
  @Nullable final HaxePackageModel parent;
  @NotNull public final String fullName;
  @NotNull public final String name;
  @NotNull public final HaxeProjectModel project;

  public HaxePackageModel(@NotNull HaxeProjectModel project, @NotNull String name, @Nullable HaxePackageModel parent) {
    this.project = project;
    this.name = name;
    this.parent = parent;
    if (parent != null && !parent.fullName.isEmpty()) {
      this.fullName = parent.fullName + "." + name;
    } else {
      this.fullName = name;
    }
  }

  @Nullable
  public HaxePackageModel access(String path) {
    if (path.contains(".")) {
      int i = path.indexOf('.');
      HaxePackageModel child = getChild(path.substring(0, i));
      return (child != null) ? child.access(path.substring(i + 1)) : null;
    } else {
      return getChild(path);
    }
  }

  @NotNull
  public HaxeProjectModel getProject() {
    return project;
  }

  @Nullable
  public HaxePackageModel getChild(String name) {
    return null;
  }

  public List<HaxePackageModel> getChilds() {
    throw new NotImplementedException();
  }

  public HaxeClassModel getHaxeClass(String name) {
    throw new NotImplementedException();
  }

  public List<HaxeClassModel> getClasses() {
    throw new NotImplementedException();
  }
}
