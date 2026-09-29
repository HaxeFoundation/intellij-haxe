package com.intellij.plugins.haxe.ide.inspections.hierarchy;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import org.apache.commons.lang3.StringUtils;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil.hasMacroForCodeGeneration;
import static java.util.function.Predicate.not;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** Interface methods (and abstract-base methods) the class does not implement. */
public class HaxeMissingInterfaceMethodInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeClass.class, HaxeMissingInterfaceMethodInspection::checkMissingInterfaceMethods);
  }
  /** Missing interface methods, plus unimplemented methods from an abstract base class. */
  public static void checkMissingInterfaceMethods(final HaxeClass clazzPsi, final HaxeProblemReporter reporter) {
    HaxeClassModel clazz = HaxeClassInspectionUtil.modelToCheck(clazzPsi);
    if (clazz == null || clazzPsi.isInterface() || clazzPsi.isTypeDef()) return;
    HaxeClassInspectionUtil.checkImplementedInterfaces(clazz, reporter, true, false, false);
    checkAbstractBaseMethods(clazz, reporter);
  }
  private static void checkAbstractBaseMethods(final HaxeClassModel clazz, final HaxeProblemReporter reporter) {
    List<HaxeClassReferenceModel> types = clazz.getExtendingTypes();
    // a class extends at most one superclass
    if (!types.isEmpty()) {
      HaxeClassReferenceModel model = types.getFirst();
      if (model.getHaxeClassModel() != null && model.getHaxeClassModel().isAbstractClass()) {
        checkAbstractMethods(clazz, model, reporter);
      }
    }
  }
  private static void checkAbstractMethods(HaxeClassModel clazz, HaxeClassReferenceModel abstractClass, HaxeProblemReporter reporter) {
    final List<HaxeMethodModel> missingMethods = new ArrayList<>();
    final List<String> missingMethodsNames = new ArrayList<String>();



    List<HaxeMethod> allMethodList = clazz.haxeClass.getHaxeMethodsAll();
    Set<HaxeMethod> extendedClassMethodList = new HashSet<>(abstractClass.getHaxeClassModel().haxeClass.getHaxeMethodsAll());

    Map<String, HaxeMethodModel> abstractMethods = extendedClassMethodList.stream()
      .map(HaxeMethodPsiMixin::getModel)
      .filter(m ->  m.isAbstract() || m.isInInterface())
      .collect(Collectors.toMap(m -> m.getMethod().getName(), Function.identity(), (m1, m2) -> m1));

    Map<String, HaxeMethodModel> nonAbstractMethods = allMethodList.stream()
      .map(HaxeMethodPsiMixin::getModel)
      .filter(not(HaxeMethodModel::isAbstract))
      .filter(not(HaxeMethodModel::isInInterface))
      .collect(Collectors.toMap(m -> m.getMethod().getName(), Function.identity(), (m1, m2) -> m1));

    for ( Map.Entry<String, HaxeMethodModel> entry : abstractMethods.entrySet()) {

      String name = entry.getKey();

      if (!nonAbstractMethods.containsKey(name)) {
        missingMethods.add(abstractMethods.get(name));
        missingMethodsNames.add(name);
      }else {
        HaxeMethodModel abstractMethod = entry.getValue();
        HaxeMethodModel methodImplementation = nonAbstractMethods.get(name);
        ResultHolder expectedReturnType = abstractMethod.getReturnType(null);
        ResultHolder actualReturnType = methodImplementation.getReturnType(null);
        if(methodImplementation.getDeclaringClass() == clazz) {
          if (!(expectedReturnType.canAssign(actualReturnType))) {
            String message = HaxeBundle.message("haxe.semantic.abstract.method.wrong.type", expectedReturnType.toPresentationString(), actualReturnType.toPresentationString());
            reporter.problem(HighlightSeverity.ERROR, message)
                    .range(methodImplementation.getReturnTypeTagOrNameOrBasePsi())
                    .create();
          }
        }
      }
    }

    if (!missingMethods.isEmpty() && !clazz.isAbstractClass()) {
      boolean macroWarning = hasMacroForCodeGeneration(clazz);
      if (macroWarning) {
        String message = HaxeBundle.message("haxe.semantic.methods.might.not.be.implemented", StringUtils.join(missingMethodsNames, ", "));
        message += HaxeBundle.message("haxe.semantic.macro.generated");
        reporter.problem(HighlightSeverity.WEAK_WARNING, message)
          .range(abstractClass.getPsi())
          .withFix(HaxeClassInspectionUtil.implementMissingMethodsFix(clazz, missingMethods))
          .create();

      }else {
        String message = HaxeBundle.message("haxe.semantic.methods.not.implemented", StringUtils.join(missingMethodsNames, ", "));
        reporter.problem(HighlightSeverity.ERROR, message)
          .range(abstractClass.getPsi())
          .withFix(HaxeClassInspectionUtil.implementMissingMethodsFix(clazz, missingMethods))
          .create();
      }
    }
  //TODO  check abstract classes for abstract methods to implement
  }

}
