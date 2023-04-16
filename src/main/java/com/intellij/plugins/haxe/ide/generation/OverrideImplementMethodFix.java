/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
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
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
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

    boolean addOverride = !isInterfaceElement && override && !element.isOverride();
    if (addOverride) {
      result.append("override ");
    }
    final HaxePsiModifier[] declarationAttributeList = PsiTreeUtil.getChildrenOfType(element, HaxePsiModifier.class);
    if (declarationAttributeList != null) {
      result.append(StringUtil.join(declarationAttributeList, new Function<HaxePsiModifier, String>() {
        @Override
        public String fun(HaxePsiModifier attribute) {
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
    } else {
      result.append("function ");
      appendMethodNameAndParameters(result, element, true);
    }
    final HaxeTypeTag typeTag = PsiTreeUtil.getChildOfType(element, HaxeTypeTag.class);
    String type = null;
    if (typeTag != null && typeTag.getTypeOrAnonymous() != null) {
      result.append(":");
      type = HaxePresentableUtil.buildTypeText(element, typeTag.getTypeOrAnonymous().getType(), specializations);
      result.append(type);
    }
    if(componentType == HaxeComponentType.FIELD) {
      result.append(";");
    } else {
      result.append("{\n");
      if(addOverride || element.isOverride()) {
        if(type != null && !type.equals("Void")) {
          result.append("return ");
        }
        result.append("super.");
        appendMethodNameAndParameters(result, element, false);
        result.append(";\n");
      }
      result.append("}");
    }
    return result.toString();
  }

  private void appendMethodNameAndParameters(StringBuilder buf, HaxeNamedComponent element, boolean addParametersTypes) {
    buf.append(element.getName());
    buf.append(" (");
    buf.append(HaxePresentableUtil.getPresentableParameterList(element, specializations, addParametersTypes));
    buf.append(")");
  }
}
