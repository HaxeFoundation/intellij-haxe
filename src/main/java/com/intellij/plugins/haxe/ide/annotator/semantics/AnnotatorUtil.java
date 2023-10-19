package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeClassReferenceModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class AnnotatorUtil {

  public static boolean hasMacroForCodeGeneration(@NotNull HaxeClassModel clazz) {
    if (clazz.hasCompileTimeMeta(HaxeMeta.BUILD)) return true;

    List<HaxeClassModel> classModels = clazz.getExtendingTypes().stream()
      .map(HaxeClassReferenceModel::getHaxeClass)
      .collect(Collectors.toList());


    for (int i = 0; i < classModels.size(); i++) {
      HaxeClassModel model = classModels.get(i);
      HaxeClass aClass = model.haxeClass;
      if (aClass != null) {
        if (aClass.hasCompileTimeMeta(HaxeMeta.AUTO_BUILD)) {
          return true;
        }
        List<HaxeClassModel> list =
          model.getExtendingTypes().stream()
            .map(HaxeClassReferenceModel::getHaxeClass)
            .filter(not(classModels::contains))
            .toList();

        classModels.addAll(list);
      }
    }

    return false;
  }


}
