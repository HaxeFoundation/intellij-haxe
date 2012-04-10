package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.lang.psi.*;
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
    return "set" + startsWithUpperCase(name);
  }

  public static String getterName(@NotNull @Nls String name) {
    return "get" + startsWithUpperCase(name);
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
    final HaxeParameterList parameterList = PsiTreeUtil.getChildOfType(element, HaxeParameterList.class);
    if (parameterList == null) {
      return "";
    }
    final List<HaxeParameter> list = parameterList.getParameterList();
    for (int i = 0, size = list.size(); i < size; i++) {
      HaxeParameter parameter = list.get(i);
      result.append(parameter.getName());
      if (parameter.getTypeTag() != null) {
        result.append(":");
        result.append(buildTypeText(element, parameter.getTypeTag(), specialization));
      }
      if (i < size - 1) {
        result.append(", ");
      }
    }

    return result.toString();
  }

  public static String buildTypeText(HaxeNamedComponent element, HaxeTypeListPart typeTag, HaxeGenericSpecialization specializations) {
    final HaxeAnonymousType anonymousType = typeTag.getAnonymousType();
    if (anonymousType != null) {
      return anonymousType.getText();
    }

    final HaxeType haxeType = typeTag.getType();
    return buildTypeText(element, haxeType, specializations);
  }

  public static String buildTypeText(HaxeNamedComponent element, HaxeTypeTag typeTag) {
    return buildTypeText(element, typeTag, new HaxeGenericSpecialization());
  }

  public static String buildTypeText(HaxeNamedComponent element, HaxeTypeTag typeTag, HaxeGenericSpecialization specialization) {
    final HaxeAnonymousType anonymousType = typeTag.getAnonymousType();
    if (anonymousType != null) {
      return anonymousType.getText();
    }
    final HaxeType haxeType = typeTag.getType();
    if (haxeType != null) {
      return buildTypeText(element, haxeType, specialization);
    }
    final HaxeFunctionType haxeFunctionType = typeTag.getFunctionType();
    return buildTypeText(element, haxeFunctionType, specialization);
  }

  private static String buildTypeText(HaxeNamedComponent element,
                                      @Nullable HaxeFunctionType functionType,
                                      HaxeGenericSpecialization specialization) {
    if (functionType == null) {
      return "";
    }

    final HaxeType[] types = PsiTreeUtil.getChildrenOfType(functionType, HaxeType.class);
    assert types != null;
    if (types.length == 2) {
      return buildTypeText(element, types[0], specialization) +
             "->" +
             buildTypeText(element, types[1], specialization);
    }
    return buildTypeText(element, functionType.getFunctionType(), specialization) +
           "->" +
           buildTypeText(element, functionType.getType(), specialization);
  }

  public static String buildTypeText(HaxeNamedComponent element, @Nullable HaxeType type) {
    return buildTypeText(element, type, new HaxeGenericSpecialization());
  }

  public static String buildTypeText(HaxeNamedComponent element, @Nullable HaxeType type, HaxeGenericSpecialization specializations) {
    if (type == null) {
      return "";
    }
    final StringBuilder result = new StringBuilder();
    final String typeText = type.getExpression().getText();
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
}
