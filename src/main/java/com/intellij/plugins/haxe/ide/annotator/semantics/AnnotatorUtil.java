package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeClassReferenceModel;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class AnnotatorUtil {

  public static boolean hasMacroForCodeGeneration(@NotNull HaxeClassModel clazz) {
    if (clazz.hasCompileTimeMeta(HaxeMeta.BUILD) || clazz.hasCompileTimeMeta(HaxeMeta.GENERIC_BUILD)) return true;

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

  /**
   * Returns true when {@code reference} is qualified and its qualifier resolves to a class
   * whose hierarchy uses a build/autoBuild/genericBuild macro — i.e. the referenced member
   * might be macro-injected and is therefore invisible to static analysis.
   */
  public static boolean qualifierIsMacroGenerated(@NotNull HaxeReferenceExpression reference) {
    PsiElement qualifier = reference.getQualifier();
    if (!(qualifier instanceof HaxeReference qref)) return false;
    HaxeClass haxeClass = qref.resolveHaxeClass().getHaxeClass();
    if (haxeClass == null) return false;
    return hasMacroForCodeGeneration(haxeClass.getModel());
  }

  private static Stream<HaxeClassModel> ancestorModels(HaxeClassModel model) {
    return Stream.concat(model.getExtendingTypes().stream(), model.getImplementingInterfaces().stream())
      .map(HaxeClassReferenceModel::getHaxeClassModel)
      .filter(Objects::nonNull);
  }

}
