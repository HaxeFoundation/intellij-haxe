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
import com.intellij.plugins.haxe.model.HaxeImportModel;
import com.intellij.plugins.haxe.model.HaxePackageModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class HaxeResolver2File extends HaxeResolver2Combined {
  public HaxePackageModel packag;
  public HaxeFileModel file;
  public Map<String, HaxeClassModel> importedClasses = new HashMap<String, HaxeClassModel>();

  public HaxeResolver2File(HaxeFileModel file) {
    super();
    resolvers.add(file.getPackage().getResolver());
    for (HaxeImportModel importz : file.getImports()) {
      HaxeClassModel clazz = importz.getImportedClass();
      if (clazz != null) {
        importedClasses.put(clazz.getName(), clazz);
      }
    }
    this.file = file;
    this.packag = file.getPackage();
  }

  @Nullable
  @Override
  public ResultHolder get(String key) {
    ResultHolder result = null;

    // Try to get from classes in this file
    if (result == null) {
      HaxeClassModel clazz = file.getHaxeClass(key);
      if (clazz != null) {
        result = SpecificHaxeClassReference.withoutGenerics(clazz.getReference()).createHolder();
      }
    }

    // Try to get from classes in this package
    if (result == null) {
      HaxeClassModel classInPackage = packag.getHaxeClass(key);
      if (classInPackage != null) {
        result = SpecificHaxeClassReference.withoutGenerics(classInPackage.getReference()).createHolder();
      }
    }

    // Try to get from super
    if (result == null) {
      result = super.get(key);
    }

    return result;
  }

  @Override
  public void addResults(@NotNull Map<String, ResultHolder> results) {
    super.addResults(results);
    for (HaxeClassModel model : file.getHaxeClasses()) {
      results.put(model.getName(), SpecificHaxeClassReference.withoutGenerics(model.getReference()).createHolder());
    }
    for (HaxeClassModel model : packag.getHaxeClasses()) {
      results.put(model.getName(), SpecificHaxeClassReference.withoutGenerics(model.getReference()).createHolder());
    }
  }
}
