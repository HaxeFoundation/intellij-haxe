package com.intellij.plugins.haxe.model.evaluator.assign;

import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
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
    if (sameClassDefinition(toClassReference.getHaxeClass(), fromClassReference.getHaxeClass())) {
        if (canAssignTypeParameters(context, toClassReference.getSpecifics(), fromClassReference.getSpecifics(), context.getConfig().ignoreFromConstraints(), true)) {
            return true;
        } else {
            //NOTE: special case for GenericBuild macros:
            // we ignore typeParameter mismatch if class has a GenericBuild macro with type parameter named "Rest"
            // "Rest" and "Const" seems to be reserved names for these macros and Rest allows you to use it with
            // an unspecified amount of TypeParameters.
            HaxeClassModel haxeClassModel = toClassReference.getHaxeClassModel();
            if (haxeClassModel != null && haxeClassModel.isGenericBuildWithRestTypeParam()) {
                for (ResultHolder specific : toClassReference.getSpecifics()) {
                    if (specific.getType() instanceof SpecificHaxeClassReference classReference) {
                        String className = classReference.getClassName();
                        if(className != null && className.equals("Rest")) return true;
                    }
                }
            }
        }
    }
    return false;
  }

  // Identity check first; otherwise treat as same definition when qualified name AND source file match.
  // Defensive against PSI snapshot duplication where the same logical type (e.g. Int, Class) is resolved
  // to distinct HaxeClass instances by different paths (type-tag resolver vs std-package lookup).
  // Requires a non-empty qualified name so that anonymous types (which share a null/empty qname) are
  // not incorrectly unified.
  private static boolean sameClassDefinition(HaxeClass a, HaxeClass b) {
    if (a == b) return true;
    if (a == null || b == null) return false;
    String qNameA = a.getQualifiedName();
    String qNameB = b.getQualifiedName();
    if (qNameA == null || qNameA.isEmpty()) return false;
    if (!qNameA.equals(qNameB)) return false;
    return HaxeTypeCompatible.sameDefinitionFile(a, b);
  }


  static  boolean testClassHierarchyAssign(HaxeAssignEvaluation context, SpecificHaxeClassReference toClassReference,
                                           SpecificHaxeClassReference fromClassReference
  ) {

    // abstracts and Enums do not extend or implement so no need to perform this check here.
    if(toClassReference.isAbstractType() || fromClassReference.isAbstractType()) return false;
    if(toClassReference.isEnumType() || fromClassReference.isEnumType()) return false;
    if(toClassReference.isEnumValue() || fromClassReference.isEnumValue()) return false;

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
