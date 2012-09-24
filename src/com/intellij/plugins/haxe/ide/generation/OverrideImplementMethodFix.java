package com.intellij.plugins.haxe.ide.generation;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeDeclarationAttributeList;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeTag;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author: Fedor.Korotkov
 */
public class OverrideImplementMethodFix extends BaseCreateMethodsFix<HaxeNamedComponent> {
  final boolean override;

  public OverrideImplementMethodFix(final HaxeClass haxeClass, boolean override) {
    super(haxeClass);
    this.override = override;
  }

  @Override
  protected String buildFunctionsText(HaxeNamedComponent element) {
    final HaxeComponentType componentType = HaxeComponentType.typeOf(element);
    final StringBuilder result = new StringBuilder();
    if (override && !element.isOverride()) {
      result.append("override ");
    }
    final HaxeDeclarationAttributeList declarationAttributeList = PsiTreeUtil.getChildOfType(element, HaxeDeclarationAttributeList.class);
    if (declarationAttributeList != null) {
      result.append(declarationAttributeList.getText());
      result.append(" ");
    }
    if (!result.toString().contains("public")) {
      result.insert(0, "public ");
    }
    if (componentType == HaxeComponentType.FIELD) {
      result.append("var ");
      result.append(element.getName());
    }
    else {
      result.append("function ");
      result.append(element.getName());
      result.append(" (");
      result.append(HaxePresentableUtil.getPresentableParameterList(element, specializations));
      result.append(")");
    }
    final HaxeTypeTag typeTag = PsiTreeUtil.getChildOfType(element, HaxeTypeTag.class);
    if (typeTag != null) {
      result.append(":");
      result.append(HaxePresentableUtil.buildTypeText(element, typeTag.getTypeOrAnonymous().getType(), specializations));
    }
    result.append(componentType == HaxeComponentType.FIELD ? ";" : "{\n}");
    return result.toString();
  }
}
