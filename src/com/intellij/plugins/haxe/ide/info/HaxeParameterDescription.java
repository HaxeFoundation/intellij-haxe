package com.intellij.plugins.haxe.ide.info;

import com.intellij.plugins.haxe.lang.psi.HaxeGenericSpecialization;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeParameter;
import com.intellij.plugins.haxe.lang.psi.HaxeParameterList;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeParameterDescription {
  private final String name;
  private final String typeText;

  public HaxeParameterDescription(String name, String text) {
    this.name = name;
    typeText = text;
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
      result[i] = new HaxeParameterDescription(parameter.getName(), typeText);
    }
    return result;
  }

  @Override
  public String toString() {
    if (typeText.isEmpty()) {
      return name;
    }
    return name + ":" + typeText;
  }
}
