/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

public class HaxeMetadataUtil {

  public static List<HaxeMethodModel> getMethodsWithMetadata(@NotNull HaxeClassModel classModel, @Nullable String metadataName,
                                                             @Nullable Class<? extends HaxeMeta> metadataType,
                                                             @Nullable HaxeGenericResolver resolver) {

    List<HaxeMethodModel> methodModels = new LinkedList<>();
    for (HaxeMethodModel methodModel : classModel.getMethods(resolver)) {
      HaxeMetadataList metadataList = methodModel.getMethodPsi().getMetadataList(metadataType);
      boolean gotFromMetadata = metadataList.stream().anyMatch(a -> metadataName == null  || a.isType(metadataName));
      if (gotFromMetadata) {
        methodModels.add(methodModel);
      }
    }
    return methodModels;
  }
}
