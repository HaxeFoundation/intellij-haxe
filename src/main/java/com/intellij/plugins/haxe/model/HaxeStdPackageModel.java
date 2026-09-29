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
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    // TODO: serve this per-call file model from a CachedValue like getStdRootTypes
    final HaxeFile file = getFile(STD_TYPES);
    if (file != null) {
      return HaxeStdTypesFileModel.fromFile(file);
    }
    return null;
  }

  @Nullable
  @Override
  public HaxeClassModel getClassModel(@NotNull String className) {
    Map<String, HaxeClassModel> stdRootTypes = getStdRootTypes();
    if (stdRootTypes != null) {
      return stdRootTypes.get(className);
    }

    // no std root directory (DUMMY root in tests) - probe by file name
    HaxeClassModel result = super.getClassModel(className);
    HaxeFileModel stdTypesModel = getStdFileModel();
    if (result == null && stdTypesModel != null) {
      result = stdTypesModel.getClassModel(className);
    }
    return result;
  }

  /**
   * Every type declared by the files directly in the std root, keyed by type
   * name. Covers both layouts in one lookup: types with their own file
   * (String, Array, Map) and the primitives declared inside StdTypes.hx
   * (Int, Bool, Void, Float, Dynamic) - the latter can never be found by a
   * file-name probe. Anchored on the root directory, so modules sharing an
   * SDK share the map. Invalidates on ANY PSI change (global modification
   * count) - a PsiDirectory has no per-directory timestamp, so finer
   * dependencies cannot exist.
   */
  @Nullable
  private Map<String, HaxeClassModel> getStdRootTypes() {
    PsiDirectory directory = root.access("");
    if (directory == null) return null;
    return CachedValuesManager.getCachedValue(directory, () -> stdRootTypesResult(directory));
  }

  private static CachedValueProvider.Result<Map<String, HaxeClassModel>> stdRootTypesResult(PsiDirectory directory) {
    Map<String, HaxeClassModel> byName = new HashMap<>();
    for (PsiFile file : directory.getFiles()) {
      if (!(file instanceof HaxeFile haxeFile)) continue;
      collectDeclaredTypes(haxeFile, byName);
    }
    return CachedValueProvider.Result.create(byName, PsiModificationTracker.MODIFICATION_COUNT);
  }

  private static void collectDeclaredTypes(HaxeFile file, Map<String, HaxeClassModel> byName) {
    HaxeFileModel fileModel = HaxeFileModel.fromElement(file);
    if (fileModel == null) return;
    String moduleName = fileModel.getName();
    for (HaxeClassModel classModel : fileModel.getClassModels()) {
      String name = classModel.getName();
      if (name == null) continue;
      // on a name collision the declaration whose file carries the type's
      // name wins, matching file-name probe precedence
      if (name.equals(moduleName)) {
        byName.put(name, classModel);
      } else {
        byName.putIfAbsent(name, classModel);
      }
    }
  }

  @Override
  public HaxeModel resolve(FullyQualifiedInfo info) {
    HaxeModel result = super.resolve(info);

    HaxeFileModel stdTypesModel = getStdFileModel();
    if (result == null && stdTypesModel != null && (info.packageName == null || info.packageName.isEmpty()) && this.path.isEmpty()) {
      result = stdTypesModel.resolve(new FullyQualifiedInfo("", null, info.moduleName, info.memberName));
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


  @NotNull
  public List<HaxeGlobalUsingModel> getGlobalUsings() {
    if (null == globalUsingModels || containsInvalidModel()) {
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

  private boolean containsInvalidModel() {
    return globalUsingModels.stream().anyMatch(m -> !m.isValid());
  }
}
