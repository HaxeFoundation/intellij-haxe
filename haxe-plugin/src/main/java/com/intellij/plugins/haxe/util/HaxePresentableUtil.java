/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
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
package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeParameterListPsiMixinImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxePresentableUtil {
  public static String setterName(@NotNull @Nls String name) {
    return "set_" + name;
  }

  public static String getterName(@NotNull @Nls String name) {
    return "get_" + name;
  }

  public static String startsWithUpperCase(@NotNull @Nls String name) {
    if (name.length() == 1) {
      return String.valueOf(Character.toUpperCase(name.charAt(0)));
    }
    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }

  @NotNull
  public static String unwrapCommentDelimiters(@NotNull String text) {
    if (text.startsWith("/**")) text = text.substring("/**".length());
    if (text.startsWith("/*")) text = text.substring("/*".length());
    if (text.startsWith("//")) text = text.substring("//".length());
    if (text.endsWith("**/")) text = text.substring(0, text.length() - "**/".length());
    if (text.endsWith("*/")) text = text.substring(0, text.length() - "*/".length());
    return text;
  }

  @NotNull
  public static String getPresentableParameterList(HaxeNamedComponent element) {
    return getPresentableParameterList(element, new HaxeGenericSpecialization());
  }

  @NotNull
  public static String getPresentableParameterList(HaxeNamedComponent element, HaxeGenericSpecialization specialization) {
    final StringBuilder result = new StringBuilder();
    final HaxeParameterListPsiMixinImpl parameterList = PsiTreeUtil.getChildOfType(element, HaxeParameterListPsiMixinImpl.class);
    if (parameterList == null) {
      return "";
    }
    final List<HaxeParameter> list = parameterList.getParametersAsList();
    for (int i = 0, size = list.size(); i < size; i++) {
      HaxeParameter parameter = list.get(i);
      result.append(parameter.getName());
      if (parameter.getTypeTag() != null) {
        result.append(":");
        result.append(buildTypeText(parameter, parameter.getTypeTag(), specialization));
      }
      if (i < size - 1) {
        result.append(", ");
      }
    }

    return result.toString();
  }

  public static String buildTypeText(HaxeNamedComponent element,
                                     @Nullable HaxeTypeListPart typeTag,
                                     HaxeGenericSpecialization specializations) {
    final List<HaxeTypeOrAnonymous> haxeTypeOrAnonymousList = typeTag != null ? typeTag.getTypeOrAnonymousList() : null;
    final HaxeTypeOrAnonymous typeOrAnonymous = ContainerUtil.getFirstItem(haxeTypeOrAnonymousList);
    if (typeOrAnonymous == null) {
      return "";
    }
    final HaxeAnonymousType anonymousType = typeOrAnonymous.getAnonymousType();
    if (anonymousType != null) {
      return anonymousType.getText();
    }

    final HaxeType haxeType = typeOrAnonymous.getType();
    return buildTypeText(element, haxeType, specializations);
  }

  public static String buildTypeText(HaxeNamedComponent element, HaxeTypeTag typeTag) {
    return buildTypeText(element, typeTag, new HaxeGenericSpecialization());
  }

  public static String buildTypeText(HaxeNamedComponent element, HaxeTypeTag typeTag, HaxeGenericSpecialization specialization) {
    if (typeTag != null)
    {
      final List<HaxeFunctionType> haxefunctionTypeList = typeTag.getFunctionTypeList();
      final HaxeFunctionType haxeFunctionType = haxefunctionTypeList.isEmpty() ? null :
                                                haxefunctionTypeList.get(0).getFunctionType();
      if (haxeFunctionType != null) {
        return buildTypeText(element, haxeFunctionType, specialization);
      }

      final List<HaxeTypeOrAnonymous> haxeTypeOrAnonymousList = typeTag.getTypeOrAnonymousList();
      if (!haxeTypeOrAnonymousList.isEmpty()) {
        final HaxeAnonymousType anonymousType = haxeTypeOrAnonymousList.get(0).getAnonymousType();
        if (anonymousType != null) {
          return anonymousType.getText();
        }
        final HaxeType haxeType = haxeTypeOrAnonymousList.get(0).getType();
        if (haxeType != null) {
          return buildTypeText(element, haxeType, specialization);
        }
      }
    }
    return "";
  }

  private static String buildTypeText(HaxeNamedComponent element,
                                      @Nullable HaxeFunctionType functionType,
                                      HaxeGenericSpecialization specialization) {
    if (functionType == null) {
      return "";
    }

    final List<HaxeTypeOrAnonymous> typeOrAnonymousList = functionType.getTypeOrAnonymousList();

    if (typeOrAnonymousList.size() == 2) {
      return buildTypeText(element, typeOrAnonymousList.get(0).getType(), specialization) +
             "->" +
             buildTypeText(element, typeOrAnonymousList.get(1).getType(), specialization);
    }
    return buildTypeText(element, functionType.getFunctionType(), specialization) +
           "->" +
           buildTypeText(element, typeOrAnonymousList.get(0).getType(), specialization);
  }

  public static String buildTypeText(HaxeNamedComponent element, @Nullable HaxeType type) {
    return buildTypeText(element, type, new HaxeGenericSpecialization());
  }

  public static String buildTypeText(HaxeNamedComponent element, @Nullable HaxeType type, HaxeGenericSpecialization specializations) {
    if (type == null) {
      return "";
    }
    final StringBuilder result = new StringBuilder();
    final String typeText = type.getReferenceExpression().getText();
    if (specializations.containsKey(element, typeText)) {
      final HaxeClass haxeClass = specializations.get(element, typeText).getHaxeClass();
      result.append(haxeClass == null ? typeText : haxeClass.getName());
    }
    else {
      result.append(typeText);
    }
    final HaxeTypeParam typeParam = type.getTypeParam();
    if (typeParam != null) {
      result.append("<");
      for (HaxeTypeListPart typeListPart : typeParam.getTypeList().getTypeListPartList()) {
        result.append(buildTypeText(element, typeListPart, specializations));
      }
      result.append(">");
    }
    return result.toString();
  }

  public static String asNullable(String type) {
    return "Null<"+type+">";
  }

  public static String unknownType() {
    return asNullable("Unknown");
  }
}
