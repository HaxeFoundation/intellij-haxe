package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

public class HaxeMetadataUtil {
  public static List<HaxeMethodModel> getMethodsWithMetadata(@NotNull HaxeClassModel classModel, @Nullable HaxeMetadataTypeName metadata,
                                                             @Nullable Class<? extends HaxeMeta> metadataType,
                                                             @Nullable HaxeGenericResolver resolver) {

    List<HaxeMethodModel> methodModels = new LinkedList<>();
    for (HaxeMethodModel methodModel : classModel.getMethods(resolver)) {
      HaxeMetadataList metadataList = methodModel.getMethodPsi().getMetadataList(metadataType);
      boolean gotFromMetadata = metadataList.stream().anyMatch(a ->  a.isType(metadata));
      if (gotFromMetadata) {
        methodModels.add(methodModel);
      }
    }
    return methodModels;
  }
}
