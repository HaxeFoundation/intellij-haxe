package com.intellij.plugins.haxe.ide.info;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.HaxeClassResolveResult;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeTag;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeFunctionDescription {
  private final String name;
  private final String returnType;
  private final HaxeParameterDescription[] parameters;

  public HaxeFunctionDescription(String name, String type, HaxeParameterDescription[] parameters) {
    this.name = name;
    returnType = type;
    this.parameters = parameters;
  }

  public String getName() {
    return name;
  }

  public String getReturnType() {
    return returnType;
  }

  public HaxeParameterDescription[] getParameters() {
    return parameters;
  }

  public String getParametersListPresentableText() {
    final StringBuilder result = new StringBuilder();
    for (HaxeParameterDescription parameterDescription : parameters) {
      if (result.length() > 0) {
        result.append(", ");
      }
      result.append(parameterDescription.toString());
    }
    return result.toString();
  }

  public TextRange getParameterRange(int index) {
    if (index == -1) {
      return new TextRange(0, 0);
    }
    int startOffset = 0;
    for (int i = 0, length = parameters.length; i < length; i++) {
      if (i == index) {
        int shift = i == 0 ? 0 : ", ".length();
        return new TextRange(startOffset + shift, startOffset + shift + parameters[i].toString().length());
      }
      if (i > 0) {
        startOffset += ", ".length();
      }
      startOffset += parameters[i].toString().length();
    }
    return new TextRange(0, 0);
  }

  public static HaxeFunctionDescription createDescription(HaxeNamedComponent namedComponent, HaxeClassResolveResult resolveResult) {
    String typeText = "";
    final HaxeTypeTag typeTag = PsiTreeUtil.getChildOfType(namedComponent, HaxeTypeTag.class);
    if (typeTag != null) {
      typeText = HaxePresentableUtil.buildTypeText(namedComponent, typeTag, resolveResult.getSpecialization());
    }
    return new HaxeFunctionDescription(namedComponent.getName(),
                                       typeText,
                                       HaxeParameterDescription.getParameters(namedComponent, resolveResult.getSpecialization()));
  }
}
