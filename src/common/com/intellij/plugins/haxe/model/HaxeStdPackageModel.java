/*
 * Copyright 2017-2018 Ilya Malanin
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

import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HaxeStdPackageModel extends HaxePackageModel {
  private static final String STD_TYPES = "StdTypes";

  final protected static HashMap<String, FullyQualifiedInfo[]> implicitSubpackageTypes = new HashMap<>();

  static {
    // This list comes from the Haxe compiler sources: typer.create (Typer.ml, 1902-ish).
    // haxe.EnumWithType.valueTools isn't in any of Haxe 2.0.0, 2.10, 3.1.3, 3.4.7, or any 4.0 releases.
    // implicitSubTypes.put("valueTools", new FullyQualifiedInfo[]{new FullyQualifiedInfo("haxe.EnumWithType.valueTools")});

    implicitSubpackageTypes.put("EnumTools", new FullyQualifiedInfo[]{new FullyQualifiedInfo("haxe.EnumTools")});

    // In some versions, these can be in separate files.
    implicitSubpackageTypes.put("EnumValueTools",
                                new FullyQualifiedInfo[]{
                                  new FullyQualifiedInfo("haxe.EnumTools.EnumValueTools"),
                                  new FullyQualifiedInfo("haxe.EnumValueTools")
                                });

    // Exception is loaded in the compiler. It warms the cache so that loading Exception doesn't run into other
    // problems, but it's not put into the global usings.
    // implicitSubTypes.put("Exception", new FullyQualifiedInfo[]{ new FullyQualifiedInfo("haxe.Exception")});
  }

  final protected static HashMap<String, FullyQualifiedInfo[]> globalUsings = new HashMap<>();

  static {
    globalUsings.put("EnumTools", new FullyQualifiedInfo[]{new FullyQualifiedInfo("haxe.EnumTools")});
    globalUsings.put("EnumValueTools",
                     new FullyQualifiedInfo[]{
                       new FullyQualifiedInfo("haxe.EnumTools.EnumValueTools"),
                       new FullyQualifiedInfo("haxe.EnumValueTools")
                     });
  }

  private List<HaxeGlobalUsingModel> globalUsingModels = null;

  HaxeStdPackageModel(@NotNull HaxeSourceRootModel root) {
    super(root, "", null);
  }

  private HaxeFileModel getStdFileModel() {
    // TODO: This is called by the resolver a LOT.  Cache the result and create a listener to invalidate.
    final HaxeFile file = getFile(STD_TYPES);
    if (file != null) {
      return HaxeStdTypesFileModel.fromFile(file);
    }
    return null;
  }

  @Nullable
  @Override
  public HaxeClassModel getClassModel(@NotNull String className) {
    HaxeClassModel result = super.getClassModel(className);

    HaxeFileModel stdTypesModel = getStdFileModel();
    if (result == null && stdTypesModel != null) {
      result = stdTypesModel.getClassModel(className);
    }

    return result;
  }

  @Override
  public HaxeModel resolve(FullyQualifiedInfo info) {
    HaxeModel result = super.resolve(info);

    HaxeFileModel stdTypesModel = getStdFileModel();
    if (result == null && stdTypesModel != null && (info.packagePath == null || info.packagePath.isEmpty()) && this.path.isEmpty()) {
      result = stdTypesModel.resolve(new FullyQualifiedInfo("", null, info.fileName, info.memberName));
    }
    if (result == null) {
      resolveGlobalSubpackage(info, implicitSubpackageTypes);
    }

    return result;
  }

  @Nullable
  private HaxeModel resolveGlobalSubpackage(FullyQualifiedInfo info, HashMap<String, FullyQualifiedInfo[]> types) {
    HaxeModel result = null;
      FullyQualifiedInfo[] subpackages = types.get(info.memberName);
    if (null != subpackages) {
      for (FullyQualifiedInfo subpkg : subpackages) {
        result = super.resolve(subpkg);
        if (null != result) {
          break;
        }
      }
    }
    return result;
  }

  @Nullable
  public HaxeGlobalUsingModel resolveGlobalUsings(FullyQualifiedInfo info) {
    HaxeModel found = resolveGlobalSubpackage(info, globalUsings);
    return null == found ? null : new HaxeGlobalUsingModel(found.getBasePsi());
  }


  @NotNull
  public List<HaxeGlobalUsingModel> getGlobalUsings() {
    if (null == globalUsingModels) {
      List<HaxeGlobalUsingModel> modelList = new ArrayList<>();
      for (FullyQualifiedInfo[] infoAry : globalUsings.values()) {
        for (FullyQualifiedInfo info : infoAry) {
          HaxeModel result = super.resolve(info);
          if (null != result) {
            modelList.add(new HaxeGlobalUsingModel(result.getBasePsi()));
          }
        }
      }
      globalUsingModels = modelList;
    }
    return globalUsingModels;
  }
}
