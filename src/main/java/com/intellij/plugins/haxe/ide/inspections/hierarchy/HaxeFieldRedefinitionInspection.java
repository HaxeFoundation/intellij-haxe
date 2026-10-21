package com.intellij.plugins.haxe.ide.inspections.hierarchy;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import java.util.HashSet;
import static com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil.hasMacroForCodeGeneration;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** Fields redefining a superclass field. */
public class HaxeFieldRedefinitionInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeFieldDeclaration.class, HaxeFieldRedefinitionInspection::checkFieldRedefinition);
  }
  public static void checkFieldRedefinition(final HaxeFieldDeclaration var, final HaxeProblemReporter reporter) {
    HaxeFieldModel field = (HaxeFieldModel)var.getModel();
    HashSet<HaxeClassModel> classSet = new HashSet<>();
    HaxeClassModel fieldDeclaringClass = field.getDeclaringClass();
    // TODO  add support for module as parent ?
    if (fieldDeclaringClass == null) return;
    if (fieldDeclaringClass.isInterface() || fieldDeclaringClass.isAnonymous()) {
      return;
    }
    classSet.add(fieldDeclaringClass);
    while (fieldDeclaringClass != null ) {
      fieldDeclaringClass = fieldDeclaringClass.getParentClass();
      if (classSet.contains(fieldDeclaringClass)) {
        break;
      }
      else {
        if (fieldDeclaringClass != null) {
          if (!fieldDeclaringClass.isInterface() && !fieldDeclaringClass.isAnonymous()) {
            classSet.add(fieldDeclaringClass);
          }
        }
      }
      if (fieldDeclaringClass != null) {
        for (HaxeFieldModel parentField : fieldDeclaringClass.getFields()) {
          if (parentField.getName().equals(field.getName())) {
            String message;
            if (parentField.isStatic()) {
              message = HaxeBundle.message("haxe.semantic.static.field.override", field.getName());
              reporter.problem(HighlightSeverity.WEAK_WARNING, message)
                .range(field.getNameOrBasePsi())
                .create();
            }
            else {
              if (hasMacroForCodeGeneration(field.getDeclaringClass())) {
                message = HaxeBundle.message("haxe.semantic.variable.redefinition.possibly", field.getName(), fieldDeclaringClass.getName());
                message += HaxeBundle.message("haxe.semantic.macro.generated");
                reporter.problem(HighlightSeverity.WEAK_WARNING, message)
                  .range(field.getBasePsi())
                  .create();
              }
              else {
                message = HaxeBundle.message("haxe.semantic.variable.redefinition", field.getName(), fieldDeclaringClass.getName());
                reporter.problem(HighlightSeverity.ERROR, message)
                  .range(field.getBasePsi())
                  .create();
              }
            }
            break;
          }
        }
      }
    }
  }

}
