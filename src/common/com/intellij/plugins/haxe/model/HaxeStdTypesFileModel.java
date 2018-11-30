/*
 * Copyright 2018 Ilya Malanin
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

import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import org.jetbrains.annotations.NotNull;

public class HaxeStdTypesFileModel extends HaxeFileModel {
  public static final String STD_TYPES_HX = "StdTypes.hx";
  private static final Key<HaxeStdTypesFileModel> HAXE_STD_FILE_MODEL_KEY = new Key<>("HAXE_STD_FILE_MODEL");

  private HaxeStdTypesFileModel(@NotNull HaxeFile file) {
    super(file);
  }

  @Override
  protected boolean isReferencingCurrentFile(FullyQualifiedInfo info) {
    return (info.packagePath == null || info.packagePath.isEmpty()) && (info.fileName == null || info.fileName.isEmpty());
  }

  public static HaxeStdTypesFileModel fromFile(@NotNull HaxeFile file) {
    HaxeStdTypesFileModel model = file.getUserData(HAXE_STD_FILE_MODEL_KEY);
    if (model == null) {
      model = new HaxeStdTypesFileModel(file);
      file.putUserData(HAXE_STD_FILE_MODEL_KEY, model);
    }
    return model;
  }
}
