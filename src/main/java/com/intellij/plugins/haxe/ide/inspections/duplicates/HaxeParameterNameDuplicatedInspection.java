package com.intellij.plugins.haxe.ide.inspections.duplicates;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.psi.PsiElement;
import java.util.HashMap;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** The same parameter name used twice in one signature. */
public class HaxeParameterNameDuplicatedInspection extends HaxeInspection {

  public static final String DEFAULT_ARG_NAME = "_";

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeMethod.class, HaxeParameterNameDuplicatedInspection::checkDuplicatedParameterNames);
  }

  public static void checkDuplicatedParameterNames(final HaxeMethod methodPsi, final HaxeProblemReporter reporter) {
    HashMap<String, PsiElement> argumentNames = new HashMap<String, PsiElement>();
    for (final HaxeParameterModel param : methodPsi.getModel().getParameters()) {
      String paramName = param.getName();
      if (argumentNames.containsKey(paramName) && !paramName.equals(DEFAULT_ARG_NAME)) {
        String warningMessage = HaxeBundle.message("haxe.semantic.repeated.argument.name", paramName);
        reporter.problem(HighlightSeverity.WARNING, warningMessage).range(param.getNamePsi()).create();
        reporter.problem(HighlightSeverity.WARNING, warningMessage).range(argumentNames.get(paramName)).create();
      }
      else {
        argumentNames.put(paramName, param.getNamePsi());
      }
    }
  }

}
