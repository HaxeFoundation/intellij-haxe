/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.ide.generation;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeDeclarationAttribute;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeTag;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;

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

    final PsiClass containingClass = element instanceof PsiMember ? ((PsiMember)element).getContainingClass() : null;
    final boolean isInterfaceElement = containingClass != null && containingClass.isInterface();

    if (!isInterfaceElement && override && !element.isOverride()) {
      result.append("override ");
    }
    final HaxeDeclarationAttribute[] declarationAttributeList = PsiTreeUtil.getChildrenOfType(element, HaxeDeclarationAttribute.class);
    if (declarationAttributeList != null) {
      result.append(StringUtil.join(declarationAttributeList, new Function<HaxeDeclarationAttribute, String>() {
        @Override
        public String fun(HaxeDeclarationAttribute attribute) {
          return attribute.getText();
        }
      }, " "));
      result.append(" ");
    }
    if (isInterfaceElement && !result.toString().contains("public")) {
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
    if ((typeTag != null) && (typeTag.getTypeOrAnonymous() != null)) {
      result.append(":");
      result.append(HaxePresentableUtil.buildTypeText(element, typeTag.getTypeOrAnonymous().getType(), specializations));
    }
    result.append(componentType == HaxeComponentType.FIELD ? ";" : "{\n}");
    return result.toString();
  }
}
