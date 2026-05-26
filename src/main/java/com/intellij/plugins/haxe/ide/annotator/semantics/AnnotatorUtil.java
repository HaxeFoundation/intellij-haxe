package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeClassReferenceModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class AnnotatorUtil {

  public static boolean hasMacroForCodeGeneration(@NotNull HaxeClassModel clazz) {
    if (clazz.hasCompileTimeMeta(HaxeMeta.BUILD)) return true;

    // @:autoBuild on an ancestor (extended class, parent interface, or implemented interface)
    // propagates the build macro to this class, so a missing member could still be generated.
    List<HaxeClassModel> classModels = ancestorModels(clazz).collect(Collectors.toList());

    for (int i = 0; i < classModels.size(); i++) {
      HaxeClassModel model = classModels.get(i);
      HaxeClass aClass = model.haxeClass;
      if (aClass != null) {
        if (aClass.hasCompileTimeMeta(HaxeMeta.AUTO_BUILD)) {
          return true;
        }
        List<HaxeClassModel> list = ancestorModels(model)
          .filter(not(classModels::contains))
          .toList();
        classModels.addAll(list);
      }
    }

    return false;
  }

  private static Stream<HaxeClassModel> ancestorModels(HaxeClassModel model) {
    return Stream.concat(model.getExtendingTypes().stream(), model.getImplementingInterfaces().stream())
      .map(HaxeClassReferenceModel::getHaxeClassModel)
      .filter(Objects::nonNull);
  }

}
