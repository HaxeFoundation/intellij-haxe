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
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.psi.util.PsiTreeUtil;
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
    return getPresentableParameterList(element, new HaxeGenericSpecialization(), true);
  }

  @NotNull
  public static String getPresentableParameterList(HaxeNamedComponent element, HaxeGenericSpecialization specialization, boolean addTypes) {
    final StringBuilder result = new StringBuilder();
    final HaxeParameterListPsiMixinImpl parameterList = PsiTreeUtil.getChildOfType(element, HaxeParameterListPsiMixinImpl.class);
    if (parameterList == null) {
      return "";
    }
    final List<HaxeParameter> list = parameterList.getParametersAsList();
    for (int i = 0, size = list.size(); i < size; i++) {
      HaxeParameter parameter = list.get(i);
      result.append(parameter.getName());
      if (addTypes && parameter.getTypeTag() != null) {
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
    final HaxeTypeOrAnonymous typeOrAnonymous = typeTag != null ? typeTag.getTypeOrAnonymous() : null;
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
    if (typeTag != null) {
      final HaxeFunctionType haxeFunctionType = typeTag.getFunctionType();
      if (haxeFunctionType != null) {
        return buildTypeText(element, haxeFunctionType, specialization);
      }

      final HaxeTypeOrAnonymous haxeTypeOrAnonymous = typeTag.getTypeOrAnonymous();
      if (haxeTypeOrAnonymous != null) {
        final HaxeAnonymousType anonymousType = haxeTypeOrAnonymous.getAnonymousType();
        if (anonymousType != null) {
          return anonymousType.getText();
        }
        final HaxeType haxeType = haxeTypeOrAnonymous.getType();
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


    final List<HaxeFunctionArgument> arguments = functionType.getFunctionArgumentList();
    StringBuilder builder = new StringBuilder();
    builder.append("(");

    if (arguments.isEmpty()) {
      builder.append(SpecificTypeReference.VOID);
    } else {
      for (int i = 0; i < arguments.size(); i++) {
        if (i > 0) {
          builder.append(", ");
        }
        HaxeFunctionArgument argument = arguments.get(i);
        builder.append(buildTypeText(element, argument, specialization));
      }
    }
    builder
      .append(")")
      .append(" -> ")
      .append(buildTypeText(element, functionType.getFunctionReturnType(), specialization));

    return builder.toString();
  }

  private static String buildTypeText(HaxeNamedComponent element, HaxeFunctionReturnType type, HaxeGenericSpecialization specialization) {
    if (type == null) return SpecificTypeReference.UNKNOWN;

    if (type.getTypeOrAnonymous() != null) {
      return buildTypeText(element, type.getTypeOrAnonymous().getType(), specialization);
    } else if (type.getFunctionType() != null) {
      return buildTypeText(element, type.getFunctionType(), specialization);
    }

    return SpecificTypeReference.UNKNOWN;
  }

  private static String buildTypeText(HaxeNamedComponent element, HaxeFunctionArgument argument, HaxeGenericSpecialization specialization) {
    if (argument == null) return SpecificTypeReference.UNKNOWN;

    StringBuilder builder = new StringBuilder();
    if (argument.getOptionalMark() != null) {
      builder.append("?");
    }
    if (argument.getComponentName() != null) {
      builder
        .append(argument.getComponentName().getIdentifier().getText())
        .append(":");
    }

    HaxeTypeOrAnonymous typeOrAnonymous = argument.getTypeOrAnonymous();

    if (typeOrAnonymous != null) {
      builder.append(buildTypeText(element, typeOrAnonymous, specialization));
    } else if (argument.getFunctionType() != null) {
      builder.append(buildTypeText(element, argument.getFunctionType(), specialization));
    } else {
      builder.append(SpecificTypeReference.UNKNOWN);
    }
    return builder.toString();
  }

  private static String buildTypeText(HaxeNamedComponent element,
                                      HaxeTypeOrAnonymous type,
                                      HaxeGenericSpecialization specialization) {
    if (type.getType() != null) {
      return buildTypeText(element, type.getType(), specialization);
    } else if (type.getAnonymousType() != null) {
      return buildTypeText(element, type.getAnonymousType(), specialization);
    }

    return SpecificTypeReference.UNKNOWN;
  }

  private static String buildTypeText(HaxeNamedComponent element, HaxeAnonymousType type, HaxeGenericSpecialization specialization) {
    //UNIMPLEMENTED
    return "<{anonymous type}>";
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
    } else {
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
    return "Null<" + type + ">";
  }

  public static String unknownType() {
    return asNullable("Unknown");
  }

  public static String buildTypeText(@NotNull HaxeFunctionType functionType, HaxeGenericSpecialization specialization) {

    final List<HaxeFunctionArgument> arguments = functionType.getFunctionArgumentList();
    StringBuilder builder = new StringBuilder();

    if (arguments.isEmpty()) {
      builder.append(SpecificTypeReference.VOID);
    } else {
      for (int i = 0; i < arguments.size(); i++) {
        if (i > 0) {
          builder.append("->");
        }
        HaxeFunctionArgument argument = arguments.get(i);
        builder.append(buildTypeText(null, argument, specialization));
      }
    }
    builder.append("->")
      .append(buildTypeText(null, functionType.getFunctionReturnType(), specialization));

    return builder.toString();
  }
}
