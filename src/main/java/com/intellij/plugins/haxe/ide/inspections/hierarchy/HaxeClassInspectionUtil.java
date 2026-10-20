package com.intellij.plugins.haxe.ide.inspections.hierarchy;

import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.generation.OverrideImplementMethodFix;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.psi.PsiElement;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import static com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil.hasMacroForCodeGeneration;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

import static com.intellij.plugins.haxe.ide.inspections.hierarchy.HaxeMethodSignatureCompatibilityInspection.checkMethodsSignatureCompatibility;

/**
 * The class-conformance machinery shared by the interface/abstract-class
 * inspections: the anonymous-nested-type guard every class-shaped check uses,
 * and the implemented-interface traversal the missing/signature/inherited
 * trio drives with its flags.
 */
public final class HaxeClassInspectionUtil {

  private HaxeClassInspectionUtil() {
  }

  /** The class model to check, or null when this PSI should be skipped. */
  @Nullable
  public static HaxeClassModel modelToCheck(final HaxeClass clazzPsi) {
    HaxeClassModel clazz = clazzPsi.getModel();
    if (clazzPsi instanceof HaxeAnonymousType && clazz.getParentClass() != null) {
      // avoiding unnecessary extra annotations when  HaxeAnonymousType is part of other non-anonymous types like typedefs etc.
      return null;
    }
    return clazz;
  }

  static boolean isAnonymousType(HaxeClassModel clazz) {
    if (clazz != null && clazz.haxeClass != null) {
      HaxeClass haxeClass = clazz.haxeClass;
      if (haxeClass instanceof HaxeAnonymousType) {
        return true;
      }
      if (haxeClass instanceof HaxeTypedefDeclaration) {
        HaxeTypeOrAnonymous anonOrType = ((HaxeTypedefDeclaration)haxeClass).getTypeOrAnonymous();
        if (anonOrType != null) {
          return anonOrType.getAnonymousType() != null;
        }
      }
    }
    return false;
  }

  static void checkImplementedInterfaces(final HaxeClassModel clazz, final HaxeProblemReporter reporter,
                                                 boolean checkMissingInterfaceMethods,
                                                 boolean checkInterfaceMethodSignature,
                                                 boolean checkInheritedInterfaceMethodSignature) {
    if (clazz.isClass() && !clazz.isAbstractClass()) {
      for (HaxeClassReferenceModel reference : clazz.getImplementingInterfaces()) {
        checkInterfaceMethods(clazz, reference, reporter,
                checkMissingInterfaceMethods,
                checkInterfaceMethodSignature,
                checkInheritedInterfaceMethodSignature);
      }
    }
  }

