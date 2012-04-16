package com.intellij.plugins.haxe.ide.info;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeParameterDescription {
  private final String name;
  private final String typeText;
  private final String varInitValue;

  public HaxeParameterDescription(String name, String text, String varInitText) {
    this.name = name;
    typeText = text;
    varInitValue = varInitText;
  }


  public static HaxeParameterDescription[] getParameters(HaxeNamedComponent element, HaxeGenericSpecialization specialization) {
    final HaxeParameterList parameterList = PsiTreeUtil.getChildOfType(element, HaxeParameterList.class);
    if (parameterList == null) {
      return new HaxeParameterDescription[0];
    }
    final List<HaxeParameter> list = parameterList.getParameterList();
    final HaxeParameterDescription[] result = new HaxeParameterDescription[list.size()];
    for (int i = 0, size = list.size(); i < size; i++) {
      HaxeParameter parameter = list.get(i);
      String typeText = "";
      if (parameter.getTypeTag() != null) {
        typeText = HaxePresentableUtil.buildTypeText(element, parameter.getTypeTag(), specialization);
      }
      String varInitText = "";
      HaxeVarInit varInit = parameter.getVarInit();
      if (varInit != null) {
        final HaxeExpression varInitExpression = varInit.getExpression();
        varInitText = varInitExpression == null ? "" : varInitExpression.getText();
      }
      result[i] = new HaxeParameterDescription(parameter.getName(), typeText, varInitText);
    }
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append(name);
    if (!typeText.isEmpty()) {
      result.append(":").append(typeText);
    }
    if (!varInitValue.isEmpty()) {
      result.append(" = ").append(varInitValue);
    }
    return result.toString();
  }
}
