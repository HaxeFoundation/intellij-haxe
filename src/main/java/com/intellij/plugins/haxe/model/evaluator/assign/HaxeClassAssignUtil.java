package com.intellij.plugins.haxe.model.evaluator.assign;

import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.model.type.resolver.HaxeGenericResolverCastUtil;
import com.intellij.psi.PsiElement;
import lombok.CustomLog;

import java.util.List;

import static com.intellij.plugins.haxe.model.evaluator.assign.HaxeAssignEvaluation.canAssignTypeParameters;

@CustomLog
public class HaxeClassAssignUtil  {

  private static final RecursionGuard<PsiElement> hierarchyRecursionGuard = RecursionManager.createGuard("propagateRecursionGuard");

  static boolean sameTypeCheck(HaxeAssignEvaluation context, SpecificHaxeClassReference toClassReference, SpecificHaxeClassReference fromClassReference) {
    if (toClassReference.getHaxeClass() == fromClassReference.getHaxeClass()) {
      if (canAssignTypeParameters(context, toClassReference.getSpecifics(), fromClassReference.getSpecifics(), context.getConfig().ignoreFromConstraints(), true)) {
        return true;
      }
    }
    return false;
  }


  static  boolean testClassHierarchyAssign(HaxeAssignEvaluation context, SpecificHaxeClassReference toClassReference,
                                           SpecificHaxeClassReference fromClassReference
  ) {

    Boolean canAssign = hierarchyRecursionGuard.computePreventingRecursion(fromClassReference.getElementContext(), true,
                                                                           () -> _testClassHierarchyAssign(context, toClassReference,
                                                                                                           fromClassReference));
    if (canAssign == null) {
      log.warn("Recursion guard prevented class hierarchy can assign check");
      return false;
    }
    return canAssign;
  }

  static  private boolean _testClassHierarchyAssign(HaxeAssignEvaluation context, SpecificHaxeClassReference toClassReference,
                                                    SpecificHaxeClassReference fromClassReference
  ) {
    //  same class
    if (sameTypeCheck(context, toClassReference, fromClassReference)) return true;

    //  class in inheritance hierarchy
    HaxeClass fromClass = fromClassReference.getHaxeClass();
    HaxeClass toClass = toClassReference.getHaxeClass();

    List<SpecificHaxeClassReference> classHierarchy = HaxeGenericResolverCastUtil.findClassHierarchy(fromClass, toClass);
    if (classHierarchy.isEmpty()) return false;

    SpecificHaxeClassReference castedClass = fromClassReference.tryCastToClass(toClassReference);
    if (castedClass == null) return false;

    return sameTypeCheck(context, toClassReference, castedClass);
   }
}
