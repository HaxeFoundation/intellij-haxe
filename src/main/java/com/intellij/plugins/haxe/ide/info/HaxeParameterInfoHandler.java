/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
 * Copyright 2018 Eric Bishton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide.info;

import com.intellij.lang.parameterInfo.*;
import com.intellij.openapi.util.Condition;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolverUtil;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeParameterInfoHandler implements ParameterInfoHandler<PsiElement, HaxeFunctionDescription> {

  @Override
  public PsiElement findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
    final PsiElement selectedElement = context.getFile().findElementAt(context.getEditor().getCaretModel().getOffset());
    final HaxeReference method = PsiTreeUtil.getParentOfType(selectedElement, HaxeCallExpression.class, HaxeNewExpression.class);
    if (selectedElement != null && method != null) {
      return method;
    }
    return null;
  }

  @Override
  public PsiElement findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
    final PsiElement selectedElement = context.getFile().findElementAt(context.getEditor().getCaretModel().getOffset());
    final HaxeReference method = PsiTreeUtil.getParentOfType(selectedElement, HaxeCallExpression.class, HaxeNewExpression.class);
    if (selectedElement != null && method != null) {
      int parameterIndex = getArgumentIndex(method, selectedElement);
      context.setCurrentParameter(parameterIndex);
      return method;
    }
    return null;
  }

  @Override
  public void showParameterInfo(@NotNull PsiElement element, @NotNull CreateParameterInfoContext context) {
    final HaxeFunctionDescription functionDescription = getParametersDescriptions(element);

    if (functionDescription != null) {
      context.setItemsToShow(new Object[]{functionDescription});
      context.showHint(element, element.getTextRange().getStartOffset(), this);
    }
  }

  @Nullable
  private HaxeFunctionDescription getParametersDescriptions(PsiElement element) {
    if (element instanceof HaxeCallExpression callExpression) {
      return HaxeFunctionDescriptionBuilder.buildForMethod(callExpression);
    }
    else if (element instanceof HaxeNewExpression newExpression) {
      return HaxeFunctionDescriptionBuilder.buildForConstructor(newExpression);
    }

    return null;
  }

  @Override
  public void updateParameterInfo(@NotNull PsiElement place, @NotNull UpdateParameterInfoContext context) {
    PsiElement owner = context.getParameterOwner();

    if (place instanceof HaxeCallExpression || place instanceof HaxeNewExpression) {
      context.setParameterOwner(place);
    }else {
      if (owner != PsiTreeUtil.getParentOfType(place, HaxeCallExpression.class, HaxeNewExpression.class)) {
        context.removeHint();
      }
    }
  }

  private int getArgumentIndex(@NotNull PsiElement owner, @NotNull PsiElement place) {
    int argumentIndex = -1;

    final HaxeFunctionDescription functionDescription = getParametersDescriptions(owner);

    if (functionDescription == null || functionDescription.getParameters().length == 0) return argumentIndex;

    final List<HaxeExpression> argumentsList = getArgumentsList(owner);
    final HaxeParameterDescription[] functionParameters = functionDescription.getParameters();
    final int functionParametersCount = functionParameters == null ? 0 : functionParameters.length;

    if (argumentsList != null) {
      final int listSize = argumentsList.size();
      if (listSize == 0) return listSize;
      argumentIndex = getArgumentIndexUnderCaret(place, argumentsList);
    }

    if (argumentIndex > functionParametersCount) return -1;

    return interpolateArgumentIndexToParameterIndex(argumentsList, functionParameters, argumentIndex);
  }

  private int getArgumentIndexUnderCaret(@NotNull PsiElement place, List<HaxeExpression> expressionList) {
    HaxeExpression expression = getExpressionAtPlace(place, expressionList);

    if (expression != null) {
      int expressionIndex = expressionList.indexOf(expression);
      if (expressionIndex >= 0) {
        return expressionIndex;
      }
    }
    else {
      final String tokenText = place.getText();

      if (tokenText.equals(HaxeTokenTypes.PRPAREN.toString())) {
        return getExpressionIndexBeforeRightParen(expressionList);
      }
      else {
        return getExpressionIndexAtPlace(place, expressionList);
      }
    }

    return -1;
  }

  private HaxeExpression getExpressionAtPlace(@NotNull PsiElement place, final List<HaxeExpression> expressionList) {
    return (HaxeExpression)PsiTreeUtil.findFirstParent(place, getContainsCondition(expressionList));
  }



  private int getExpressionIndexAtPlace(PsiElement place, List<HaxeExpression> list) {
    final int listSize = list.size();

    if (list.get(listSize - 1).getTextOffset() < place.getTextOffset()) {
      return listSize;
    }

    final int placeTextOffset = place.getTextOffset();

    int expressionIndex = 0;
    for (HaxeExpression expression : list) {
      if (expression.getTextOffset() > placeTextOffset) {
        return expressionIndex;
      }
      expressionIndex++;
    }

    return -1;
  }

  private int getExpressionIndexBeforeRightParen(List<HaxeExpression> list) {
    final int listSize = list.size();
    final PsiElement commaExpression = UsefulPsiTreeUtil.getNextSiblingSkippingCondition(list.get(listSize-1), getNotCommaCondition(), false);
    if (commaExpression != null) {
      return listSize;
    }
    else {
      return listSize - 1;
    }
  }



  private int interpolateArgumentIndexToParameterIndex(List<HaxeExpression> arguments,
                                                       HaxeParameterDescription[] parameters,
                                                       int currentArgumentIndex) {

    int argumentIndex = 0;
    int recoverParameterIndex = 0;

    if (arguments == null || arguments.isEmpty()) return argumentIndex;

    final int parameterCount = parameters.length;
    final int argumentsCount = arguments.size();

    for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
      if (argumentIndex >= argumentsCount) return parameterIndex;

      final HaxeParameterDescription parameter = parameters[parameterIndex];

      if (!parameters[parameterIndex].isPredefined()) {
        if (argumentIndex == currentArgumentIndex) return parameterIndex;

        recoverParameterIndex = parameterIndex + 1;
        argumentIndex++;
        continue;
      }

      final HaxeExpression argument = arguments.get(argumentIndex);

      final ResultHolder parameterType = parameter.getResultHolder();
      final ResultHolder argumentType = HaxeTypeResolver.getPsiElementType(argument,
                                            HaxeGenericResolverUtil.generateResolverFromScopeParents(argument));

      if (parameterType.canAssign(argumentType)) {
        if (argumentIndex == currentArgumentIndex) return parameterIndex;

        recoverParameterIndex = parameterIndex + 1;
        argumentIndex++;
      }
    }

    return recoverParameterIndex;
  }

  private List<HaxeExpression> getArgumentsList(@NotNull PsiElement element) {
    if (element instanceof HaxeNewExpression newExpression) {
      return newExpression.getExpressionList();
    }
    else if (element instanceof HaxeCallExpression expression) {
      HaxeExpressionList expressionList = expression.getExpressionList();
      if (expressionList != null) {
        return expressionList.getExpressionList();
      }
    }

    return null;
  }

  @Override
  public void updateUI(@Nullable HaxeFunctionDescription description, @NotNull ParameterInfoUIContext context) {
    if (description == null) {
      context.setUIComponentEnabled(false);
      return;
    }

    int index = context.getCurrentParameterIndex();

    context.setupUIComponentPresentation(
      description.toString(),
      description.getParameterRange(index).getStartOffset(),
      description.getParameterRange(index).getEndOffset(),
      !context.isUIComponentEnabled(),
      false,
      false,
      context.getDefaultParameterColor()
    );
  }


  @NotNull
  private static Condition<PsiElement> getContainsCondition(List<HaxeExpression> expressionList) {
    return element -> element instanceof HaxeExpression && expressionList.contains(element);
  }
  @NotNull
  private static Condition<PsiElement> getNotCommaCondition() {
    return element -> !(element instanceof HaxePsiToken && element.getText().equals(HaxeTokenTypes.OCOMMA.toString()));
  }
}
