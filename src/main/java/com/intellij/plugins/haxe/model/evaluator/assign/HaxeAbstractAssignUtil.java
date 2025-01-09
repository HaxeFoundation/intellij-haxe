package com.intellij.plugins.haxe.model.evaluator.assign;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.MULTI_TYPE;
import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.TRANSITIVE;

public class HaxeAbstractAssignUtil {

  public static boolean isMultiType(@NotNull SpecificHaxeClassReference classReference) {
    HaxeClass aClass = classReference.getHaxeClass();
    return aClass != null && aClass.hasCompileTimeMeta(MULTI_TYPE);
  }
  public  static boolean isTransitive(@NotNull SpecificTypeReference from) {
    if (from instanceof  SpecificHaxeClassReference classReference) {
      HaxeClass aClass = classReference.getHaxeClass();
      return aClass != null && aClass.hasCompileTimeMeta(TRANSITIVE);
    }
    return  false;
  }
}
