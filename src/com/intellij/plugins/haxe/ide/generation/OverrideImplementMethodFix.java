package com.intellij.plugins.haxe.ide.generation;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.List;

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
    final StringBuilder result = new StringBuilder();
    if (override && !element.isOverride()) {
      result.append("override ");
    }
    final HaxeDeclarationAttributeList declarationAttributeList = PsiTreeUtil.getChildOfType(element, HaxeDeclarationAttributeList.class);
    if (declarationAttributeList != null) {
      result.append(declarationAttributeList.getText());
      result.append(" ");
    }
    if(!result.toString().contains("public")){
      result.insert(0, "public ");
    }
    result.append("function ");
    result.append(element.getName());
    result.append(" (");
    result.append(HaxePresentableUtil.getPresentableParameterList(element, specializations));
    result.append(")");
    final HaxeTypeTag typeTag = PsiTreeUtil.getChildOfType(element, HaxeTypeTag.class);
    if(typeTag != null){
      result.append(":");
      result.append(HaxePresentableUtil.buildTypeText(element, typeTag.getType(), specializations));
    }
    result.append("{\n}\n");
    return result.toString();
  }
}
