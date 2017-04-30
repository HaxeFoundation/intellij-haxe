/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.parameterInfo.*;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeExpressionList;
import com.intellij.plugins.haxe.lang.psi.HaxeNewExpression;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeParameterInfoHandler implements ParameterInfoHandler<PsiElement, HaxeFunctionDescription> {
  private int currentParameterIndex = -1;

  @Override
  public boolean couldShowInLookup() {
    return true;
  }

  @Override
  public Object[] getParametersForLookup(LookupElement item, ParameterInfoContext context) {
    final Object object = item.getObject();

    if (object instanceof PsiElement) {
      final PsiElement element = (PsiElement)object;
      final HaxeComponentType type = HaxeComponentType.typeOf(element.getParent());

      if (type == HaxeComponentType.METHOD) {
        return new Object[]{element.getParent()};
      }
    }
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  @Nullable
  @Override
  public Object[] getParametersForDocumentation(HaxeFunctionDescription description, ParameterInfoContext context) {
    return description.getParameters();
  }

  @Override
  public PsiElement findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
    final PsiElement place = context.getFile().findElementAt(context.getEditor().getCaretModel().getOffset());
    return PsiTreeUtil.getParentOfType(place, HaxeCallExpression.class, HaxeNewExpression.class);
  }

  @Override
  public PsiElement findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
    return context.getFile().findElementAt(context.getEditor().getCaretModel().getOffset());
  }

  @Override
  public void showParameterInfo(@NotNull PsiElement element, @NotNull CreateParameterInfoContext context) {
    final HaxeFunctionDescription functionDescription = getParametersDescriptions(element);

    if (functionDescription != null && functionDescription.getParameters().length > 0) {
      context.setItemsToShow(new Object[]{functionDescription});
      context.showHint(element, element.getTextRange().getStartOffset(), this);
    }
  }

  @Nullable
  private HaxeFunctionDescription getParametersDescriptions(PsiElement element) {
    if (element instanceof HaxeCallExpression) {
      return HaxeFunctionDescriptionBuilder.buildForMethod((HaxeCallExpression)element);
    }
    else if (element instanceof HaxeNewExpression) {
      return HaxeFunctionDescriptionBuilder.buildForConstructor((HaxeNewExpression)element);
    }

    return null;
  }

  @Override
  public void updateParameterInfo(@NotNull PsiElement place, @NotNull UpdateParameterInfoContext context) {
    if (context.getParameterOwner() != PsiTreeUtil.getParentOfType(place, HaxeCallExpression.class, HaxeNewExpression.class)) {
      context.removeHint();
      return;
    }

    final Object[] objects = context.getObjectsToView();

    for (int i = 0; i < objects.length; i++) {
      context.setUIComponentEnabled(i, true);
    }

    currentParameterIndex = getParameterIndex(context.getParameterOwner(), place);
    context.setCurrentParameter(currentParameterIndex);
  }

  private int getParameterIndex(@NotNull PsiElement owner, @NotNull PsiElement place) {
    int parameterIndex = -1;

    final List<HaxeExpression> expressionList = getExpressionList(owner);
    final HaxeFunctionDescription functionDescription = getParametersDescriptions(owner);

    if (functionDescription == null) return parameterIndex;

    final HaxeParameterDescription[] functionParameters = functionDescription.getParameters();

    final int functionParametersCount = functionParameters == null ? 0 : functionParameters.length;

    if (expressionList != null) {
      HaxeExpression expression = (HaxeExpression)UsefulPsiTreeUtil.getParentOfType(place, HaxeExpression.class);

      if (expression != null) {
        int expressionIndex = expressionList.indexOf(expression);
        if (expressionIndex >= 0) {
          parameterIndex = expressionIndex;
        }
        else if (expression == owner) {
          if (place == owner.getLastChild()) {
            parameterIndex = expressionList.size() - 1;
          }
          else {
            expression = (HaxeExpression)UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(place);

            if (expression == owner.getLastChild()) {
              parameterIndex = expressionList.size() - 1;
            }
            else if (expression != null) {
              parameterIndex = expressionList.indexOf(expression);
            }
          }
        }
      }
    }

    return parameterIndex > functionParametersCount ? -1 : parameterIndex;
  }

  private List<HaxeExpression> getExpressionList(@NotNull PsiElement element) {
    if (element instanceof HaxeNewExpression) {
      return ((HaxeNewExpression)element).getExpressionList();
    }
    else if (element instanceof HaxeCallExpression) {
      HaxeExpressionList expressionList = ((HaxeCallExpression)element).getExpressionList();
      if (expressionList != null) {
        return expressionList.getExpressionList();
      }
    }

    return null;
  }

  @Override
  public String getParameterCloseChars() {
    return ",){}";
  }

  @Override
  public boolean tracksParameterIndex() {
    return true;
  }

  @Override
  public void updateUI(HaxeFunctionDescription description, @NotNull ParameterInfoUIContext context) {
    if (description == null) {
      context.setUIComponentEnabled(false);
      return;
    }

    context.setupUIComponentPresentation(
      description.toString(),

      description.getParameterRange(currentParameterIndex).getStartOffset(),
      description.getParameterRange(currentParameterIndex).getEndOffset(),

      !context.isUIComponentEnabled(),
      false,
      false,
      context.getDefaultParameterColor()
    );
  }
}
