package com.intellij.plugins.haxe.ide.hint.parameters;

import com.intellij.codeInsight.hints.InlayInfo;
import com.intellij.codeInsight.hints.InlayParameterHintsProvider;
import com.intellij.codeInsight.hints.Option;
import com.intellij.plugins.haxe.HaxeHintBundle;
import com.intellij.plugins.haxe.ide.annotator.semantics.HaxeCallExpressionUtil;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeEnumValueModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HaxeInlayParameterHintsProvider implements InlayParameterHintsProvider {

  Option showHintsForConstructors = new Option("haxe.parameter.hint.constructor", HaxeHintBundle.message("haxe.parameter.hint.constructor.description"), true);
  Option showHintsForMethodCalls = new Option("haxe.parameter.hint.method", HaxeHintBundle.message("haxe.parameter.hint.method.description"), true);
  Option showHintsForEnumConstructors = new Option("haxe.parameter.hint.enum.constructor", HaxeHintBundle.message("haxe.parameter.hint.enum.constructor.description"), true);

  Option showHintsForAnyParameterExpressions = new Option("haxe.parameter.hint.parameter.all", HaxeHintBundle.message("haxe.parameter.hint.parameter.all.description"), false);

  @Override
  public @NotNull List<Option> getSupportedOptions() {
   return  List.of(showHintsForConstructors,
                   showHintsForMethodCalls,
                   showHintsForEnumConstructors,
                   showHintsForAnyParameterExpressions);
  }

  @Override
  public @NotNull Set<String> getDefaultBlackList() {
    return Set.of(
      "add(*)",
      "put(*)",
      "set(*,*)",
      "get(*)",
      "getProperty(*)",
      "setProperty(*,*)",
      "append(*)",
      "charAt(*)",
      "indexOf(*)",
      "contains(*)",
      "startsWith(*)",
      "endsWith(*)"
    );
  }




  @Override
  public @NotNull List<InlayInfo> getParameterHints(@NotNull PsiElement element) {
    List<InlayInfo> infoList = new ArrayList<>();

    if (showHintsForConstructors.isEnabled()) {
      if (element instanceof HaxeNewExpression newExpression) {
        handleNewExpressions(newExpression, infoList);
        return infoList;
      }
    }


    if (element instanceof HaxeCallExpression callExpression) {
      if (showHintsForMethodCalls.isEnabled()) {
        HaxeMethodModel methodModel = getMethodModel(callExpression);
        if (methodModel != null) {
          handleMethodHints(callExpression, methodModel, infoList);
          return infoList;
        }
      }

      if (showHintsForEnumConstructors.isEnabled()) {
        HaxeEnumValueModel enumValueModel = getEnumValueModel(callExpression);
        if (enumValueModel != null) {
          handleEnumValueCOnstructor(callExpression, enumValueModel, infoList);
          return infoList;
        }
      }
    }

    return infoList;
  }

  private void handleEnumValueCOnstructor(HaxeCallExpression callExpression, HaxeEnumValueModel enumValueModel, List<InlayInfo> infoList) {
    List<HaxeExpression> expressionList =
      callExpression.getExpressionList() == null ? List.of() : callExpression.getExpressionList().getExpressionList();
    HaxeCallExpressionUtil.CallExpressionValidation validation =
      HaxeCallExpressionUtil.checkEnumConstructor(callExpression, enumValueModel);
    if (enumValueModel.getConstructorParameters() != null) {
      List<HaxeParameterModel> parameters = MapParametersToModel(enumValueModel.getConstructorParameters());
      processArguments(validation.getArgumentToParameterIndex(), expressionList, parameters, infoList);
    }
  }

  private void handleMethodHints(HaxeCallExpression callExpression, HaxeMethodModel model, List<InlayInfo> infoList) {
    List<HaxeExpression> expressionList =
      callExpression.getExpressionList() == null ? List.of() : callExpression.getExpressionList().getExpressionList();
    HaxeCallExpressionUtil.CallExpressionValidation validation =
      HaxeCallExpressionUtil.checkMethodCall(callExpression, model.getMethod());

    if (validation.isCompleted()) {
      List<HaxeParameterModel> parameters = model.getParameters();
      processArguments(validation.getArgumentToParameterIndex(), expressionList, parameters, infoList);
    }
  }

  @Nullable
  private void handleNewExpressions(HaxeNewExpression newExpression, List<InlayInfo> infoList) {
    HaxeCallExpressionUtil.CallExpressionValidation validation = HaxeCallExpressionUtil.checkConstructor(newExpression);
    HaxeMethodModel model = getMethodModel(newExpression);
    if (validation.isCompleted()) {
      if (model == null) return;
      List<HaxeExpression> expressionList = newExpression.getExpressionList();
      List<HaxeParameterModel> parameters = model.getParameters();
      processArguments(validation.getArgumentToParameterIndex(), expressionList, parameters, infoList);
    }
  }

  @NotNull
  private static List<HaxeParameterModel> MapParametersToModel(HaxeParameterList parameterList) {
    return parameterList.getParameterList().stream().map(HaxeParameterModel::new).toList();
  }


  private void processArguments(Map<Integer, Integer> indexMap,
                                List<HaxeExpression> expressionList,
                                List<HaxeParameterModel> parameters,
                                List<InlayInfo> infoList) {
    int maxCounter = Math.min(expressionList.size(), parameters.size());
    for (int i = 0; i < maxCounter; i++) {
      HaxeExpression expression = expressionList.get(i);
      // since we can have optional parameters and they are matched by type, we use result from CallExpression evaluater to connect argument  to correct parameter
      if (indexMap == null || indexMap.containsKey(i)) {
        int parameterIndex = indexMap == null ? i : indexMap.get(i);
        HaxeParameterModel parameterModel = parameters.get(parameterIndex);
        boolean literal = isLiteral(expression) || showHintsForAnyParameterExpressions.get();

        if (literal) {
          int offset = getOffset(expression);
          String parameterName = getParameterName(parameterModel);
          infoList.add(new InlayInfo(parameterName, offset));
        }
      }
    }
  }

  private String getParameterName(HaxeParameterModel parameterModel) {
    return parameterModel.getName();
  }

  static HaxeMethodModel getMethodModel(HaxeExpression expression) {
    if (expression instanceof HaxeCallExpression callExpression) {

      if (callExpression.getExpression() instanceof HaxeReference reference) {
        final PsiElement target = reference.resolve();
        if (target instanceof HaxeMethod method) {
          return method.getModel();
        }
      }
    }
    if (expression instanceof HaxeNewExpression newExpression) {
      HaxeType type = newExpression.getType();
      if (type != null) {
        HaxeResolveResult result = type.getReferenceExpression().resolveHaxeClass();

        if (result.isHaxeClass() && result.getHaxeClass().isTypeDef()) {
          result = HaxeResolver.fullyResolveTypedef(result.getHaxeClass(), result.getSpecialization());
        }
        HaxeClass haxeClass = result.getHaxeClass();
        if (haxeClass != null) {
          HaxeClassModel model = haxeClass.getModel();
          if (model != null) {
            return model.getConstructor(result.getGenericResolver());
          }
        }
      }
    }
    return null;
  }

  static HaxeEnumValueModel getEnumValueModel(HaxeExpression expression) {
    if (expression instanceof HaxeCallExpression callExpression
        && callExpression.getExpression() instanceof HaxeReference reference) {
      final PsiElement target = reference.resolve();

      if (target instanceof HaxeEnumValueDeclaration enumValueDeclaration) {
        return (HaxeEnumValueModel)enumValueDeclaration.getModel();
      }
    }
    return null;
  }


  private static int getOffset(PsiElement element) {
    return element.getTextRange().getStartOffset();
  }

  private static boolean isLiteral(HaxeExpression expression) {
    return expression instanceof HaxeStringLiteralExpression
           || expression instanceof HaxeBinaryExpression
           || expression instanceof HaxeUnaryExpression
           || expression instanceof HaxeMapLiteral
           || expression instanceof HaxeArrayLiteral
           || expression instanceof HaxeObjectLiteral
           || expression instanceof HaxeLiteralExpression;
  }
}
