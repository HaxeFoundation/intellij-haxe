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
