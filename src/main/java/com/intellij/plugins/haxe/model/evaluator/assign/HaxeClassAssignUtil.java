package com.intellij.plugins.haxe.model.evaluator.assign;

import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.model.type.resolver.HaxeGenericResolverCastUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import lombok.CustomLog;

import java.util.List;

import static com.intellij.plugins.haxe.model.evaluator.assign.HaxeAssignEvaluation.canAssignTypeParameters;

@CustomLog
public class HaxeClassAssignUtil  {

  private static final RecursionGuard<PsiElement> hierarchyRecursionGuard = RecursionManager.createGuard("propagateRecursionGuard");

  static boolean sameTypeCheck(HaxeAssignEvaluation context, SpecificHaxeClassReference toClassReference, SpecificHaxeClassReference fromClassReference) {
      HaxeClass toCaxeClass = toClassReference.getHaxeClass();
      HaxeClass fromHaxeClass = fromClassReference.getHaxeClass();

      // In cases where a Type/TypeTag does not resolve to an element we create a ClassReference with the `Type` as context
      // this means there is no HaxeClass, and thus no way to get Qname, so to avoid issues we return false unless its the same object.
      if(toCaxeClass == null && fromHaxeClass == null) {
          return toClassReference.context == fromClassReference.context;
      } else if(toCaxeClass == null || fromHaxeClass == null) {
          return false;
      }

      // Resolved on both sides: require the same source-level definition (qname + same file),
      // which also guards against PSI snapshot duplication unifying distinct types.
      if (sameClassDefinition(toCaxeClass, fromHaxeClass)) {
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

  // Treat two HaxeClass PSI instances as the same source-level definition when their
  // qualified names match AND HaxeTypeCompatible.sameDefinitionFile confirms they come
  // from the same file. Defensive against PSI snapshot duplication where the same logical
  // type is resolved to distinct HaxeClass instances by different paths (type-tag resolver
  // vs std-package lookup, stub-backed PSI vs AST-backed PSI). A non-empty qualified name
  // is required so that anonymous types (which share a null/empty qname) are not
  // incorrectly unified.
  private static boolean sameClassDefinition(HaxeClass a, HaxeClass b) {
    if (a == b) return true;
    if (a == null || b == null) return false;

    String qNameA = a.getQualifiedName();
    String qNameB = b.getQualifiedName();
    if (qNameA == null || qNameA.isEmpty()) {
      logSameClassDiagnostics("null/empty qNameA", a, b, qNameA, qNameB);
      return false;
    }
    if (!qNameA.equals(qNameB)) {
      logSameClassDiagnostics("qname mismatch", a, b, qNameA, qNameB);
      return false;
    }
    boolean sameFile = HaxeTypeCompatible.sameDefinitionFile(a, b);
    if (!sameFile) {
      logSameClassDiagnostics("file mismatch despite matching qname", a, b, qNameA, qNameB);
    }
    return sameFile;
  }

  // Emits a single debug-level entry describing why two HaxeClass instances were rejected
  // as the same definition. Enable trace/debug for this logger to capture the data when
  // the false-positive "Incompatible type" warning reproduces in the IDE.
  private static void logSameClassDiagnostics(String reason, HaxeClass a, HaxeClass b, String qNameA, String qNameB) {
    if (!log.isDebugEnabled()) return;
    log.debug("sameClassDefinition: " + reason
              + " | a=" + describeForLog(a, qNameA)
              + " | b=" + describeForLog(b, qNameB));
  }

  // Single-call summary of a HaxeClass for diagnostic logging - includes file name, VFS
  // path and canonical path so PSI duplication scenarios (different VFS roots, symlinks)
  // are visible at debug level when reproducing the false-positive "X should be X" error.
  private static String describeForLog(HaxeClass clazz, String qName) {
    PsiFile file = clazz.getContainingFile();
    String fileName = file != null ? file.getName() : "<null>";
    String path = "<null>";
    String canonical = "<null>";
    if (file != null) {
      PsiFile original = file.getOriginalFile();
      com.intellij.openapi.vfs.VirtualFile vf = original.getVirtualFile();
      if (vf != null) {
        path = vf.getPath();
        canonical = vf.getCanonicalPath();
      }
    }
    return clazz.getName() + "(qname=" + qName + ", file=" + fileName
           + ", path=" + path + ", canonical=" + canonical
           + ", " + System.identityHashCode(clazz) + ")";
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