  private static void checkInterfaceMethods(
    final HaxeClassModel classModel,
    final HaxeClassReferenceModel intReference,
    final HaxeProblemReporter reporter,
    final boolean checkMissingInterfaceMethods,
    final boolean checkInterfaceMethodSignature,
    final boolean checkInheritedInterfaceMethodSignature
  ) {
    final List<HaxeMethodModel> missingMethods = new ArrayList<HaxeMethodModel>();
    final List<String> missingMethodsNames = new ArrayList<String>();

    if (intReference.getHaxeClassModel() != null) {
      List<HaxeMethodModel> implementedMethods = getAllMethodsExcludingAbstractAndInterfaces(classModel);
      List<HaxeMethodModel> interfaceMethods = getAllInterfaceMethodDeclarations(intReference);

      for (HaxeMethodModel interfaceMethod : interfaceMethods) {

        // NOTE: Static methods are allowed in extern interfaces
        if (!interfaceMethod.isStatic()) {
          Optional<HaxeMethodModel> methodImplementation = findInterfaceDeclarationForMethod(interfaceMethod, implementedMethods);

          if (methodImplementation.isEmpty()) {
            if (checkMissingInterfaceMethods) {
              missingMethods.add(interfaceMethod);
              missingMethodsNames.add(interfaceMethod.getName());
            }

          } else {
            final HaxeMethodModel implementMethodModel = methodImplementation.get();

            // an inherited implementation's signature may differ from the interface method
            HaxeClassModel declaringClass = implementMethodModel.getDeclaringClass();

            if (declaringClass != null && declaringClass != classModel) {
              if (declaringClass.isInterface()) {
                missingMethods.add(implementMethodModel);
                missingMethodsNames.add(interfaceMethod.getName());

              } else {
                if (checkInheritedInterfaceMethodSignature && !checkMethodsSignatureCompatibility(implementMethodModel, interfaceMethod)) {
                  final HaxeClass parentClass = declaringClass.haxeClass;
                  final String errorMessage = HaxeBundle.message(
                    "haxe.semantic.implemented.super.method.signature.differs",
                    implementMethodModel.getName(),
                    parentClass.getQualifiedName(),
                    interfaceMethod.getPresentableText(HaxeMethodContext.NO_EXTENSION),
                    implementMethodModel.getPresentableText(HaxeMethodContext.NO_EXTENSION)
                  );

                  reporter.problem(HighlightSeverity.ERROR, errorMessage).range(intReference.getPsi()).create();
                }
              }
            }
            else {
              if (checkInterfaceMethodSignature) {
                boolean canAnnotate = implementMethodModel.getDeclaringClass().haxeClass == classModel.haxeClass;
                checkMethodsSignatureCompatibility(implementMethodModel, interfaceMethod, reporter, canAnnotate);
              }
            }
          }
        }
      }
    }

    if (!missingMethods.isEmpty()) {
      boolean macroWarning = hasMacroForCodeGeneration(classModel);
      if (macroWarning) {
        String message = HaxeBundle.message("haxe.semantic.methods.might.not.be.implemented", StringUtils.join(missingMethodsNames, ", "));
        message += HaxeBundle.message("haxe.semantic.macro.generated");
        reporter.problem(HighlightSeverity.WEAK_WARNING, message)
          .range(intReference.getPsi())
          .withFix(implementMissingMethodsFix(classModel, missingMethods))
          .create();
      }else {
        String message = HaxeBundle.message("haxe.semantic.methods.not.implemented", StringUtils.join(missingMethodsNames, ", "));
        reporter.problem(HighlightSeverity.ERROR, message)
          .range(intReference.getPsi())
          .withFix(implementMissingMethodsFix(classModel, missingMethods))
          .create();
      }
    }
  }

  private static @NotNull Optional<HaxeMethodModel> findInterfaceDeclarationForMethod(HaxeMethodModel intMethod, List<HaxeMethodModel> implementedMethods) {
    return implementedMethods.stream()
            .filter(method -> intMethod.getName().equals(method.getName()))
            .findFirst();
  }

  private static List<HaxeMethodModel> getAllInterfaceMethodDeclarations(HaxeClassReferenceModel intReference) {
    return intReference.getHaxeClassModel().getMethods(null);
  }

  private static @NotNull List<HaxeMethodModel> getAllMethodsExcludingAbstractAndInterfaces(HaxeClassModel clazz) {
    return clazz.haxeClass.getHaxeMethodsAll(HaxeComponentType.INTERFACE).stream()
            .map(HaxeMethodPsiMixin::getModel)
            .filter(not(HaxeMethodModel::isAbstract))
            .toList();
  }

  @NotNull
  static HaxeFixer implementMissingMethodsFix(HaxeClassModel clazz, List<HaxeMethodModel> missingMethods) {
    String popupText = HaxeBundle.message("haxe.quickfix.implement.methods");
    return new HaxeFixer(popupText) {
      @Override
      public void run() {
        OverrideImplementMethodFix fix = new OverrideImplementMethodFix(clazz.haxeClass, false);
        for (HaxeMethodModel mm : missingMethods) {
          fix.addElementToProcess(mm.getMethodPsi());
        }

        PsiElement basePsi = clazz.getBasePsi();
        Project p = basePsi.getProject();
        fix.invoke(p, FileEditorManager.getInstance(p).getSelectedTextEditor(), basePsi.getContainingFile());
      }
    };
  }
}
