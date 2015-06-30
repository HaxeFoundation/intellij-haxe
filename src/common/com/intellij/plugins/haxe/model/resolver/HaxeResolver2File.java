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
import com.intellij.plugins.haxe.model.HaxePackageModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HaxeResolver2File extends HaxeResolver2Combined {
  public List<HaxePackageModel> packages = new ArrayList<HaxePackageModel>();
  public HaxeFileModel file;
  public Map<String, HaxeClassModel> importedClasses = new HashMap<String, HaxeClassModel>();

  public HaxeResolver2File(@NotNull HaxeFileModel file) {
    super();
    packages.add(file.getProject().rootPackage);
    final HaxePackageModel aPackage = file.getDetectedPackage();
    if (aPackage != null) packages.add(aPackage);
    for (HaxeClassModel clazz : file.getImports().getImportedClasses()) {
      if (clazz != null) {
        importedClasses.put(clazz.getName(), clazz);
      }
    }
    this.file = file;
  }

  @Nullable
  @Override
  public ResultHolder get(String name) {
    ResultHolder result = null;

    // Try to get from classes in this file
    if (result == null) {
      HaxeClassModel clazz = file.getHaxeClass(name);
      if (clazz != null) {
        result = clazz.getClassType();
      }
    }

    // Try to get from classes in available packages
    if (result == null) {
      for (int n = packages.size() - 1; n >= 0; n--) {
        HaxePackageModel packag = packages.get(n);
        if (result == null && packag != null) {
          HaxeClassModel clazz = packag.getHaxeClass(name);
          if (clazz != null) {
            result = clazz.getClassType();
          }
        }
      }
    }

    // Try to get imported classes
    if (result == null) {
      final HaxeClassModel clazz = importedClasses.get(name);
      if (clazz != null) {
        result = clazz.getClassType();
      }
    }

    // Try to get from super
    if (result == null) {
      result = super.get(name);
    }

    return result;
  }

  @Override
  public void addResults(@NotNull Map<String, ResultHolder> results) {
    super.addResults(results);
    for (HaxeClassModel clazz : file.getHaxeClasses()) {
      results.put(clazz.getName(), clazz.getClassType());
    }

    for (HaxeClassModel clazz : importedClasses.values()) {
      results.put(clazz.getName(), clazz.getClassType());
    }

    for (HaxePackageModel packag : packages) {
      for (HaxeClassModel clazz : packag.getHaxeClasses()) {
        results.put(clazz.getName(), clazz.getClassType());
      }
    }
  }
}
